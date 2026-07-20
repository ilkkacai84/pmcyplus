package com.rcai.pm.risk;
import org.springframework.data.jpa.repository.*;
import java.util.List;
public interface RiskUpdateRepository extends JpaRepository<RiskUpdate,Long>{
 @EntityGraph(attributePaths="author") List<RiskUpdate> findByRiskIdOrderByCreatedAtAsc(Long riskId);
}
