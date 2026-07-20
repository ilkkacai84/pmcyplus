package com.rcai.pm.user;

import com.rcai.pm.audit.AuditService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {
    private final DepartmentService service;
    private final AuditService audit;

    public DepartmentController(DepartmentService service, AuditService audit) {
        this.service = service;
        this.audit = audit;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'DEPARTMENT_MANAGER')")
    public List<DepartmentService.DepartmentView> list() {
        return service.list();
    }

    @GetMapping("/my-scope")
    @PreAuthorize("hasRole('DEPARTMENT_MANAGER')")
    public DepartmentService.DepartmentScope myScope(Authentication authentication) {
        return service.myScope(authentication);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentService.DepartmentView create(@Valid @RequestBody DepartmentService.SaveDepartment request,
                                                   Authentication authentication) {
        DepartmentService.DepartmentView department = service.create(request);
        audit.log(authentication, "DEPARTMENT_CREATED", "DEPARTMENT", department.id(), Map.of("name", department.name()));
        return department;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentService.DepartmentView update(@PathVariable Long id,
                                                    @Valid @RequestBody DepartmentService.SaveDepartment request,
                                                    Authentication authentication) {
        DepartmentService.DepartmentView department = service.update(id, request);
        audit.log(authentication, "DEPARTMENT_UPDATED", "DEPARTMENT", department.id(), Map.of("name", department.name()));
        return department;
    }
}
