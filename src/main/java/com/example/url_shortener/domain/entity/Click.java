package com.example.url_shortener.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "clicks")
@Getter
@Setter
@NoArgsConstructor
public class Click {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_id", nullable = false)
    private Url url;

    @Column(name = "ip_address", columnDefinition = "INET")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(columnDefinition = "TEXT")
    private String referer;

    @Column(length = 2)
    private String country;

    @Column(name = "clicked_at", nullable = false, updatable = false, insertable = false)
    private Instant clickedAt;

    @Builder
    public Click(Url url, String ipAddress, String userAgent, String referer, String country) {
        this.url = url;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.referer = referer;
        this.country = country;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Click click = (Click) o;
        return id != null && Objects.equals(id, click.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
