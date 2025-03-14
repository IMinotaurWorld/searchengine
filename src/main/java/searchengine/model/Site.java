package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "site")
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "status_time", nullable = false)
    private LocalDateTime status_time;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String last_error;

    @Column(name = "url", unique = true, nullable = false)
    private String url;

    @Column(name = "name", nullable = false)
    private String name;
}
