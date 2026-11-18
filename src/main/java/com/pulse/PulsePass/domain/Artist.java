package com.pulse.PulsePass.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "artists")
public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stage_name", nullable = false, unique = true, length = 120)
    private String stageName;

    @Column(name = "country", length = 80)
    private String country;

    @Column(name = "genre", length = 80)
    private String genre;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @ManyToMany(mappedBy = "artists")
    private Set<Event> events = new HashSet<>();

    public Artist() {
    }

    public Artist(String stageName, String country, String genre, boolean active) {
        this.stageName = stageName;
        this.country = country;
        this.genre = genre;
        this.active = active;
    }

    public Long getId() { return id; }
    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Set<Event> getEvents() { return events; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Artist other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Artist{id=" + id + ", stageName='" + stageName + "'}";
    }
}
