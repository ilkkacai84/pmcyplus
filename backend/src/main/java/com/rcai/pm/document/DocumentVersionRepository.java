package com.rcai.pm.document;
import org.springframework.data.jpa.repository.*; import java.util.List;
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion,Long>{
 @EntityGraph(attributePaths={"document","document.project","uploadedBy"}) List<DocumentVersion> findByDocumentIdOrderByVersionNoDesc(Long documentId);
 @EntityGraph(attributePaths={"document","document.project","uploadedBy"}) @Query("select v from DocumentVersion v where v.id=:id") java.util.Optional<DocumentVersion> findWithDocumentById(@org.springframework.data.repository.query.Param("id") Long id);
 @Query("select coalesce(max(v.versionNo),0) from DocumentVersion v where v.document.id=:id") int maxVersion(@org.springframework.data.repository.query.Param("id") Long id);
}
