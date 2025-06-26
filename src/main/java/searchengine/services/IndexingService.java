// IndexingService.java
package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SitesList;
import searchengine.dto.IndexingResponse;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IndexingService {
    private final SiteIndexer siteIndexer;
    private final SitesList sitesList;
    private final Lemmatizer lemmatizer;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;

    @Transactional
    public IndexingResponse startIndexing() {
        if (siteIndexer.isIndexing()) {
            return new IndexingResponse(false, "Индексация уже запущена!");
        }

        sitesList.getSites().forEach(configSite -> {
            Site site = siteRepository.findByUrl(configSite.getUrl());
            if (site == null) {
                site = new Site();
                site.setUrl(configSite.getUrl());
                site.setName(configSite.getName());
                siteRepository.save(site);
            }

            indexRepository.deleteBySite(site.getId());
            lemmaRepository.deleteBySite(site);
            pageRepository.deleteBySite(site);

            site.setStatus(Status.INDEXING);
            site.setLastError(null);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        });

        siteIndexer.startIndexing();
        return new IndexingResponse(true);
    }

    public IndexingResponse stopIndexing() {
        if (!siteIndexer.isIndexing()) {
            return new IndexingResponse(false, "Индексация не запущена!");
        }
        siteIndexer.stopIndexing();
        return new IndexingResponse(true);
    }

    @Transactional
    public IndexingResponse indexPage(String url) {
        if (siteIndexer.isIndexing()) {
            return new IndexingResponse(false, "Дождитесь завершения текущей индексации");
        }

        Optional<searchengine.config.Site> siteOptional = sitesList.getSites().stream()
                .filter(site -> url.startsWith(site.getUrl()))
                .findFirst();

        if (siteOptional.isEmpty()) {
            return new IndexingResponse(false, "Данная страница находится за пределами сайтов, указанных в конфигурации!");
        }

        searchengine.config.Site configSite = siteOptional.get();
        searchengine.model.Site site = siteRepository.findByUrl(configSite.getUrl());

        if (site == null) {
            site = new searchengine.model.Site();
            site.setUrl(configSite.getUrl());
            site.setName(configSite.getName());
            site.setStatus(Status.INDEXED);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        }

        Page existingPage = pageRepository.findBySiteAndPath(site, url);
        if (existingPage != null) {
            indexRepository.deleteBySite(site.getId());
            lemmaRepository.deleteBySite(site);
            pageRepository.deleteBySite(site);

        }

        String html;
        try {
            html = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                    .referrer("https://www.google.com")
                    .timeout(10_000)
                    .get()
                    .html();
        } catch (IOException e) {
            return new IndexingResponse(false, "Ошибка при загрузке страницы: " + e.getMessage());
        }

        Page newPage = new Page();
        newPage.setPath(url);
        newPage.setCode(200);
        newPage.setContent(html);
        newPage.setSite(site);
        pageRepository.save(newPage);

        String cleanHtml = lemmatizer.cleanHtml(html);
        Map<String, Integer> lemmaCounts = lemmatizer.lemmatizeText(cleanHtml);

        for (Map.Entry<String, Integer> entry : lemmaCounts.entrySet()) {
            String lemmaText = entry.getKey();
            int frequency = entry.getValue();

            Lemma lemma = lemmaRepository.findByLemmaAndSite(lemmaText, site);
            if (lemma == null) {
                lemma = new Lemma();
                lemma.setLemma(lemmaText);
                lemma.setSite(site);
                lemma.setFrequency(1);
            } else {
                lemma.setFrequency(lemma.getFrequency() + 1);
            }
            lemmaRepository.save(lemma);

            Index index = new Index();
            index.setPage(newPage);
            index.setLemma(lemma);
            index.setRank((float) frequency);
            indexRepository.save(index);
        }

        return new IndexingResponse(true);
    }
}