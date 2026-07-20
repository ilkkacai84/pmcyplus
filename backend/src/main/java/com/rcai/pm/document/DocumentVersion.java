package com.rcai.pm.document;
import com.rcai.pm.user.UserAccount; import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="document_versions",uniqueConstraints=@UniqueConstraint(name="uk_document_version",columnNames={"document_id","version_no"}))
public class DocumentVersion{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="document_id") private Document document;
 @Column(name="version_no",nullable=false) private int versionNo; @Column(name="storage_key",nullable=false,length=500) private String storageKey;
 @Column(name="file_name",nullable=false) private String fileName; @Column(name="content_type") private String contentType;
 @Column(name="file_size",nullable=false) private long fileSize; @Column(length=1000) private String note;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="uploaded_by") private UserAccount uploadedBy;
 @Column(name="uploaded_at",nullable=false,updatable=false) private Instant uploadedAt;
 protected DocumentVersion(){} public DocumentVersion(Document d,int n,String k,String f,String c,long s,String note,UserAccount u){document=d;versionNo=n;storageKey=k;fileName=f;contentType=c;fileSize=s;this.note=note;uploadedBy=u;uploadedAt=Instant.now();}
 public Long getId(){return id;} public Document getDocument(){return document;} public int getVersionNo(){return versionNo;} public String getStorageKey(){return storageKey;}
 public String getFileName(){return fileName;} public String getContentType(){return contentType;} public long getFileSize(){return fileSize;} public String getNote(){return note;} public UserAccount getUploadedBy(){return uploadedBy;} public Instant getUploadedAt(){return uploadedAt;}
}
