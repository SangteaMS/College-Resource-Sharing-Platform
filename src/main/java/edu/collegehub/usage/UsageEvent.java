package edu.collegehub.usage;

import java.time.LocalDateTime;

import edu.collegehub.accounts.AppUser;
import edu.collegehub.resources.Resource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "usage_events")
public class UsageEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_type", nullable = false, length = 20)
    private String eventType;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "actor_id")
    private AppUser actor;
    @Column(name = "event_at", nullable = false)
    private LocalDateTime eventAt;

    protected UsageEvent() { }
    public UsageEvent(String eventType, Resource resource, AppUser actor) {
        this.eventType = eventType; this.resource = resource; this.actor = actor;
    }
    public String getEventType() { return eventType; }
    public Resource getResource() { return resource; }
    public LocalDateTime getEventAt() { return eventAt; }
    @PrePersist void onCreate() { if (eventAt == null) eventAt = LocalDateTime.now(); }
}
