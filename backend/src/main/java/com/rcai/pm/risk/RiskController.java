package com.rcai.pm.risk;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api")
public class RiskController{
 private final RiskService service; public RiskController(RiskService service){this.service=service;}
 @GetMapping("/projects/{projectId}/risks") public List<RiskService.RiskView> list(@PathVariable Long projectId,Authentication a){return service.list(projectId,a);}
 @PostMapping("/projects/{projectId}/risks") public RiskService.RiskView create(@PathVariable Long projectId,@Valid @RequestBody RiskService.CreateRisk r,Authentication a){return service.create(projectId,r,a);}
 @PutMapping("/risks/{id}") public RiskService.RiskView update(@PathVariable Long id,@Valid @RequestBody RiskService.UpdateRisk r,Authentication a){return service.update(id,r,a);}
}
