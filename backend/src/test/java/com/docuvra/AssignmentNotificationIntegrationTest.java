package com.docuvra;

import com.docuvra.entity.UserEntity;
import com.docuvra.enums.UserRole;
import com.docuvra.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssignmentNotificationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void supervisorAssignsDocumentToStaffAndCreatesAssignmentNotification() throws Exception {
        UserEntity staff = staffUser();
        JsonNode upload = upload(supervisorToken, "sample.pdf", "application/pdf");
        UUID documentId = uuid(upload, "documentId");

        mockMvc.perform(post("/api/documents/{documentId}/assignments", documentId)
                        .header("Authorization", bearer(supervisorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("staffUserId", staff.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(staff.getId().toString()));

        String staffSummary = mockMvc.perform(get("/api/notifications/summary")
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(objectMapper.readTree(staffSummary).path("unreadCount").asLong()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void staffRequestsUnassignedDocumentAndSupervisorApprovesRequest() throws Exception {
        JsonNode upload = upload(supervisorToken, "sample.pdf", "application/pdf");
        UUID documentId = uuid(upload, "documentId");

        JsonNode request = requestAssignment(documentId);
        UUID requestId = uuid(request, "requestId");
        assertThat(request.path("status").asText()).isEqualTo("PENDING");

        mockMvc.perform(get("/api/assignment-requests/pending")
                        .header("Authorization", bearer(supervisorToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/assignment-requests/{requestId}/approve", requestId)
                        .header("Authorization", bearer(supervisorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/documents/{documentId}", documentId)
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments.length()").value(1));
    }

    @Test
    void supervisorRejectsAssignmentRequestAndStaffReceivesNotification() throws Exception {
        JsonNode upload = upload(supervisorToken, "sample.pdf", "application/pdf");
        UUID documentId = uuid(upload, "documentId");
        UUID requestId = uuid(requestAssignment(documentId), "requestId");

        mockMvc.perform(post("/api/assignment-requests/{requestId}/reject", requestId)
                        .header("Authorization", bearer(supervisorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("comment", "Not needed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].notificationType").exists());
    }

    @Test
    void notificationUnreadCountMarkReadAndMarkAllRead() throws Exception {
        UserEntity staff = staffUser();
        JsonNode upload = upload(supervisorToken, "sample.pdf", "application/pdf");
        UUID documentId = uuid(upload, "documentId");
        mockMvc.perform(post("/api/documents/{documentId}/assignments", documentId)
                        .header("Authorization", bearer(supervisorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("staffUserId", staff.getId()))))
                .andExpect(status().isCreated());

        String notificationsBody = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode notifications = objectMapper.readTree(notificationsBody);
        UUID notificationId = UUID.fromString(notifications.get(0).path("notificationId").asText());

        mockMvc.perform(post("/api/notifications/{notificationId}/read", notificationId)
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/notifications/read-all")
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/notifications/summary")
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    private JsonNode requestAssignment(UUID documentId) throws Exception {
        String response = mockMvc.perform(post("/api/documents/{documentId}/assignment-requests", documentId)
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private UserEntity staffUser() {
        return userRepository.findByEmailIgnoreCaseAndRole("staff@docuvra.local", UserRole.STAFF).orElseThrow();
    }
}
