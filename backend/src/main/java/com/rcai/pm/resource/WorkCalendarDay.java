package com.rcai.pm.resource;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.LocalDate;
@Entity @Table(name="work_calendar_days") public class WorkCalendarDay{
 @Id @Column(name="calendar_date") private LocalDate date; @Column(nullable=false) private boolean workday;
 @Column(name="standard_hours",nullable=false) private BigDecimal standardHours; private String note;
 protected WorkCalendarDay(){} public WorkCalendarDay(LocalDate d,boolean w,BigDecimal h,String n){date=d;workday=w;standardHours=h;note=n;}
 public void update(boolean w,BigDecimal h,String n){workday=w;standardHours=h;note=n;} public LocalDate getDate(){return date;} public boolean isWorkday(){return workday;} public BigDecimal getStandardHours(){return standardHours;} public String getNote(){return note;}
}
