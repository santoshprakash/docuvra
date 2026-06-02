package com.docuvra;

import com.docuvra.entity.UserEntity;
import com.docuvra.enums.AnnotationType;
import com.docuvra.enums.UserRole;
import com.docuvra.repository.AnnotationRepository;
import com.docuvra.repository.CommentMentionRepository;
import com.docuvra.repository.NotificationRepository;
import com.docuvra.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnnotationRoleIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnotationRepository annotationRepository;

    @Autowired
    private CommentMentionRepository commentMentionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void createsAllSupportedAnnotationTypesAndLoadsPageAnnotations() throws Exception {
        JsonNode upload = upload(supervisorToken, "sample.pdf", "application/pdf");
        UUID documentId = uuid(upload, "documentId");
        UUID versionId = uuid(upload, "versionId");

        for (AnnotationType type : AnnotationType.values()) {
            JsonNode annotation = createAnnotation(supervisorToken, documentId, versionId, type, List.of());
            assertThat(annotation.path("annotationType").asText()).isEqualTo(type.name());
            assertThat(annotation.path("xPercent").asDouble()).isEqualTo(10.0);
            assertThat(annotation.path("yPercent").asDouble()).isEqualTo(20.0);
        }

        mockMvc.perform(get("/api/documents/{documentId}/versions/{versionId}/pages/1/annotations", documentId, versionId)
                        .header("Authorization", bearer(supervisorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(AnnotationType.values().length));
    }

    @Test
    void createsRepliesMentionsNotificationsAndDeletesLinkedCommentAnnotation() throws Exception {
        UserEntity normal = userRepository.findByEmailIgnoreCaseAndRole("normal@docuvra.local", UserRole.NORMAL_USER).orElseThrow();
        JsonNode upload = upload(supervisorToken, "sample.pdf", "application/pdf");
        UUID documentId = uuid(upload, "documentId");
        UUID versionId = uuid(upload, "versionId");
        JsonNode annotation = createAnnotation(supervisorToken, documentId, versionId, AnnotationType.COMMENT, List.of(normal.getId()));
        UUID annotationId = uuid(annotation, "annotationId");
        UUID firstCommentId = UUID.fromString(annotation.path("comments").get(0).path("commentId").asText());

        assertThat(commentMentionRepository.existsByCommentIdAndMentionedUserId(firstCommentId, normal.getId())).isTrue();
        assertThat(notificationRepository.countByUserIdAndReadFlagFalse(normal.getId())).isGreaterThanOrEqualTo(1);

        JsonNode reply = createComment(normalToken, annotationId, "Normal user reply", List.of());
        assertThat(reply.path("commentText").asText()).isEqualTo("Normal user reply");

        mockMvc.perform(get("/api/annotations/{annotationId}/comments", annotationId)
                        .header("Authorization", bearer(normalToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(delete("/api/annotations/comments/{commentId}", firstCommentId)
                        .header("Authorization", bearer(supervisorToken)))
                .andExpect(status().isNoContent());
        assertThat(annotationRepository.existsById(annotationId)).isFalse();
    }

    @Test
    void enforcesRoleAccessForAnnotationAndCommentOperations() throws Exception {
        UserEntity normal = userRepository.findByEmailIgnoreCaseAndRole("normal@docuvra.local", UserRole.NORMAL_USER).orElseThrow();
        JsonNode normalUpload = upload(normalToken, "sample.pdf", "application/pdf");
        UUID normalDocumentId = uuid(normalUpload, "documentId");
        UUID normalVersionId = uuid(normalUpload, "versionId");

        mockMvc.perform(post("/api/documents/{documentId}/versions/{versionId}/annotations", normalDocumentId, normalVersionId)
                        .header("Authorization", bearer(normalToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annotationBody(AnnotationType.HIGHLIGHT, "normal root", List.of())))
                .andExpect(status().isForbidden());

        JsonNode staffAnnotation = createAnnotation(staffToken, normalDocumentId, normalVersionId, AnnotationType.COMMENT, List.of(normal.getId()));
        UUID annotationId = uuid(staffAnnotation, "annotationId");
        UUID commentId = UUID.fromString(staffAnnotation.path("comments").get(0).path("commentId").asText());

        mockMvc.perform(delete("/api/annotations/{annotationId}", annotationId)
                        .header("Authorization", bearer(normalToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/annotations/comments/{commentId}", commentId)
                        .header("Authorization", bearer(normalToken)))
                .andExpect(status().isForbidden());

        createComment(staffToken, annotationId, "Staff comment", List.of());
        createComment(supervisorToken, annotationId, "Supervisor comment", List.of());
        createComment(normalToken, annotationId, "Mentioned normal reply", List.of());
    }

    protected JsonNode createAnnotation(String token, UUID documentId, UUID versionId, AnnotationType type, List<UUID> mentionedUserIds) throws Exception {
        String response = mockMvc.perform(post("/api/documents/{documentId}/versions/{versionId}/annotations", documentId, versionId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annotationBody(type, "Comment for " + type, mentionedUserIds)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    protected JsonNode createComment(String token, UUID annotationId, String text, List<UUID> mentionedUserIds) throws Exception {
        String response = mockMvc.perform(post("/api/annotations/{annotationId}/comments", annotationId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "commentText", text,
                                "mentionedUserIds", mentionedUserIds
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private String annotationBody(AnnotationType type, String commentText, List<UUID> mentionedUserIds) throws Exception {
        return objectMapper.writeValueAsString(Map.ofEntries(
                entry("pageNumber", 1),
                entry("annotationType", type.name()),
                entry("xPercent", 10.0),
                entry("yPercent", 20.0),
                entry("widthPercent", 30.0),
                entry("heightPercent", 12.0),
                entry("pixelX", 100.0),
                entry("pixelY", 200.0),
                entry("pixelWidth", 300.0),
                entry("pixelHeight", 120.0),
                entry("pageRenderWidth", 1000.0),
                entry("pageRenderHeight", 1400.0),
                entry("color", "#2563eb"),
                entry("strokeWidth", 2.0),
                entry("selectedText", "selected words"),
                entry("drawingData", "[[1,2],[3,4]]"),
                entry("commentText", commentText),
                entry("mentionedUserIds", mentionedUserIds)
        ));
    }
}
