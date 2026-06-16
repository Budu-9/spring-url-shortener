package com.example.url_shortener.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User implements UserDetails {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    // Field-level initialization guarantees "ROLE_USER" is set whether using a builder or "new User()"
    @Column(nullable = false, length = 20)
    private String role = "ROLE_USER";

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)  // ✅ add insertable = false
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)  // ✅ add insertable = false
    private Instant updatedAt;

    // custom builder constructor that excludes auto generated fields
    @Builder
    public User(String email, String password, String role) {
        this.email = email;
        this.password = password;
        if (role != null) {
            this.role = role;
        }
    }

    // --- UserDetails Implementation ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getUsername() {
        return email;
    }

    // --- JPA-Safe Identity Methods ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id != null && Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode(); // Standard safe hash for entities with generated IDs
    }
}
