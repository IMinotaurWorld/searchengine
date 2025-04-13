package searchengine.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchResult {
    private String uri;
    private String title;
    private String snippet;
    private Double relevant;
}
