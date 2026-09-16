package com.grain.depot.repository;

import com.grain.depot.entity.TempRecord;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TempRecordRepository extends JpaRepository<TempRecord, Long> {

    List<TempRecord> findByGranaryIdAndRecordDate(Long granaryId, LocalDate recordDate);

    List<TempRecord> findAllByOrderByIdDesc();
}
