package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.SearchResponse;
import searchengine.dto.statistics.SearchResult;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.*;

@RequiredArgsConstructor
@Service
public class SearchService {
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final IndexRepository indexRepository;
    private final Lemmatizer lemmatizer;

    public SearchResponse search(String querry, String siteUrl){
        SearchResponse searchResponse = new SearchResponse();
        List<SearchResult> searchResultList = new ArrayList<>();

        Map<String, Integer> querryLemmas = lemmatizer.lemmatizeText(querry);
        List<Lemma> filteredLemmas = filterLemmas(querryLemmas.keySet(), siteUrl);
        Set<Page> pageSet = findPagesByLemma(filteredLemmas);
        Map<Page, Double> relevanceMap = calculateRelevance(pageSet, filteredLemmas);
        List<Page> sortedPages = relevanceMap.entrySet().stream()
                .sorted(Map.Entry.<Page, Double>comparingByValue().reversed())
                .map(Map.Entry :: getKey)
                .toList();
        for(Page page : sortedPages){
            SearchResult result = new SearchResult();
            result.setUri(page.getPath());
            result.setTitle(Jsoup.parse(page.getContent()).title());
            result.setSnippet(generateSnippet(page.getContent(), querryLemmas.keySet()));
            result.setRelevant(relevanceMap.get(page));
            searchResultList.add(result);
        }

        searchResponse.setResult(true);
        searchResponse.setCount(searchResultList.size());
        searchResponse.setSearchResultList(searchResultList);
        return searchResponse;
    }

    private List<Lemma> filterLemmas(Set<String> querryLemmas, String siteUrl){
        List<Lemma> lemmas = new ArrayList<>();
        for(String lemma : querryLemmas){
            List<Lemma> foundLemmas;
            if(siteUrl != null){
                Site site = siteRepository.findByUrl(siteUrl);
                foundLemmas = new ArrayList<>();
                foundLemmas.add(lemmaRepository.findByLemmaAndSite(lemma, site));
            }else{
                foundLemmas = lemmaRepository.findByLemma(lemma);
            }
            if(!foundLemmas.isEmpty() && foundLemmas != null){
                lemmas.addAll(foundLemmas);
            }
        }
        lemmas.removeIf(lemma -> lemma.getFrequency() > 80);
        lemmas.sort(Comparator.comparingInt(Lemma::getFrequency));
        return lemmas;
    }

    private Set<Page> findPagesByLemma(List<Lemma> lemmas){
        if(lemmas.isEmpty()) return Collections.emptySet();
        Set<Page> pages = new HashSet<>(pageRepository.findByLemma(lemmas.get(0)));
        for(int i = 1;i < lemmas.size();i++){
            pages.retainAll(pageRepository.findByLemma(lemmas.get(i)));
        }
        return pages;
    }

    private Map<Page, Double> calculateRelevance(Set<Page> pages, List<Lemma> lemmas){
        Map<Page, Double> relevanceMap = new HashMap<>();
        Double maxRelevance = 0.0;
        for(Page page : pages) {
            double relevance = 0.0;
            for(Lemma lemma : lemmas){
                Float rank = indexRepository.findRankByPageAndLemma(page, lemma);
                if(rank != null){
                    relevance += rank;
                }
            }
            relevanceMap.put(page, relevance);
            if(relevance > maxRelevance){
                maxRelevance = relevance;
            }
        }

        if(maxRelevance > 0){
            for(Map.Entry<Page, Double> entry : relevanceMap.entrySet()){
                entry.setValue(entry.getValue() / maxRelevance);
            }
        }

        return relevanceMap;
    }

    private String generateSnippet(String html, Set<String> querryLemmas){
        String text = Jsoup.parse(html).text();
        StringBuilder snippetBuilder = new StringBuilder();
        int snippetLength = 150;
        int start = 0;
        int end = 0;
        for(String lemma : querryLemmas){
            int lemmaIndex = text.indexOf(lemma);
            if(lemmaIndex != -1){
                start = Math.max(0, lemmaIndex - snippetLength / 2);
                end = Math.min(text.length(), lemmaIndex + snippetLength);
                snippetBuilder.append(text, start, end).append("... ");
            }
        }
        if(snippetBuilder.length() == 0){
            snippetBuilder.append(text, 0, Math.min(text.length(), snippetLength)).append("... ");
        }
        for(String lemma : querryLemmas){
            snippetBuilder = new StringBuilder(snippetBuilder.toString().replace(lemma, "<b>" + lemma + "</b>"));
        }
        return snippetBuilder.toString();
    }
}
