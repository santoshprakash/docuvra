package com.docuvra.controller;

import com.docuvra.dto.AssignmentRequestResponse;
import com.docuvra.dto.RejectAssignmentRequest;
import com.docuvra.service.AssignmentRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AssignmentRequestController {

    private final AssignmentRequestService assignmentRequestService;

    @GetMapping("/api/assignment-requests/pending")
    public List<AssignmentRequestResponse> pendingRequests() {
        return assignmentRequestService.pendingRequests();
    }

    @PostMapping("/api/assignment-requests/{requestId}/approve")
    public AssignmentRequestResponse approve(@PathVariable UUID requestId) {
        return assignmentRequestService.approve(requestId);
    }

    @PostMapping("/api/assignment-requests/{requestId}/reject")
    public AssignmentRequestResponse reject(
            @PathVariable UUID requestId,
            @RequestBody(required = false) RejectAssignmentRequest request
    ) {
        return assignmentRequestService.reject(requestId, request);
    }
}
