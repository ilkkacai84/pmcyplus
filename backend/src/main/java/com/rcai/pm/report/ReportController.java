package com.rcai.pm.report;
import com.rcai.pm.project.ProjectType; import org.springframework.format.annotation.DateTimeFormat; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*; import java.time.LocalDate;
@RestController @RequestMapping("/api/reports") @PreAuthorize("hasAnyRole('ADMIN','PROJECT_MANAGER','DEPARTMENT_MANAGER')") public class ReportController{
 private final ReportService service; public ReportController(ReportService s){service=s;}
 @GetMapping public ReportService.Summary report(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,@RequestParam(required=false) ProjectType projectType,@RequestParam(required=false) Long projectId,@RequestParam(required=false) Long departmentId,@RequestParam(required=false) Long ownerId,@RequestParam(required=false) Long customerId,Authentication a){return service.report(from,to,projectType,projectId,departmentId,ownerId,customerId,a);}
}
