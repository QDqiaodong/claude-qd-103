package com.grain.depot.service;

import com.grain.depot.dto.BizException;
import com.grain.depot.entity.Fumigation;
import com.grain.depot.entity.Granary;
import com.grain.depot.repository.FumigationRepository;
import com.grain.depot.repository.GranaryRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FumigationService {

    /** 库里的「今天」按上海时区算。 */
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final FumigationRepository fumigations;
    private final GranaryRepository granaries;

    public FumigationService(FumigationRepository fumigations, GranaryRepository granaries) {
        this.fumigations = fumigations;
        this.granaries = granaries;
    }

    static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public List<Fumigation> list(Long granaryId, String status) {
        List<Fumigation> all = granaryId == null
                ? fumigations.findAllByOrderByIdDesc()
                : fumigations.findByGranaryIdOrderByIdDesc(granaryId);
        return all.stream()
                .filter(f -> status == null || status.isEmpty() || status.equals(f.status))
                .toList();
    }

    /** 开单：把在储仓、药剂、投药量、密闭/散气计划和投药人一次写齐，先落在「待密闭」。 */
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
        if (!"在储".equals(granary.status)) {
            throw new BizException("仓房 " + granary.name + " 现在是「" + granary.status
                    + "」状态，只有在储的仓才能安排熏蒸");
        }
        if (fumigations.existsBySealedGranaryId(granary.id)) {
            throw new BizException("仓房 " + granary.name + " 已经在密闭中，不能再开熏蒸单");
        }
        if (input.chemical == null || input.chemical.isBlank()) {
            throw new BizException("请填药剂名称");
        }
        if (input.dosage == null || input.dosage <= 0) {
            throw new BizException("投药量要大于 0");
        }
        if (input.planSealDate == null) {
            throw new BizException("请填计划密闭起始日");
        }
        if (input.planAirDate == null) {
            throw new BizException("请填计划散气日");
        }
        if (input.planAirDate.isBefore(input.planSealDate)) {
            throw new BizException("计划散气日不能早于计划密闭起始日");
        }
        if (input.operator == null || input.operator.isBlank()) {
            throw new BizException("请填投药人");
        }

        Fumigation saved = new Fumigation();
        saved.code = input.code.trim();
        saved.granaryId = granary.id;
        saved.chemical = input.chemical.trim();
        saved.dosage = input.dosage;
        saved.planSealDate = input.planSealDate;
        saved.planAirDate = input.planAirDate;
        saved.operator = input.operator.trim();
        saved.status = "待密闭";
        return fumigations.save(saved);
    }

    /**
     * 开始密闭：密闭期间这间仓出入库、开批次、转维修全停。
     * 柜台两边各点一次开始：先锁仓房行再查密闭占用，后到的那笔直接失败。
     */
    @Transactional
    public Fumigation start(Long id) {
        Fumigation f = fumigations.findById(id)
                .orElseThrow(() -> new BizException("熏蒸单不存在"));
        if (!"待密闭".equals(f.status)) {
            if ("密闭中".equals(f.status)) {
                throw new BizException("这张熏蒸单已经在密闭中了");
            }
            throw new BizException("这张熏蒸单已经复检放行，不能再开始密闭");
        }
        Granary granary = granaries.findByIdForUpdate(f.granaryId)
                .orElseThrow(() -> new BizException("仓房不存在"));
        if (fumigations.existsBySealedGranaryId(granary.id)) {
            throw new BizException("仓房 " + granary.name + " 已经在密闭中，这张熏蒸单不能再开始密闭");
        }
        f.status = "密闭中";
        f.sealedGranaryId = granary.id;
        if (f.sealDate == null) {
            f.sealDate = today();
        }
        try {
            return fumigations.saveAndFlush(f);
        } catch (DataIntegrityViolationException dup) {
            // sealed_granary_id 唯一约束兜底：并发下挤进来的第二笔。
            throw new BizException("仓房 " + granary.name + " 已经在密闭中，这张熏蒸单不能再开始密闭");
        }
    }

    /**
     * 散气后残气复检。计划散气日没到不能放行；
     * 不合格继续停作业（单子仍停在密闭中，可再复检）；合格才解除密闭。
     * 两边一起点放行：@Version 乐观锁保证只有一笔成功。
     */
    @Transactional
    public Fumigation release(Long id, Fumigation input) {
        Fumigation f = fumigations.findById(id)
                .orElseThrow(() -> new BizException("熏蒸单不存在"));
        if (!"密闭中".equals(f.status)) {
            if ("已放行".equals(f.status)) {
                throw new BizException("这张熏蒸单已经复检放行过了");
            }
            throw new BizException("这张熏蒸单还没开始密闭，谈不上复检放行");
        }
        LocalDate now = today();
        if (now.isBefore(f.planAirDate)) {
            throw new BizException("计划散气日 " + f.planAirDate + " 还没到，散气前不能复检放行");
        }
        if (input.releaseInspector == null || input.releaseInspector.isBlank()) {
            throw new BizException("请填残气检测人");
        }
        if (!"合格".equals(input.releaseResult) && !"不合格".equals(input.releaseResult)) {
            throw new BizException("复检结论只能填合格或者不合格");
        }

        f.releaseInspector = input.releaseInspector.trim();
        f.releaseResult = input.releaseResult;
        f.releaseDate = now;
        f.releaseRemark = input.releaseRemark;

        if ("不合格".equals(input.releaseResult)) {
            // 残气还没散干净：密闭不解，作业继续停，等散够了再来复检。
            return fumigations.save(f);
        }

        f.status = "已放行";
        f.sealedGranaryId = null;
        try {
            return fumigations.saveAndFlush(f);
        } catch (ObjectOptimisticLockingFailureException locked) {
            throw new BizException("这间仓已经复检放行过了，不用再点一次");
        }
    }
}
