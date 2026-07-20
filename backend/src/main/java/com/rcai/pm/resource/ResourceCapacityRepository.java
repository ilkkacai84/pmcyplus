package com.rcai.pm.resource; import org.springframework.data.jpa.repository.*; import java.time.LocalDate; import java.util.*;
public interface ResourceCapacityRepository extends JpaRepository<ResourceCapacity,Long>{
 @EntityGraph(attributePaths="user") List<ResourceCapacity> findByUserIdInAndDateBetween(List<Long> ids,LocalDate from,LocalDate to);
 Optional<ResourceCapacity> findByUserIdAndDate(Long userId,LocalDate date);
}
