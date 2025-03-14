package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {
    //@Query("DELETE FROM Site s WHERE s.id = :site.id")
    //void deleteBySite(@Param("site") Site site);

    Site findByUrl(String url);
}
