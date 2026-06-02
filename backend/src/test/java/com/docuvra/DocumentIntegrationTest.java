package com.docuvra;

import com.docuvra.entity.DocumentVersionEntity;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentIntegrationTest extends BaseIntegrationTest {

    @Test
    void uploadsSupportedFilesAndPersistsMetadataDiskCopyAndByteaBackup() throws Exception {
        assertUploadPersists("sample.pdf", "application/pdf");
        assertUploadPersists("sample.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertUploadPersists("sample.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertUploadPersists("sample.png", "image/png");
    }

    @Test
    void rejectsFileOverFiftyMegabytes() throws Exception {
        byte[] oversized = new byte[(50 * 1024 * 1024) + 1];
        MockMultipartFile file = new MockMultipartFile("file", "too-large.pdf", "application/pdf", oversized);

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .header("Authorization", bearer(supervisorToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadsFetchesAndDeletesDocumentVersions() throws Exception {
        JsonNode first = upload(supervisorToken, "sample.pdf", "application/pdf");
        UUID documentId = uuid(first, "documentId");
        MockMultipartFile secondFile = fixtureFile("sample.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        String secondBody = mockMvc.perform(multipart("/api/documents/{documentId}/versions", documentId)
                        .file(secondFile)
                        .header("Authorization", bearer(supervisorToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNumber").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID secondVersionId = uuid(objectMapper.readTree(secondBody), "versionId");

        mockMvc.perform(get("/api/documents/{documentId}", documentId)
                        .header("Authorization", bearer(supervisorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versions.length()").value(2));

        mockMvc.perform(delete("/api/documents/{documentId}/versions/{versionId}", documentId, secondVersionId)
                        .header("Authorization", bearer(supervisorToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/documents/{documentId}", documentId)
                        .header("Authorization", bearer(supervisorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versions.length()").value(1));
    }

    @Test
    void viewsPdfDownloadsOriginalAndFetchesThumbnail() throws Exception {
        JsonNode upload = upload(supervisorToken, "sample.pdf", "application/pdf");
        UUID documentId = uuid(upload, "documentId");
        UUID versionId = uuid(upload, "versionId");

        mockMvc.perform(get("/api/documents/{documentId}/versions/{versionId}/view", documentId, versionId)
                        .header("Authorization", bearer(supervisorToken)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        mockMvc.perform(get("/api/documents/{documentId}/versions/{versionId}/download", documentId, versionId)
                        .header("Authorization", bearer(supervisorToken)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")));

        mockMvc.perform(get("/api/documents/{documentId}/versions/{versionId}/thumbnail", documentId, versionId)
                        .header("Authorization", bearer(supervisorToken)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));
    }

    @Test
    void regeneratesConvertedPreviewAndRestoresOriginalWhenLocalFileIsMissing() throws Exception {
        JsonNode upload = upload(supervisorToken, "sample.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        UUID documentId = uuid(upload, "documentId");
        UUID versionId = uuid(upload, "versionId");
        DocumentVersionEntity version = documentVersionRepository.findByDocumentIdAndId(documentId, versionId).orElseThrow();
        Files.delete(Path.of(version.getLocalFilePath()));

        doAnswer(invocation -> {
            Path finalPdf = invocation.getArgument(2);
            copySamplePdf(finalPdf);
            return finalPdf;
        }).when(officeConverterService).convert(any(Path.class), any(Path.class), any(Path.class));

        mockMvc.perform(get("/api/documents/{documentId}/versions/{versionId}/view", documentId, versionId)
                        .header("Authorization", bearer(supervisorToken)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        assertThat(Files.exists(Path.of(version.getLocalFilePath()))).isTrue();
        mockMvc.perform(get("/api/documents/{documentId}/versions/{versionId}/converted/status", documentId, versionId)
                        .header("Authorization", bearer(supervisorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.converted").value(true));
    }

    private void assertUploadPersists(String fixture, String contentType) throws Exception {
        JsonNode uploaded = upload(supervisorToken, fixture, contentType);
        UUID documentId = uuid(uploaded, "documentId");
        UUID versionId = uuid(uploaded, "versionId");
        DocumentVersionEntity version = documentVersionRepository.findByDocumentIdAndId(documentId, versionId).orElseThrow();

        assertThat(version.getOriginalFileName()).isEqualTo(fixture);
        assertThat(version.getMimeType()).isEqualTo(contentType);
        assertThat(version.getFileSize()).isGreaterThan(0);
        assertThat(version.getFileData()).isNotEmpty();
        assertThat(Files.exists(Path.of(version.getLocalFilePath()))).isTrue();
    }
}
