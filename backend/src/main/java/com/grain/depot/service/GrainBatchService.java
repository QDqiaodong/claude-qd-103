package com.grain.depot.service;

import com.grain.depot.dto.BizException;
import com.grain.depot.entity.GrainBatch;
import com.grain.depot.entity.Granary;
import com.grain.depot.repository.FumigationRepository;
import com.grain.depot.repository.GrainBatchRepository;
import com.grain.depot.repository.GranaryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GrainBatchService {

    private final GrainBatchRepository batches;
    private final GranaryRepository granaries;
    private final FumigationRepository fumigations;

    public GrainBatchService(GrainBatchRepository batches, GranaryRepository granaries,
                             FumigationRepository fumigations) {
        this.batches = batches;
        this.granaries = granaries;
        this.fumigations = fumigations;
    }

    public List<GrainBatch> list(Long granaryId, String status, String variety) {
        return batches.findAllByOrderByIdAsc().stream()
                .filter(b -> granaryId == null || granaryId.equals(b.granaryId))
                .filter(b -> status == null || status.isEmpty() || status.equals(b.status))
                .filter(b -> variety == null || variety.isEmpty() || variety.equals(b.variety))
                .toList();
    }

    @Transactional
    public GrainBatch create(GrainBatch input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("批次号不能为空");
        }
        if (batches.existsByCode(input.code)) {
            throw new BizException("批次号 " + input.code + " 已经用过了");
        }
        if (input.granaryId == null) {
            throw new BizException("请选择入哪个仓");
        }
        if (input.quantity == null || input.quantity <= 0) {
            throw new BizException("入库吨数要大于 0");
        }
        if (input.moisture != null && (input.moisture < 0 || input.moisture > 30)) {
            throw new BizException("水分要在 0 到 30 之间");
        }
        Granary granary = granaries.findById(input.granaryId)
                .orElseThrow(() -> new BizException("仓房不存在"));
        if ("维修".equals(granary.status)) {
            throw new BizException("仓房 " + granary.name + " 正在维修，不能入粮");
        }
        if (fumigations.existsBySealedGranaryId(granary.id)) {
            throw new BizException("仓房 " + granary.name + " 正在熏蒸密闭中，散气复检合格前不能再建在储批次");
        }
        int stored = batches.findByGranaryIdAndStatus(granary.id, "在储").stream()
                .mapToInt(b -> b.quantity)
                .sum();
        if (stored + input.quantity > granary.capacity) {
            throw new BizException("仓房 " + granary.name + " 只能存 " + granary.capacity
                    + " 吨，现在已经存了 " + stored + " 吨，装不下这 " + input.quantity + " 吨");
        }
        GrainBatch saved = new GrainBatch();
        saved.code = input.code.trim();
        saved.variety = (input.variety == null || input.variety.isBlank()) ? "小麦" : input.variety;
        saved.granaryId = granary.id;
        saved.quantity = input.quantity;
        saved.inDate = input.inDate;
        saved.moisture = input.moisture == null ? 12.0 : input.moisture;
        saved.status = "在储";
        if (granary.status == null || "空仓".equals(granary.status)) {
            granary.status = "在储";
            granaries.save(granary);
        }
        return batches.save(saved);
    }

    @Transactional
    public GrainBatch update(Long id, GrainBatch input) {
        GrainBatch b = batches.findById(id).orElseThrow(() -> new BizException("批次不存在"));
        if ("已出库".equals(b.status)) {
            throw new BizException("这批粮已经出库了，改不了");
        }
        if (input.variety != null && !input.variety.isBlank()) {
            b.variety = input.variety;
        }
        if (input.moisture != null && !input.moisture.equals(b.moisture)) {
            if (input.moisture < 0 || input.moisture > 30) {
                throw new BizException("水分要在 0 到 30 之间");
            }
            b.moisture = input.moisture;
        }
        if (input.status != null && !input.status.isBlank() && !input.status.equals(b.status)) {
            if ("已出库".equals(input.status) && b.quantity > 0) {
                throw new BizException("这批粮还有 " + b.quantity + " 吨在库里，出空了才能标已出库");
            }
            b.status = input.status;
        }
        return batches.save(b);
    }
}
