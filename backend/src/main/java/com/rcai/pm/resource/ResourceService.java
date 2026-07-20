package com.rcai.pm.resource;

import com.rcai.pm.audit.AuditService;
import com.rcai.pm.common.ApiException;
import com.rcai.pm.project.*;
import com.rcai.pm.user.*;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service @Transactional(readOnly=true)
public class ResourceService {
 private final WorkCalendarRepository calendar; private final ResourceCapacityRepository capacities; private final UserAccountRepository users;
 private final ProjectRepository projects; private final ProjectMemberRepository members; private final TaskItemRepository tasks; private final WorklogRepository worklogs; private final AuditService audit;
 public ResourceService(WorkCalendarRepository c,ResourceCapacityRepository cp,UserAccountRepository u,ProjectRepository p,ProjectMemberRepository m,TaskItemRepository t,WorklogRepository w,AuditService a){calendar=c;capacities=cp;users=u;projects=p;members=m;tasks=t;worklogs=w;audit=a;}
 public ResourceReport report(LocalDate from,LocalDate to,Long projectId,Authentication auth){if(to.isBefore(from)||from.plusMonths(6).isBefore(to))throw new ApiException(HttpStatus.BAD_REQUEST,"查询区间必须在六个月内");UserAccount actor=current(auth);List<UserAccount> visible=visibleUsers(actor,projectId);if(visible.isEmpty())return new ResourceReport(from,to,"周一至周五默认 8 小时；节假日和个人容量覆盖优先",List.of());List<Long> ids=visible.stream().map(UserAccount::getId).toList();Map<LocalDate,WorkCalendarDay> overrides=calendar.findByDateBetweenOrderByDate(from,to).stream().collect(Collectors.toMap(WorkCalendarDay::getDate,Function.identity()));Map<String,BigDecimal> personal=capacities.findByUserIdInAndDateBetween(ids,from,to).stream().collect(Collectors.toMap(x->x.getUser().getId()+":"+x.getDate(),ResourceCapacity::getAvailableHours));List<TaskItem> assigned=tasks.findResourceTasks(ids,from.atStartOfDay(),to.atTime(23,59,59),List.of(TaskStatus.COMPLETED,TaskStatus.CANCELLED,TaskStatus.MERGED));Map<Long,BigDecimal> actual=worklogs.findByUserIdInAndWorkedOnBetween(ids,from,to).stream().collect(Collectors.groupingBy(x->x.getUser().getId(),Collectors.reducing(BigDecimal.ZERO,com.rcai.pm.project.Worklog::getHours,BigDecimal::add)));List<MemberLoad> result=new ArrayList<>();for(UserAccount user:visible){Map<LocalDate,BigDecimal> daily=new LinkedHashMap<>();BigDecimal capacity=BigDecimal.ZERO;for(LocalDate d=from;!d.isAfter(to);d=d.plusDays(1)){BigDecimal h=personal.getOrDefault(user.getId()+":"+d,standard(d,overrides));daily.put(d,BigDecimal.ZERO);capacity=capacity.add(h);}for(TaskItem task:assigned.stream().filter(x->x.getOwner().getId().equals(user.getId())).toList()){List<LocalDate> taskDays=dates(task.getPlannedStartAt().toLocalDate(),task.getPlannedEndAt().toLocalDate(),overrides);if(taskDays.isEmpty())continue;BigDecimal perDay=task.getEstimatedHours().divide(BigDecimal.valueOf(taskDays.size()),2,RoundingMode.HALF_UP);taskDays.stream().filter(d->!d.isBefore(from)&&!d.isAfter(to)).forEach(d->daily.computeIfPresent(d,(k,v)->v.add(perDay)));}BigDecimal allocated=daily.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add);boolean conflict=daily.entrySet().stream().anyMatch(e->e.getValue().compareTo(personal.getOrDefault(user.getId()+":"+e.getKey(),standard(e.getKey(),overrides)))>0);BigDecimal load=capacity.signum()==0?BigDecimal.ZERO:allocated.multiply(BigDecimal.valueOf(100)).divide(capacity,1,RoundingMode.HALF_UP);result.add(new MemberLoad(user.getId(),user.getDisplayName(),user.getDepartment()==null?null:user.getDepartment().getName(),capacity,allocated,actual.getOrDefault(user.getId(),BigDecimal.ZERO),load,conflict));}return new ResourceReport(from,to,"周一至周五默认 8 小时；节假日和个人容量覆盖优先",result);}
 @Transactional public CalendarDayView saveDay(SaveCalendarDay r,Authentication a){WorkCalendarDay d=calendar.findById(r.date()).orElseGet(()->new WorkCalendarDay(r.date(),r.workday(),r.standardHours(),r.note()));d.update(r.workday(),r.standardHours(),r.note());calendar.save(d);audit.log(a,"WORK_CALENDAR_UPDATED","CALENDAR",null,Map.of("date",r.date().toString(),"workday",r.workday()));return CalendarDayView.from(d);}
 @Transactional public void saveCapacity(SaveCapacity r,Authentication a){UserAccount u=users.findById(r.userId()).orElseThrow();ResourceCapacity c=capacities.findByUserIdAndDate(r.userId(),r.date()).orElseGet(()->new ResourceCapacity(u,r.date(),r.availableHours(),r.note()));c.update(r.availableHours(),r.note());capacities.save(c);}
 private List<UserAccount> visibleUsers(UserAccount a,Long projectId){if(a.getRoles().contains(Role.ADMIN))return users.findAllByOrderByDisplayNameAsc().stream().filter(u->u.getUserType()==UserType.INTERNAL).toList();if(a.getRoles().contains(Role.DEPARTMENT_MANAGER)&&a.getDepartmentId()!=null)return users.findDepartmentUsers(a.getDepartmentId());if(a.getRoles().contains(Role.PROJECT_MANAGER)&&projectId!=null){Project p=projects.findById(projectId).orElseThrow();if(!p.getManager().getId().equals(a.getId()))throw new ApiException(HttpStatus.FORBIDDEN,"只能查看自己管理项目的资源");return users.findAllById(members.findByProjectId(projectId).stream().map(ProjectMember::getUserId).toList());}return List.of(a);}
 private BigDecimal standard(LocalDate d,Map<LocalDate,WorkCalendarDay> o){WorkCalendarDay x=o.get(d);if(x!=null)return x.isWorkday()?x.getStandardHours():BigDecimal.ZERO;return d.getDayOfWeek().getValue()<=5?BigDecimal.valueOf(8):BigDecimal.ZERO;}
 private List<LocalDate> dates(LocalDate f,LocalDate t,Map<LocalDate,WorkCalendarDay> o){List<LocalDate> r=new ArrayList<>();for(LocalDate d=f;!d.isAfter(t);d=d.plusDays(1))if(standard(d,o).signum()>0)r.add(d);return r;}
 private UserAccount current(Authentication a){return users.findByUsernameIgnoreCase(a.getName()).orElseThrow();}
 public record SaveCalendarDay(@NotNull LocalDate date,boolean workday,@NotNull @DecimalMin("0") BigDecimal standardHours,String note){}
 public record SaveCapacity(@NotNull Long userId,@NotNull LocalDate date,@NotNull @DecimalMin("0") BigDecimal availableHours,String note){}
 public record CalendarDayView(LocalDate date,boolean workday,BigDecimal standardHours,String note){static CalendarDayView from(WorkCalendarDay d){return new CalendarDayView(d.getDate(),d.isWorkday(),d.getStandardHours(),d.getNote());}}
 public record ResourceReport(LocalDate from,LocalDate to,String calculationRule,List<MemberLoad> members){}
 public record MemberLoad(Long userId,String userName,String departmentName,BigDecimal capacityHours,BigDecimal allocatedHours,BigDecimal actualHours,BigDecimal loadRate,boolean conflict){}
}
