package com.grain.depot.repository;

import com.grain.depot.entity.Granary;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GranaryRepository extends JpaRepository<Granary, Long> {

    boolean existsByCode(String code);

    List<Granary> findAllByOrderByIdAsc();
}
