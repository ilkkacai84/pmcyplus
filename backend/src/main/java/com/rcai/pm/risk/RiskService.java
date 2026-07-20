package com.rcai.pm.risk;

import com.rcai.pm.audit.AuditService;
import com.rcai.pm.common.ApiException;
import com.rcai.pm.notification.NotificationService;
import com.rcai.pm.project.*;
import com.rcai.pm.user.*;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service @Transactional(readOnly=true)
public class RiskService {
 private final RiskRepository risks; private final RiskUpdateRepository updates; private final ProjectRepository projects;
 private final ProjectMemberRepository members; private final MilestoneRepository milestones; private final TaskItemRepository tasks;
 private final UserAccountRepository users; private final AuditService audit; private final NotificationService notifications;
 public RiskService(RiskRepository risks,RiskUpdateRepository updates,ProjectRepository projects,ProjectMemberRepository members,
  MilestoneRepository milestones,TaskItemRepository tasks,UserAccountRepository users,AuditService audit,NotificationService notifications){
  this.risks=risks;this.updates=updates;this.projects=projects;this.members=members;this.milestones=milestones;this.tasks=tasks;this.users=users;this.audit=audit;this.notifications=notifications;
 }
 public List<RiskView> list(Long projectId,Authentication auth){ UserAccount actor=access(projectId,auth); return risks.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
  .filter(r->!actor.getRoles().contains(Role.DEPARTMENT_MANAGER)||Objects.equals(actor.getDepartmentId(),r.getOwner().getDepartmentId()))
  .map(this::view).toList(); }
 @Transactional public RiskView create(Long projectId,CreateRisk req,Authentication auth){ Project p=managed(projectId,auth); UserAccount owner=user(req.ownerId());
  Milestone m=req.milestoneId()==null?null:milestones.findById(req.milestoneId()).filter(x->x.getProject().getId().equals(projectId)).orElseThrow(()->bad("里程碑不属于项目"));
  TaskItem t=req.taskId()==null?null:tasks.findById(req.taskId()).filter(x->x.getProject().getId().equals(projectId)).orElseThrow(()->bad("任务不属于项目"));
  Risk risk=risks.save(new Risk(p,m,t,req.title().trim(),req.description(),req.riskLevel(),owner)); audit.log(auth,"RISK_CREATED","RISK",risk.getId(),Map.of("level",req.riskLevel().name()));
  if(List.of(RiskLevel.HIGH,RiskLevel.CRITICAL).contains(req.riskLevel())) notifications.notify(List.of(p.getManager()),"HIGH_RISK","项目出现高风险",risk.getTitle(),"RISK",risk.getId(),0);
  return view(risk); }
 @Transactional public RiskView update(Long id,UpdateRisk req,Authentication auth){ Risk r=risks.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"风险不存在")); UserAccount a=access(r.getProject().getId(),auth);
  if(!a.getRoles().contains(Role.ADMIN)&&!r.getProject().getManager().getId().equals(a.getId())&&!r.getOwner().getId().equals(a.getId())) throw new ApiException(HttpStatus.FORBIDDEN,"无权更新风险");
  r.update(req.riskLevel(),req.status()); if(req.note()!=null&&!req.note().isBlank()) updates.save(new RiskUpdate(r,a,req.note().trim())); audit.log(a,"RISK_UPDATED","RISK",id,Map.of("status",req.status().name(),"level",req.riskLevel().name())); return view(r); }
 private RiskView view(Risk r){return new RiskView(r.getId(),r.getTitle(),r.getDescription(),r.getRiskLevel(),r.getStatus(),r.getOwner().getId(),r.getOwner().getDisplayName(),r.getMilestone()==null?null:r.getMilestone().getId(),r.getTask()==null?null:r.getTask().getId(),r.getCreatedAt(),updates.findByRiskIdOrderByCreatedAtAsc(r.getId()).stream().map(UpdateView::from).toList());}
 private Project managed(Long id,Authentication auth){Project p=projects.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"项目不存在"));UserAccount a=current(auth);if(!a.getRoles().contains(Role.ADMIN)&&!p.getManager().getId().equals(a.getId()))throw new ApiException(HttpStatus.FORBIDDEN,"只有项目经理可以管理风险");return p;}
 private UserAccount access(Long id,Authentication auth){Project p=projects.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"项目不存在"));UserAccount a=current(auth);boolean ok=a.getRoles().contains(Role.ADMIN)||p.getManager().getId().equals(a.getId())||members.existsByProjectIdAndUserId(id,a.getId())||a.getRoles().contains(Role.DEPARTMENT_MANAGER);if(!ok||a.getUserType()==UserType.CUSTOMER)throw new ApiException(HttpStatus.FORBIDDEN,"无权查看风险");return a;}
 private UserAccount current(Authentication a){return users.findByUsernameIgnoreCase(a.getName()).orElseThrow();} private UserAccount user(Long id){return users.findById(id).orElseThrow(()->bad("负责人不存在"));} private ApiException bad(String m){return new ApiException(HttpStatus.BAD_REQUEST,m);}
 public record CreateRisk(@NotBlank String title,String description,@NotNull RiskLevel riskLevel,@NotNull Long ownerId,Long milestoneId,Long taskId){}
 public record UpdateRisk(@NotNull RiskLevel riskLevel,@NotNull RiskStatus status,String note){}
 public record RiskView(Long id,String title,String description,RiskLevel riskLevel,RiskStatus status,Long ownerId,String ownerName,Long milestoneId,Long taskId,Instant createdAt,List<UpdateView> updates){}
 public record UpdateView(Long id,String authorName,String note,Instant createdAt){static UpdateView from(RiskUpdate u){return new UpdateView(u.getId(),u.getAuthor().getDisplayName(),u.getNote(),u.getCreatedAt());}}
}
