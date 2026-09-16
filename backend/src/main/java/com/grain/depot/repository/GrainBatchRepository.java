package com.grain.depot.repository;

import com.grain.depot.entity.GrainBatch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrainBatchRepository extends JpaRepository<GrainBatch, Long> {

    boolean existsByCode(String code);

    List<GrainBatch> findByGranaryIdAndStatus(Long granaryId, String status);

    List<GrainBatch> findAllByOrderByIdAsc();
}
