package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

@Component
@RequiredArgsConstructor
public class SiteIndexer {
    private volatile boolean isStopped = false;
    private volatile boolean isIndexing = false;

    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;

    public void indexAllSites(){
        isStopped = false;
        isIndexing = true;
        List<searchengine.config.Site> sites = sitesList.getSites();
        for(searchengine.config.Site site : sites){
            if(isStopped) return;
            indexSite(site);
        }
        isIndexing = false;
    }

    public void stopIndexing(){
        isStopped = true;
        isIndexing = false;
    }

    @Transactional
    public void indexSite(searchengine.config.Site configSite){
        Site existingSite = siteRepository.findByUrl(configSite.getUrl());
        if(existingSite == null){
            Site newSite = new Site();
            newSite.setUrl(configSite.getUrl());
            newSite.setName(configSite.getName());
            newSite.setStatus(Status.INDEXING);
            newSite.setStatusTime(LocalDateTime.now());
            siteRepository.save(newSite);
            ForkJoinPool.commonPool().invoke(new PageIndexer(newSite, configSite.getUrl(), new HashSet<>(), 0));
        }
    }

    private class PageIndexer extends RecursiveAction {
        private final Site site;
        private final String url;
        private final Set<String> visitedUrls;
        private final int depth;

        public PageIndexer(Site site, String url, Set<String> visitedUrls, int depth) {
            this.site = site;
            this.url = url;
            this.visitedUrls = visitedUrls;
            this.depth = depth;
        }

        @Override
        protected void compute() {
            if(isStopped || visitedUrls.contains(url)){
                return;
            }
            visitedUrls.add(url);
            try{
                Document doc = Jsoup.connect(url)
                        .userAgent("").referrer("http://www.google.com").get();
                Page page = new Page();
                page.setSite(site);
                page.setPath(url);
                page.setCode(200);
                page.setContent(doc.html());
                pageRepository.save(page);
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);
                Elements elements = doc.select("a[href]");
                for(Element element : elements){
                    if(isStopped) return;
                    String nextUrl = element.absUrl("href");
                    if(nextUrl.startsWith(site.getUrl())){
                        ForkJoinPool.commonPool().invoke(new PageIndexer(site, nextUrl, visitedUrls, depth + 1));
                    }
                }
                Thread.sleep(1000);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
