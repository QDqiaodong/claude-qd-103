package com.grain.depot.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * 熏蒸作业：虫情来了给某间在储仓投药密闭，散气后残气复检合格才放行。
 * 状态机：待密闭 → 密闭中 → 已放行（复检不合格则一直停在密闭中，可继续复检）。
 */
@Entity
@Table(name = "fumigation")
public class Fumigation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** 熏蒸单号 FM-xxxx */
    @Column(nullable = false, length = 32, unique = true)
    public String code;

    @Column(name = "granary_id", nullable = false)
    public Long granaryId;

    /** 药剂名称，如 磷化铝 */
    @Column(nullable = false, length = 64)
    public String chemical;

    /** 投药量（千克） */
    @Column(nullable = false)
    public Double dosage;

    /** 计划密闭起始日 */
    @Column(name = "plan_seal_date", nullable = false)
    public LocalDate planSealDate;

    /** 计划散气日 */
    @Column(name = "plan_air_date", nullable = false)
    public LocalDate planAirDate;

    /** 投药人 */
    @Column(nullable = false, length = 32)
    public String operator;

    /** 待密闭 / 密闭中 / 已放行 */
    @Column(nullable = false, length = 16)
    public String status;

    /** 实际开始密闭（投药）日期 */
    @Column(name = "seal_date")
    public LocalDate sealDate;

    /**
     * 密闭中的仓房占用标记：密闭中时等于 granaryId，其余状态为 null。
     * 数据库上对这一列加唯一约束，从根上保证同一间仓不能同时挂两张密闭中的单。
     */
    @Column(name = "sealed_granary_id", unique = true)
    public Long sealedGranaryId;

    /** 最近一次残气复检人 */
    @Column(name = "release_inspector", length = 32)
    public String releaseInspector;

    /** 最近一次复检结论：合格 / 不合格 */
    @Column(name = "release_result", length = 16)
    public String releaseResult;

    /** 最近一次复检日期 */
    @Column(name = "release_date")
    public LocalDate releaseDate;

    /** 复检备注 */
    @Column(name = "release_remark", length = 255)
    public String releaseRemark;

    /** 乐观锁：复检放行两边一起点，只能有一笔成功。 */
    @Version
    @Column(nullable = false)
    public Long version;
}
