package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
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

    @PostMapping("/indexPage")
    public IndexingResponse indexPage(@RequestParam String url) {
        return indexingService.indexPage(url);
    }
}
