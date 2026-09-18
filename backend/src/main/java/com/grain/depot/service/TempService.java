package com.grain.depot.service;

import com.grain.depot.dto.BizException;
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
        if (!records.findByGranaryIdAndRecordDate(granary.id, input.recordDate).isEmpty()) {
            throw new BizException("仓房 " + granary.name + " " + input.recordDate
                    + " 已经测过温了，一天只量一次（密闭期测温也不能混进第二条）");
        }
        // 密闭期间测温照常允许，但这条要盖上「密闭期测温」的戳，跟日常测温区分开。
        boolean duringFumigation = fumigations.existsBySealedGranaryId(granary.id);
        TempRecord saved = new TempRecord();
        saved.granaryId = granary.id;
        saved.recordDate = input.recordDate;
        saved.temperature = input.temperature;
        saved.humidity = input.humidity;
        saved.recorder = input.recorder;
        saved.result = input.temperature >= WARN_TEMPERATURE ? "超温" : "正常";
        saved.duringFumigation = duringFumigation;
        return records.save(saved);
    }
}
