package com.grain.depot.service;

import com.grain.depot.dto.BizException;
import com.grain.depot.entity.Fumigation;
import com.grain.depot.entity.Granary;
import com.grain.depot.repository.FumigationRepository;
import com.grain.depot.repository.GranaryRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FumigationService {

    private final FumigationRepository fumigations;
    private final GranaryRepository granaries;
    private final GranaryService granaryService;

    public FumigationService(FumigationRepository fumigations, GranaryRepository granaries,
                             GranaryService granaryService) {
        this.fumigations = fumigations;
        this.granaries = granaries;
        this.granaryService = granaryService;
    }

    public List<Fumigation> list(Long granaryId, String status) {
        return fumigations.findAllByOrderByIdDesc().stream()
                .filter(f -> granaryId == null || granaryId.equals(f.granaryId))
                .filter(f -> status == null || status.isEmpty() || status.equals(f.status))
                .toList();
    }

    /** 这间仓现在是否处于熏蒸密闭中（散气复检没合格之前都算）。 */
    public boolean isSealed(Long granaryId) {
        return granaryId != null
                && !fumigations.findByGranaryIdAndStatus(granaryId, "密闭中").isEmpty();
    }

    @Transactional
    public Fumigation create(Fumigation input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("熏蒸单号不能为空");
        }
        if (fumigations.existsByCode(input.code)) {
            throw new BizException("熏蒸单号 " + input.code + " 已经用过了");
        }
        if (input.granaryId == null) {
            throw new BizException("请选择要熏蒸的仓房");
        }
        Granary granary = granaries.findById(input.granaryId)
                .orElseThrow(() -> new BizException("仓房不存在"));
        if ("维修".equals(granary.status)) {
            throw new BizException("仓房 " + granary.name + " 正在维修，不能熏蒸");
        }
        if (granaryService.stored(granary.id) <= 0) {
            throw new BizException("仓房 " + granary.name + " 现在没有在储粮，不用熏蒸");
        }
        if (isSealed(granary.id)) {
            throw new BizException("仓房 " + granary.name + " 已经在密闭，不能再开熏蒸单");
        }
        if (input.chemicalName == null || input.chemicalName.isBlank()) {
            throw new BizException("请填药剂名称");
        }
        if (input.dosage == null || input.dosage <= 0) {
            throw new BizException("投药量要大于 0");
        }
        if (input.sealDate == null) {
            throw new BizException("请填计划密闭起始日");
        }
        if (input.aerationDate == null) {
            throw new BizException("请填计划散气日");
        }
        if (input.aerationDate.isBefore(input.sealDate)) {
            throw new BizException("计划散气日不能早于计划密闭起始日");
        }
        if (input.applicator == null || input.applicator.isBlank()) {
            throw new BizException("请填投药人");
        }

        Fumigation saved = new Fumigation();
        saved.code = input.code.trim();
        saved.granaryId = granary.id;
        saved.chemicalName = input.chemicalName.trim();
        saved.dosage = input.dosage;
        saved.sealDate = input.sealDate;
        saved.aerationDate = input.aerationDate;
        saved.applicator = input.applicator.trim();
        saved.status = "待密闭";
        return fumigations.save(saved);
    }

    /** 开始密闭：密闭期间一切作业全停。 */
    @Transactional
    public Fumigation start(Long id) {
        Fumigation f = fumigations.findById(id).orElseThrow(() -> new BizException("熏蒸单不存在"));
        // 先锁仓房行，同一间仓两张单并发开始时只放一个进来。
        Granary granary = granaries.findByIdForUpdate(f.granaryId)
                .orElseThrow(() -> new BizException("仓房不存在"));
        if ("密闭中".equals(f.status)) {
            throw new BizException("仓房 " + granary.name + " 已经在密闭，这张单已经开始过了");
        }
        if ("已完成".equals(f.status)) {
            throw new BizException("这张熏蒸单已经复检放行了");
        }
        if (!fumigations.findActiveForUpdate(granary.id).isEmpty()) {
            throw new BizException("仓房 " + granary.name + " 已经在密闭，不能再开始第二张");
        }
        f.status = "密闭中";
        return fumigations.save(f);
    }

    /** 复检放行：到计划散气日才能复检；不合格继续停作业，合格才恢复。 */
    @Transactional
    public Fumigation release(Long id, Fumigation input) {
        Fumigation f = fumigations.findByIdForUpdate(id)
                .orElseThrow(() -> new BizException("熏蒸单不存在"));
        if ("已完成".equals(f.status)) {
            throw new BizException("这张熏蒸单已经复检放行了，只能放行一次");
        }
        if (!"密闭中".equals(f.status)) {
            throw new BizException("还没开始密闭，不能复检");
        }
        LocalDate today = LocalDate.now();
        if (today.isBefore(f.aerationDate)) {
            throw new BizException("计划散气日是 " + f.aerationDate + "，日子没到，复检放行点不了");
        }
        if (input.checker == null || input.checker.isBlank()) {
            throw new BizException("请填残气检测人");
        }
        if (!"合格".equals(input.checkResult) && !"不合格".equals(input.checkResult)) {
            throw new BizException("请选复检结论（合格 / 不合格）");
        }
        f.checker = input.checker.trim();
        f.checkResult = input.checkResult;
        f.checkDate = input.checkDate != null ? input.checkDate : today;
        // 不合格：单子仍停在密闭中，作业继续全停，改日再来复检。
        if ("合格".equals(input.checkResult)) {
            f.status = "已完成";
        }
        return fumigations.save(f);
    }
}
