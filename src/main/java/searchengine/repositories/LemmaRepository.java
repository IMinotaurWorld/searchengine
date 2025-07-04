package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    Lemma findByLemmaAndSite(String lemma, Site site);
    List<Lemma> findByLemma(String lemma);

    long countBySite(Site site);

    @Modifying
    @Query("DELETE FROM Lemma l WHERE l.site = :site")
    void deleteBySite(Site site);
}
