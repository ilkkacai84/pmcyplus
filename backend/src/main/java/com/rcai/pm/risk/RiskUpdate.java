package com.rcai.pm.risk;
import com.rcai.pm.user.UserAccount;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="risk_updates")
public class RiskUpdate {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="risk_id") private Risk risk;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="author_id") private UserAccount author;
 @Column(nullable=false,length=1000) private String note;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 protected RiskUpdate(){} public RiskUpdate(Risk risk,UserAccount author,String note){this.risk=risk;this.author=author;this.note=note;this.createdAt=Instant.now();}
 public Long getId(){return id;} public UserAccount getAuthor(){return author;} public String getNote(){return note;} public Instant getCreatedAt(){return createdAt;}
}
