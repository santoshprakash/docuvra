package com.docuvra.entity;

import com.docuvra.enums.DocumentAssignmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_assignments")
@Getter
@Setter
public class DocumentAssignmentEntity {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentEntity document;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assigned_to_user_id", nullable = false)
    private UserEntity assignedToUser;

    @ManyToOne
    @JoinColumn(name = "assigned_by_user_id")
    private UserEntity assignedByUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DocumentAssignmentStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
