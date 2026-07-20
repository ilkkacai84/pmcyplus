package com.rcai.pm.resource;
import jakarta.validation.Valid; import org.springframework.format.annotation.DateTimeFormat; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*; import java.time.LocalDate;
@RestController @RequestMapping("/api/resources") public class ResourceController{
 private final ResourceService service; public ResourceController(ResourceService s){service=s;}
 @GetMapping public ResourceService.ResourceReport report(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,@RequestParam(required=false) Long projectId,Authentication a){return service.report(from,to,projectId,a);}
 @PutMapping("/calendar") @PreAuthorize("hasRole('ADMIN')") public ResourceService.CalendarDayView calendar(@Valid @RequestBody ResourceService.SaveCalendarDay r,Authentication a){return service.saveDay(r,a);}
 @PutMapping("/capacity") @PreAuthorize("hasRole('ADMIN')") public void capacity(@Valid @RequestBody ResourceService.SaveCapacity r,Authentication a){service.saveCapacity(r,a);}
}
