package com.grain.depot.service;

import com.grain.depot.dto.BizException;
import com.grain.depot.entity.GrainBatch;
import com.grain.depot.entity.Granary;
import com.grain.depot.entity.StockMove;
import com.grain.depot.repository.FumigationRepository;
import com.grain.depot.repository.GrainBatchRepository;
import com.grain.depot.repository.GranaryRepository;
import com.grain.depot.repository.StockMoveRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockMoveService {

    private final StockMoveRepository moves;
    private final GrainBatchRepository batches;
    private final GranaryRepository granaries;
    private final FumigationRepository fumigations;

    public StockMoveService(StockMoveRepository moves, GrainBatchRepository batches,
                            GranaryRepository granaries, FumigationRepository fumigations) {
        this.moves = moves;
        this.batches = batches;
        this.granaries = granaries;
        this.fumigations = fumigations;
    }

    public List<StockMove> list(Long batchId, String moveType, String status) {
        return moves.findAllByOrderByIdDesc().stream()
                .filter(m -> batchId == null || batchId.equals(m.batchId))
                .filter(m -> moveType == null || moveType.isEmpty() || moveType.equals(m.moveType))
                .filter(m -> status == null || status.isEmpty() || status.equals(m.status))
                .toList();
    }

    @Transactional
    public StockMove create(StockMove input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("作业单号不能为空");
        }
        if (moves.existsByCode(input.code)) {
            throw new BizException("作业单号 " + input.code + " 已经用过了");
        }
        if (input.batchId == null) {
            throw new BizException("请选择批次");
        }
        if (input.quantity == null || input.quantity <= 0) {
            throw new BizException("作业吨数要大于 0");
        }
        if (!"入库".equals(input.moveType) && !"出库".equals(input.moveType)) {
            throw new BizException("作业类型只能是入库或者出库");
        }
        if (input.moveDate == null) {
            throw new BizException("请填作业日期");
        }
        GrainBatch batch = batches.findById(input.batchId)
                .orElseThrow(() -> new BizException("批次不存在"));
        assertNotSealed(batch.granaryId);
        if ("出库".equals(input.moveType) && input.quantity > batch.quantity) {
            throw new BizException("这批粮库里只有 " + batch.quantity + " 吨，出不了 "
                    + input.quantity + " 吨");
        }
        StockMove saved = new StockMove();
        saved.code = input.code.trim();
        saved.batchId = batch.id;
        saved.moveType = input.moveType;
        saved.quantity = input.quantity;
        saved.moveDate = input.moveDate;
        saved.operator = input.operator;
        saved.status = "待执行";
        return moves.save(saved);
    }

    @Transactional
    public StockMove execute(Long id) {
        StockMove move = moves.findById(id).orElseThrow(() -> new BizException("作业单不存在"));
        if (!"待执行".equals(move.status)) {
            throw new BizException("这张作业单已经执行过了");
        }
        GrainBatch batch = batches.findById(move.batchId)
                .orElseThrow(() -> new BizException("批次不存在"));
        assertNotSealed(batch.granaryId);

        if ("出库".equals(move.moveType)) {
            if (move.quantity > batch.quantity) {
                throw new BizException("这批粮现在只剩 " + batch.quantity + " 吨，出不了 "
                        + move.quantity + " 吨");
            }
            batch.quantity = batch.quantity - move.quantity;
            if (batch.quantity == 0) {
                batch.status = "已出库";
            }
        } else {
            Granary granary = granaries.findById(batch.granaryId)
                    .orElseThrow(() -> new BizException("仓房不存在"));
            int stored = batches.findByGranaryIdAndStatus(granary.id, "在储").stream()
                    .mapToInt(b -> b.quantity)
                    .sum();
            if (stored + move.quantity > granary.capacity) {
                throw new BizException("仓房 " + granary.name + " 还剩 "
                        + (granary.capacity - stored) + " 吨位置，装不下这 " + move.quantity + " 吨");
            }
            batch.quantity = batch.quantity + move.quantity;
        }
        batches.save(batch);
        move.status = "已完成";
        return moves.save(move);
    }

    /** 密闭期间作业全停：出入库一律不许动，紧急出库也不行（安监处规矩）。 */
    private void assertNotSealed(Long granaryId) {
        if (!fumigations.findByGranaryIdAndStatus(granaryId, "密闭中").isEmpty()) {
            Granary granary = granaries.findById(granaryId).orElse(null);
            String name = granary == null ? String.valueOf(granaryId) : granary.name;
            throw new BizException("仓房 " + name + " 正在熏蒸密闭中，出入库作业全停，复检合格后才能恢复");
        }
    }
}
