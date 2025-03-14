package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Set;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    @Query("DELETE FROM Page p WHERE p.site = :site")
    void deleteBySite(@Param("site") Site site);

    Page findByPathAndSite(String url, Site site);
    Set<Page> findByLemma(Lemma lemma);
}
