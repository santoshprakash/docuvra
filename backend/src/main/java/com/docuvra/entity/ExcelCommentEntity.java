package com.docuvra.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "excel_comments")
public class ExcelCommentEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "version_id", nullable = false)
    private UUID versionId;

    @Column(name = "sheet_index", nullable = false)
    private Integer sheetIndex;

    @Column(name = "sheet_name", nullable = false, length = 255)
    private String sheetName;

    @Column(name = "start_cell", nullable = false, length = 20)
    private String startCell;

    @Column(name = "end_cell", length = 20)
    private String endCell;

    @Column(name = "start_row", nullable = false)
    private Integer startRow;

    @Column(name = "start_column", nullable = false)
    private Integer startColumn;

    @Column(name = "end_row")
    private Integer endRow;

    @Column(name = "end_column")
    private Integer endColumn;

    @Column(name = "comment_text", nullable = false, columnDefinition = "text")
    private String commentText;

    @Column(name = "created_by_name", nullable = false, length = 100)
    private String createdByName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
