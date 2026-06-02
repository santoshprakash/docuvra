package com.docuvra;

import com.docuvra.repository.DocumentVersionRepository;
import com.docuvra.repository.UserRepository;
import com.docuvra.service.ImageConversionService;
import com.docuvra.service.OfficeConverterService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@ActiveProfiles("test")
@SpringBootTest
abstract class BaseIntegrationTest {

    private static final Path TEST_STORAGE = Path.of(System.getProperty("java.io.tmpdir"), "docuvra-test-storage-" + UUID.randomUUID());
    private static final Path TEST_CONVERTED = Path.of(System.getProperty("java.io.tmpdir"), "docuvra-test-converted-" + UUID.randomUUID());

    protected MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    protected ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    protected DocumentVersionRepository documentVersionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    protected OfficeConverterService officeConverterService;

    @MockitoBean
    protected ImageConversionService imageConversionService;

    protected String supervisorToken;
    protected String staffToken;
    protected String normalToken;

    @BeforeEach
    void authenticateDefaultUsers() throws Exception {
        seedDefaultUsers();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .alwaysDo(AllureTestAttachmentUtil.attachMockMvcExchange())
                .build();
        supervisorToken = login("supervisor@docuvra.local", "password");
        staffToken = login("staff@docuvra.local", "password");
        normalToken = login("normal@docuvra.local", "password");
    }

    protected String login(String usernameOrEmail, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "usernameOrEmail", usernameOrEmail,
                "password", password
        ));
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("token").asText();
    }

    protected JsonNode upload(String token, String fixture, String contentType) throws Exception {
        MockMultipartFile file = fixtureFile(fixture, contentType);
        String response = mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    protected MockMultipartFile fixtureFile(String fixture, String contentType) throws IOException {
        ClassPathResource resource = new ClassPathResource("test-files/" + fixture);
        return new MockMultipartFile("file", fixture, contentType, resource.getInputStream());
    }

    protected byte[] fixtureBytes(String fixture) throws IOException {
        return new ClassPathResource("test-files/" + fixture).getInputStream().readAllBytes();
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected UUID uuid(JsonNode node, String field) {
        return UUID.fromString(node.path(field).asText());
    }

    protected void copySamplePdf(Path target) {
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, fixtureBytes("sample.pdf"));
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void seedDefaultUsers() {
        seedUser("10000000-0000-0000-0000-000000000001", "normal", "normal@docuvra.local", "+10000000001", com.docuvra.enums.UserRole.NORMAL_USER);
        seedUser("10000000-0000-0000-0000-000000000002", "staff", "staff@docuvra.local", "+10000000002", com.docuvra.enums.UserRole.STAFF);
        seedUser("10000000-0000-0000-0000-000000000003", "supervisor", "supervisor@docuvra.local", "+10000000003", com.docuvra.enums.UserRole.SUPERVISOR);
    }

    private void seedUser(String id, String username, String email, String mobile, com.docuvra.enums.UserRole role) {
        if (userRepository.findByEmailIgnoreCaseAndRole(email, role).isPresent()) {
            return;
        }
        com.docuvra.entity.UserEntity user = new com.docuvra.entity.UserEntity();
        user.setId(UUID.fromString(id));
        user.setUsername(username);
        user.setEmail(email);
        user.setMobile(mobile);
        user.setPasswordHash(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);
        user.setForcePasswordChange(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}
