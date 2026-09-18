package com.grain.depot.repository;

import com.grain.depot.entity.Granary;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface GranaryRepository extends JpaRepository<Granary, Long> {

    boolean existsByCode(String code);

    List<Granary> findAllByOrderByIdAsc();

    /** 状态机动作（开始密闭等）先锁仓房行，柜台两边同时点也只过一个。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Granary g where g.id = :id")
    Optional<Granary> findByIdForUpdate(@Param("id") Long id);
}
