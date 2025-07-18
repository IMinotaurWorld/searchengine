package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {
    void deleteByPage(Page page);
    @Query("SELECT i.rank FROM Index i WHERE i.page = :page AND i.lemma = :lemma")
    Float findRankByPageAndLemma(@Param("page") Page page, @Param("lemma") Lemma lemma);

    @Modifying
    @Query(value = "DELETE FROM search_index WHERE lemma_id IN (SELECT id FROM lemma WHERE site_id = :siteId)", nativeQuery = true)
    void deleteBySite(@Param("siteId") Long siteId);
}
