package com.docuvra.entity;

import com.docuvra.enums.DocumentAssignmentRequestStatus;
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
@Table(name = "document_assignment_requests")
@Getter
@Setter
public class DocumentAssignmentRequestEntity {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentEntity document;

    @ManyToOne(optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private UserEntity requestedByUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DocumentAssignmentRequestStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @ManyToOne
    @JoinColumn(name = "reviewed_by_user_id")
    private UserEntity reviewedByUser;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_comment")
    private String reviewComment;
}
