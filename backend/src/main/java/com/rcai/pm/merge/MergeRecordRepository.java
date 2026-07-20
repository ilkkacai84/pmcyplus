package com.rcai.pm.merge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MergeRecordRepository extends JpaRepository<MergeRecord, Long> {
    List<MergeRecord> findTop100ByOrderByCreatedAtDesc();
}
