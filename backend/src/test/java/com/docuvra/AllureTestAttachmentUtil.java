package com.docuvra;

import io.qameta.allure.Allure;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public final class AllureTestAttachmentUtil {

    private AllureTestAttachmentUtil() {
    }

    public static void attachText(String name, String content) {
        Allure.addAttachment(name, "text/plain", content == null ? "" : content, ".txt");
    }

    public static void attachJson(String name, String json) {
        Allure.addAttachment(name, "application/json", json == null ? "" : json, ".json");
    }

    public static void attachError(String name, Throwable throwable) {
        if (throwable == null) {
            attachText(name, "");
            return;
        }
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        attachText(name, writer.toString());
    }

    public static ResultHandler attachMockMvcExchange() {
        return (MvcResult result) -> {
            MockHttpServletRequest request = result.getRequest();
            attachText("MockMvc request URL", request.getMethod() + " " + request.getRequestURI()
                    + (request.getQueryString() == null ? "" : "?" + request.getQueryString()));
            String requestBody = bodyAsString(request.getContentAsByteArray());
            if (looksLikeJson(requestBody)) {
                attachJson("MockMvc request body", requestBody);
            } else {
                attachText("MockMvc request body", requestBody);
            }

            attachText("MockMvc response status", String.valueOf(result.getResponse().getStatus()));
            String responseBody = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
            if (looksLikeJson(responseBody)) {
                attachJson("MockMvc response body", responseBody);
            } else {
                attachText("MockMvc response body", responseBody);
            }
        };
    }

    private static boolean looksLikeJson(String content) {
        if (content == null) {
            return false;
        }
        String trimmed = content.trim();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }

    private static String bodyAsString(byte[] body) {
        if (body == null || body.length == 0) {
            return "";
        }
        return new String(body, StandardCharsets.UTF_8);
    }
}
