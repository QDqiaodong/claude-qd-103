package com.grain.depot.repository;

import com.grain.depot.entity.Fumigation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FumigationRepository extends JpaRepository<Fumigation, Long> {

    boolean existsByCode(String code);

    /** 这间仓是不是正挂着一张密闭中的单（sealed_granary_id 有唯一约束兜底）。 */
    boolean existsBySealedGranaryId(Long granaryId);

    List<Fumigation> findAllByOrderByIdDesc();

    List<Fumigation> findByGranaryIdOrderByIdDesc(Long granaryId);
}
