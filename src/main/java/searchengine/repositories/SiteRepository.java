package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Site;
import searchengine.model.Status;
import java.util.List;

public interface SiteRepository extends JpaRepository<Site, Long> {
    Site findByUrl(String url);
    List<Site> findByStatus(Status status);
}