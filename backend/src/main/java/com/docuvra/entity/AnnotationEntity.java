package com.docuvra.entity;

import com.docuvra.enums.AnnotationType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "annotations")
public class AnnotationEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentEntity document;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    private DocumentVersionEntity version;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "annotation_type", nullable = false, length = 40)
    private AnnotationType annotationType;

    @Column(name = "x_percent", nullable = false)
    private Double xPercent;

    @Column(name = "y_percent", nullable = false)
    private Double yPercent;

    @Column(name = "width_percent", nullable = false)
    private Double widthPercent;

    @Column(name = "height_percent", nullable = false)
    private Double heightPercent;

    @Column(name = "pixel_x", nullable = false)
    private Double pixelX;

    @Column(name = "pixel_y", nullable = false)
    private Double pixelY;

    @Column(name = "pixel_width", nullable = false)
    private Double pixelWidth;

    @Column(name = "pixel_height", nullable = false)
    private Double pixelHeight;

    @Column(name = "page_render_width", nullable = false)
    private Double pageRenderWidth;

    @Column(name = "page_render_height", nullable = false)
    private Double pageRenderHeight;

    @Column(name = "color", length = 40)
    private String color;

    @Column(name = "stroke_width")
    private Double strokeWidth;

    @Column(name = "selected_text", columnDefinition = "text")
    private String selectedText;

    @Column(name = "drawing_data", columnDefinition = "text")
    private String drawingData;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "annotation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<AnnotationCommentEntity> comments = new ArrayList<>();
}
