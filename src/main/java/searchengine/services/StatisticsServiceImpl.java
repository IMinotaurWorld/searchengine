package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Status;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private  final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private  final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        List<DetailedStatisticsItem> detailed = new ArrayList<>();

        List<searchengine.model.Site> sites = siteRepository.findAll();
        total.setSites(sites.size());
        total.setIndexing(sites.stream().anyMatch(s -> s.getStatus() == Status.INDEXING));

        for (searchengine.model.Site site : sites) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            item.setStatus(site.getStatus().toString());
            item.setError(site.getLastError());
            item.setStatusTime(site.getStatusTime().toEpochSecond(ZoneOffset.UTC) * 1000);

            long pageCount = pageRepository.countBySite(site);
            long lemmaCount = lemmaRepository.countBySite(site);

            item.setPages((int) pageCount);
            item.setLemmas((int) lemmaCount);

            total.setPages(total.getPages() + (int) pageCount);
            total.setLemmas(total.getLemmas() + (int) lemmaCount);

            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
