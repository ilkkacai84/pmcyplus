package com.rcai.pm.document;
import com.rcai.pm.project.Project; import com.rcai.pm.user.UserAccount; import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="documents")
public class Document{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="project_id") private Project project;
 @Column(nullable=false,length=240) private String title; @Column(name="customer_visible",nullable=false) private boolean customerVisible;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="created_by") private UserAccount createdBy;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 protected Document(){} public Document(Project p,String t,boolean v,UserAccount u){project=p;title=t;customerVisible=v;createdBy=u;createdAt=Instant.now();}
 public void update(String t,boolean v){title=t;customerVisible=v;} public Long getId(){return id;} public Project getProject(){return project;}
 public String getTitle(){return title;} public boolean isCustomerVisible(){return customerVisible;} public UserAccount getCreatedBy(){return createdBy;} public Instant getCreatedAt(){return createdAt;}
}
