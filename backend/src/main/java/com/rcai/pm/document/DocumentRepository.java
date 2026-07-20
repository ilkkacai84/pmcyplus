package com.rcai.pm.document;
import org.springframework.data.jpa.repository.*; import java.util.List;
public interface DocumentRepository extends JpaRepository<Document,Long>{@EntityGraph(attributePaths={"project","createdBy"}) List<Document> findByProjectIdOrderByCreatedAtDesc(Long projectId);}
