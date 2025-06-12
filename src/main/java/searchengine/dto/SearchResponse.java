package searchengine.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SearchResponse {
    private boolean result;
    private List<SearchResult> data = new ArrayList<>();
    private int count;
    private String error;

}
