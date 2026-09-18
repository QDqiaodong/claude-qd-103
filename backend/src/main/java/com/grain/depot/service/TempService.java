package com.grain.depot.service;

import com.grain.depot.dto.BizException;
import com.grain.depot.entity.Fumigation;
import com.grain.depot.entity.Granary;
import com.grain.depot.entity.TempRecord;
import com.grain.depot.repository.FumigationRepository;
import com.grain.depot.repository.GranaryRepository;
import com.grain.depot.repository.TempRecordRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TempService {

    /** 粮温到了这个度数就算超温 */
    private static final double WARN_TEMPERATURE = 26.0;

    private final TempRecordRepository records;
    private final GranaryRepository granaries;
    private final FumigationRepository fumigations;

    public TempService(TempRecordRepository records, GranaryRepository granaries,
                       FumigationRepository fumigations) {
        this.records = records;
        this.granaries = granaries;
        this.fumigations = fumigations;
    }

    public List<TempRecord> list(Long granaryId, LocalDate recordDate) {
        return records.findAllByOrderByIdDesc().stream()
                .filter(r -> granaryId == null || granaryId.equals(r.granaryId))
                .filter(r -> recordDate == null || recordDate.equals(r.recordDate))
                .toList();
    }

    /** 这天这间仓是否处在熏蒸密闭期：密闭中的单按计划密闭起始日起算；已放行的单算到计划散气日。 */
    private boolean sealedOnDate(Long granaryId, LocalDate date) {
        for (Fumigation f : fumigations.findByGranaryId(granaryId)) {
            if (date.isBefore(f.sealDate)) {
                continue;
            }
            if ("密闭中".equals(f.status)) {
                return true;
            }
            if ("已完成".equals(f.status) && !date.isAfter(f.aerationDate)) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public TempRecord create(TempRecord input) {
        if (input.granaryId == null) {
            throw new BizException("请选择仓房");
        }
        if (input.recordDate == null) {
            throw new BizException("请填测温日期");
        }
        if (input.temperature == null) {
            throw new BizException("请填粮温");
        }
        if (input.humidity == null) {
            throw new BizException("请填仓内湿度");
        }
        if (input.humidity < 0 || input.humidity > 100) {
            throw new BizException("湿度要在 0 到 100 之间");
        }
        Granary granary = granaries.findById(input.granaryId)
                .orElseThrow(() -> new BizException("仓房不存在"));
        if ("维修".equals(granary.status)) {
            throw new BizException("仓房 " + granary.name + " 正在维修，不用测温");
        }
        // 同一间仓同一天只能有一条：密闭测温占了这天，日常测温就混不进来，反之亦然。
        if (!records.findByGranaryIdAndRecordDate(granary.id, input.recordDate).isEmpty()) {
            throw new BizException("仓房 " + granary.name + " " + input.recordDate
                    + " 已经测过温了，一天只量一次");
        }
        TempRecord saved = new TempRecord();
        saved.granaryId = granary.id;
        saved.recordDate = input.recordDate;
        saved.temperature = input.temperature;
        saved.humidity = input.humidity;
        saved.recorder = input.recorder;
        saved.result = input.temperature >= WARN_TEMPERATURE ? "超温" : "正常";
        // 密闭期照样测温，但结论由服务端按熏蒸单判成「密闭测温」，不用手填。
        saved.scene = sealedOnDate(granary.id, input.recordDate) ? "密闭测温" : "日常测温";
        return records.save(saved);
    }
}
