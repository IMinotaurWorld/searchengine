package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.persistence.Index;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "page", indexes = @Index(name = "idx_page_path", columnList = "path", unique = true))
@Getter @Setter
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false, foreignKey = @ForeignKey(name = "fk_page_site"))
    private Site site;

    @Column(nullable = false, length = 511)
    private String path;

    @Column(nullable = false)
    private Integer code;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<searchengine.model.Index> indices = new ArrayList<>();
}
