package com.rcai.pm.merge;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/merges")
@PreAuthorize("hasRole('ADMIN')")
public class MergeController {
    private final MergeService service;

    public MergeController(MergeService service) {
        this.service = service;
    }

    @PostMapping("/preview")
    public MergeService.MergePreview preview(@Valid @RequestBody PreviewRequest request) {
        return service.preview(request.objectType(), request.sourceIds(), request.targetId());
    }

    @PostMapping("/execute")
    public MergeService.MergeResult execute(@Valid @RequestBody ExecuteRequest request,
                                            Authentication authentication) {
        return service.execute(request.objectType(), request.sourceIds(), request.targetId(),
            request.acceptedTargetFields(), authentication);
    }

    public record PreviewRequest(@NotNull MergeObjectType objectType, @NotEmpty List<Long> sourceIds,
                                 @NotNull Long targetId) {}
    public record ExecuteRequest(@NotNull MergeObjectType objectType, @NotEmpty List<Long> sourceIds,
                                 @NotNull Long targetId, @NotNull Set<String> acceptedTargetFields) {}
}
