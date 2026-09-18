package com.grain.depot.repository;

import com.grain.depot.entity.Granary;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GranaryRepository extends JpaRepository<Granary, Long> {

    boolean existsByCode(String code);

    List<Granary> findAllByOrderByIdAsc();

    /** 开始密闭时先把仓房行锁住，柜台两边同时点也只能串行开单。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Granary g where g.id = :id")
    Optional<Granary> findByIdForUpdate(@Param("id") Long id);
}
