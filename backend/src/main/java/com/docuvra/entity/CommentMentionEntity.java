package com.docuvra.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "comment_mentions")
@Getter
@Setter
public class CommentMentionEntity {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private AnnotationCommentEntity comment;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mentioned_user_id", nullable = false)
    private UserEntity mentionedUser;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
