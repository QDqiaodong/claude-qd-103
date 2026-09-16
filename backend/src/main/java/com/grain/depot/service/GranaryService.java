package com.grain.depot.service;

import com.grain.depot.dto.BizException;
import com.grain.depot.entity.GrainBatch;
import com.grain.depot.entity.Granary;
import com.grain.depot.repository.GrainBatchRepository;
import com.grain.depot.repository.GranaryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GranaryService {

    private final GranaryRepository granaries;
    private final GrainBatchRepository batches;

    public GranaryService(GranaryRepository granaries, GrainBatchRepository batches) {
        this.granaries = granaries;
        this.batches = batches;
    }

    /** 这个仓房现在实际存了多少吨。 */
    public int stored(Long granaryId) {
        return batches.findByGranaryIdAndStatus(granaryId, "在储").stream()
                .mapToInt(b -> b.quantity)
                .sum();
    }

    public List<Granary> list(String status, String keyword) {
        return granaries.findAllByOrderByIdAsc().stream()
                .filter(g -> status == null || status.isEmpty() || status.equals(g.status))
                .filter(g -> keyword == null || keyword.isEmpty()
                        || g.name.contains(keyword) || g.code.contains(keyword))
                .toList();
    }

    @Transactional
    public Granary create(Granary input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("仓房编号不能为空");
        }
        if (granaries.existsByCode(input.code)) {
            throw new BizException("编号 " + input.code + " 已经被别的仓房用掉了");
        }
        if (input.capacity == null || input.capacity <= 0) {
            throw new BizException("仓容要大于 0 吨");
        }
        Granary saved = new Granary();
        saved.code = input.code.trim();
        saved.name = input.name;
        saved.capacity = input.capacity;
        saved.status = (input.status == null || input.status.isBlank()) ? "空仓" : input.status;
        return granaries.save(saved);
    }

    @Transactional
    public Granary update(Long id, Granary input) {
        Granary g = granaries.findById(id).orElseThrow(() -> new BizException("仓房不存在"));
        int current = stored(g.id);
        if (input.name != null) {
            g.name = input.name;
        }
        if (input.capacity != null && !input.capacity.equals(g.capacity)) {
            if (input.capacity > 0 && input.capacity < current) {
                throw new BizException("这个仓现在存着 " + current + " 吨，仓容不能改到比它小");
            }
            if (input.capacity <= 0) {
                throw new BizException("仓容要大于 0 吨");
            }
            g.capacity = input.capacity;
        }
        if (input.status != null && !input.status.isBlank() && !input.status.equals(g.status)) {
            if ("维修".equals(input.status) && current > 0) {
                throw new BizException("这个仓还存着 " + current + " 吨粮，先出空才能进维修");
            }
            g.status = input.status;
        }
        return granaries.save(g);
    }
}
