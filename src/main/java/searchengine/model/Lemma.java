package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lemma",
        indexes = @javax.persistence.Index(name = "idx_lemma_site_lemma", columnList = "site_id,lemma", unique = true))
@Getter @Setter
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false, foreignKey = @ForeignKey(name = "fk_lemma_site"))
    private Site site;

    @Column(nullable = false, length = 255)
    private String lemma;

    @Column(nullable = false)
    private Integer frequency;

    @OneToMany(mappedBy = "lemma", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Index> indices = new ArrayList<>();
}

