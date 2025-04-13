package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.IndexingResponse;
import searchengine.exception.IndexingException;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Status;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class IndexingService {
    private volatile boolean indexing = false;

    private final SiteIndexer siteIndexer;
    private final SitesList sitesList;
    private final Lemmatizer lemmatizer;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;

    public IndexingResponse startIndexing(){
        if(indexing) return new IndexingResponse(false, "Индексация уже запущена!");
        indexing = true;
        CompletableFuture.runAsync(() -> {
            try{
                siteIndexer.indexAllSites();
            }catch(Exception e){
                indexing = false;
                IndexingException exception = new IndexingException();
                exception.message = "Индексация прервана!";
                throw exception;
            }
        });

        return new IndexingResponse(true);
    }

    public IndexingResponse indexPage(String url){
        Optional<Site> siteOptional = sitesList
                .getSites().stream()
                .filter(site -> url.startsWith(site.getUrl()))
                .findFirst();
        if(siteOptional.isEmpty()) return new IndexingResponse(false, "Данная страница находится за пределами сайта!");
        Site configSite = siteOptional.get();
        searchengine.model.Site site = siteRepository.findByUrl(configSite.getUrl());
        if(site == null){
            site = new searchengine.model.Site();
            site.setUrl(configSite.getUrl());
            site.setName(configSite.getName());
            site.setStatus(Status.INDEXING);
            site.setStatus_time(LocalDateTime.now());
            siteRepository.save(site);
        }
        Page existingPage = pageRepository.findByPathAndSite(url, site);
        if(existingPage != null){
            indexRepository.deleteByPage(existingPage);
            pageRepository.delete(existingPage);
        }
        String html;
        try{
            html = Jsoup.connect(url).get().html();
        }catch(IOException e){
            IndexingException exception = new IndexingException();
            exception.message = "Ошибка при загрузке страницы!";
            throw exception;
        }
        Page newPage = new Page();
        newPage.setPath(url);
        newPage.setCode(200);
        newPage.setContent(html);
        newPage.setSite(site);
        pageRepository.save(newPage);
        String cleanHtml = lemmatizer.cleanHtml(html);
        Map<String, Integer> lemmaCounts = lemmatizer.lemmatizeText(cleanHtml);
        for(Map.Entry<String, Integer> entry : lemmaCounts.entrySet()){
            String text = entry.getKey();
            int frequency = entry.getValue();
            Lemma lemma = lemmaRepository.findByLemmaAndSite(text, site);
            if(lemma == null){
                lemma = new Lemma();
                lemma.setLemma(text);
                lemma.setSite(site);
                lemma.setFrequency(1);
            }else{
                lemma.setFrequency(lemma.getFrequency() + 1);
            }
            lemmaRepository.save(lemma);
            Index index = new Index();
            index.setPage(newPage);
            index.setLemma(lemma);
            index.setRank(lemma.getFrequency());
            indexRepository.save(index);
        }
        return new IndexingResponse(true);
    }

    public IndexingResponse stopIndexing(){
        if(!indexing) return new IndexingResponse(false, "Индексация не запущена!");
        indexing = false;
        siteIndexer.stopIndexing();
        return new IndexingResponse(true);
    }
}
