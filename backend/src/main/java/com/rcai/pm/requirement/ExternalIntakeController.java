package com.rcai.pm.requirement;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/intake")
public class ExternalIntakeController {
    private final ExternalIntakeService service;

    public ExternalIntakeController(ExternalIntakeService service) {
        this.service = service;
    }

    @PostMapping("/{source}")
    public RequirementService.RequirementView receive(
        @PathVariable RequirementSource source,
        @RequestHeader(value = "X-Intake-Token", required = false) String token,
        @Valid @RequestBody ExternalIntakeService.IntakeRequest request
    ) {
        return service.receive(source, request, token);
    }
}
