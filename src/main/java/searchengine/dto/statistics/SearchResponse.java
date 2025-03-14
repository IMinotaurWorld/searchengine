package searchengine.dto.statistics;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SearchResponse {
    private boolean result;
    private List<SearchResult> searchResultList;
    private int count;
}
