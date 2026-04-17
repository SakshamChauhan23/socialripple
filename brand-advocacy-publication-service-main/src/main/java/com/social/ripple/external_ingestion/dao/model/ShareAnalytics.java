package com.social.ripple.external_ingestion.dao.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "share_analytics")
@Getter
@Setter
public class ShareAnalytics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_share_id", nullable = false, unique = true)
    private Long externalShareId;

    @Column(name = "platform", nullable = false, length = 20)
    private String platform;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "impressions", nullable = false)
    private int impressions;

    @Column(name = "reach", nullable = false)
    private int reach;

    @Column(name = "likes", nullable = false)
    private int likes;

    @Column(name = "comments", nullable = false)
    private int comments;

    @Column(name = "shares", nullable = false)
    private int shares;

    @Column(name = "saves", nullable = false)
    private int saves;

    @Column(name = "bookmarks", nullable = false)
    private int bookmarks;

    @Column(name = "retweets", nullable = false)
    private int retweets;

    @Column(name = "replies", nullable = false)
    private int replies;

    @Column(name = "quotes", nullable = false)
    private int quotes;

    @Column(name = "clicks", nullable = false)
    private int clicks;

    @Column(name = "video_views", nullable = false)
    private int videoViews;

    @Column(name = "engagements", nullable = false)
    private int engagements;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (fetchedAt == null) fetchedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
