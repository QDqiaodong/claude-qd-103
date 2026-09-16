package com.grain.depot.repository;

import com.grain.depot.entity.StockMove;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMoveRepository extends JpaRepository<StockMove, Long> {

    boolean existsByCode(String code);

    List<StockMove> findAllByOrderByIdDesc();
}
