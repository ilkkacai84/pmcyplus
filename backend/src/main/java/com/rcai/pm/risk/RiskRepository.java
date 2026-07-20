package com.rcai.pm.risk;
import org.springframework.data.jpa.repository.*;
import java.util.List;
public interface RiskRepository extends JpaRepository<Risk,Long>{
 @EntityGraph(attributePaths={"project","milestone","task","owner"}) List<Risk> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
