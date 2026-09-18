package com.grain.depot.repository;

import com.grain.depot.entity.Fumigation;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FumigationRepository extends JpaRepository<Fumigation, Long> {

    boolean existsByCode(String code);

    List<Fumigation> findAllByOrderByIdDesc();

    List<Fumigation> findByGranaryId(Long granaryId);

    List<Fumigation> findByGranaryIdAndStatus(Long granaryId, String status);

    /** 密闭中判定加行锁，并发开始/复检时只放一个进来。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Fumigation f where f.granaryId = :granaryId and f.status = '密闭中'")
    List<Fumigation> findActiveForUpdate(@Param("granaryId") Long granaryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Fumigation f where f.id = :id")
    Optional<Fumigation> findByIdForUpdate(@Param("id") Long id);
}
