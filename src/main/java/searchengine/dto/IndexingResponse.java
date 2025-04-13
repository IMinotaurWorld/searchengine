package searchengine.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IndexingResponse {
    private boolean result;
    private String error;

    public IndexingResponse(boolean result){
        this.result = result;
    }

    public IndexingResponse(boolean result, String error){
        this.result = result;
        this.error = error;
    }
}
