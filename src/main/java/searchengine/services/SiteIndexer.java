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
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class SiteIndexer {
    private final AtomicBoolean isIndexing = new AtomicBoolean(false);
    private final AtomicBoolean isStopped = new AtomicBoolean(false);
    private ForkJoinPool forkJoinPool;

    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    public synchronized void startIndexing() {
        if (isIndexing.get()) {
            return;
        }

        isIndexing.set(true);
        isStopped.set(false);
        new Thread(this::indexAllSites).start();
    }

    public synchronized void stopIndexing() {
        if (!isIndexing.get()) {
            return;
        }

        isStopped.set(true);
        isIndexing.set(false);

        if (forkJoinPool != null) {
            forkJoinPool.shutdownNow();
        }

        siteRepository.findByStatus(Status.INDEXING).forEach(site -> {
            site.setStatus(Status.FAILED);
            site.setLastError("Индексация остановлена пользователем");
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        });
    }

    public boolean isIndexing() {
        return isIndexing.get();
    }

    private void indexAllSites() {
        try {
            for (searchengine.config.Site configSite : sitesList.getSites()) {
                if (isStopped.get()) {
                    break;
                }
                indexSite(configSite);
            }
        } finally {
            isIndexing.set(false);
        }
    }

    @Transactional
    public void indexSite(searchengine.config.Site configSite) {
        Site site = siteRepository.findByUrl(configSite.getUrl());
        if (site == null) {
            site = new Site();
            site.setUrl(configSite.getUrl());
            site.setName(configSite.getName());
        }

        site.setStatus(Status.INDEXING);
        site.setLastError(null);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);

        try {
            forkJoinPool = new ForkJoinPool();
            forkJoinPool.invoke(new PageIndexer(site, site.getUrl(), new HashSet<>(), 0));

            if (isStopped.get()) {
                site.setStatus(Status.FAILED);
                site.setLastError("Индексация остановлена");
            } else {
                site.setStatus(Status.INDEXED);
            }
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        } catch (Exception e) {
            if (!isStopped.get()) {
                site.setStatus(Status.FAILED);
                site.setLastError("Ошибка индексации: " + e.getMessage());
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);
            }
        } finally {
            if (forkJoinPool != null) {
                forkJoinPool.shutdown();
            }
        }
    }

    private class PageIndexer extends RecursiveAction {
        private final Site site;
        private final String url;
        private final Set<String> visitedUrls;
        private final int depth;
        private static final int MAX_DEPTH = 5;
        private static final int MAX_PAGES_PER_SITE = 1000;

        public PageIndexer(Site site, String url, Set<String> visitedUrls, int depth) {
            this.site = site;
            this.url = url;
            this.visitedUrls = visitedUrls;
            this.depth = depth;
        }

        @Override
        protected void compute() {
            if (isStopped.get() || depth > MAX_DEPTH || visitedUrls.size() >= MAX_PAGES_PER_SITE || visitedUrls.contains(url)) {
                return;
            }

            visitedUrls.add(url);

            try {
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                        .referrer("https://www.google.com")
                        .timeout(10_000)
                        .ignoreHttpErrors(true)
                        .get();

                if (doc == null || isStopped.get()) {
                    return;
                }

                Page page = new Page();
                page.setSite(site);
                page.setPath(url);
                page.setCode(doc.connection().response().statusCode());
                page.setContent(doc.html());

                synchronized (pageRepository) {
                    pageRepository.save(page);
                }

                synchronized (siteRepository) {
                    site.setStatusTime(LocalDateTime.now());
                    siteRepository.save(site);
                }

                if (depth < MAX_DEPTH && visitedUrls.size() < MAX_PAGES_PER_SITE) {
                    Elements links = doc.select("a[href]");
                    List<PageIndexer> subtasks = new ArrayList<>();

                    for (Element link : links) {
                        if (isStopped.get() || visitedUrls.size() >= MAX_PAGES_PER_SITE) {
                            break;
                        }

                        String nextUrl = link.absUrl("href");
                        if (shouldIndex(nextUrl) && !visitedUrls.contains(nextUrl)) {
                            subtasks.add(new PageIndexer(site, nextUrl, visitedUrls, depth + 1));
                        }
                    }

                    invokeAll(subtasks);
                    Thread.sleep(500);
                }

            } catch (Exception e) {
                if (!isStopped.get()) {
                    synchronized (siteRepository) {
                        site.setLastError("Ошибка при индексации страницы: " + url + " - " + e.getMessage());
                        site.setStatusTime(LocalDateTime.now());
                        siteRepository.save(site);
                    }
                }
            }
        }

        private boolean shouldIndex(String url) {
            return url != null &&
                    url.startsWith(site.getUrl()) &&
                    !url.contains("#") &&
                    !url.matches(".*\\.(pdf|docx?|xlsx?|pptx?|zip|rar|7z|tar|gz|png|jpe?g|gif|bmp|webp|mp3|mp4|avi|mov)$") &&
                    depth < MAX_DEPTH;
        }
    }
}