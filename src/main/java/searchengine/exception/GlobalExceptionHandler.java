package searchengine.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import searchengine.dto.IndexingResponse;

import java.util.Optional;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IndexingException.class)
    public ResponseEntity<IndexingResponse> handleResourceNotFound(IndexingException e){
        IndexingResponse response = new IndexingResponse(false, e.message);
        return ResponseEntity.of(Optional.of(response));
    }

    //@ExceptionHandler(LemmatizerException.class)
    //public ResponseEntity<RuntimeException> handleResourceNotFound(LemmatizerException e){
    //    RuntimeException response = new RuntimeException(e.message);
    //    return ResponseEntity.of(Optional.of(response));
    //}
}
