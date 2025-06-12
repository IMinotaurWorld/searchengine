package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "search_index",
        indexes = @javax.persistence.Index(name = "idx_index_page_lemma", columnList = "page_id,lemma_id", unique = true))
@Getter @Setter
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false, foreignKey = @ForeignKey(name = "fk_index_page"))
    private Page page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false, foreignKey = @ForeignKey(name = "fk_index_lemma"))
    private Lemma lemma;

    @Column(name = "rank_", nullable = false)
    private Float rank;
}
