package com.rcai.pm.resource;
import com.rcai.pm.user.UserAccount; import jakarta.persistence.*; import java.math.BigDecimal; import java.time.LocalDate;
@Entity @Table(name="resource_capacities",uniqueConstraints=@UniqueConstraint(name="uk_resource_capacity",columnNames={"user_id","capacity_date"})) public class ResourceCapacity{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id") private UserAccount user;
 @Column(name="capacity_date",nullable=false) private LocalDate date; @Column(name="available_hours",nullable=false) private BigDecimal availableHours; private String note;
 protected ResourceCapacity(){} public ResourceCapacity(UserAccount u,LocalDate d,BigDecimal h,String n){user=u;date=d;availableHours=h;note=n;} public void update(BigDecimal h,String n){availableHours=h;note=n;}
 public Long getId(){return id;} public UserAccount getUser(){return user;} public LocalDate getDate(){return date;} public BigDecimal getAvailableHours(){return availableHours;} public String getNote(){return note;}
}
