package com.saurabh.taskcollab.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "activity_logs")
@Getter
@Setter
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Board board;

    private String action;

    private String entityType;

    private Long entityId;

    @Column(length = 2000)
    private String description;

    @ManyToOne(optional = false)
    private User user;

    private LocalDateTime createdAt = LocalDateTime.now();
}
