package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Set;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    Page findBySiteAndPath(Site site, String path);


    @Query("SELECT p FROM Page p JOIN Index i ON p = i.page WHERE i.lemma = :lemma")
    Set<Page> findByLemma(@Param("lemma") Lemma lemma);

    long countBySite(Site site);

    @Modifying
    @Query("DELETE FROM Page p WHERE p.site = :site")
    void deleteBySite(Site site);
}
