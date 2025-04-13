package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.IndexingResponse;
import searchengine.services.IndexingService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class IndexingController {
    private final IndexingService indexingService;

    @GetMapping("/startIndexing")
    public IndexingResponse startIndexing(){
        return indexingService.startIndexing();
    }

    @GetMapping("/stopIndexing")
    public IndexingResponse stopIndexing(){
        return indexingService.stopIndexing();
    }
}
