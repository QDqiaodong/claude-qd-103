package com.grain.depot.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/** 熏蒸作业：对一间在储仓投药密闭，散气复检合格后才算结束。 */
@Entity
@Table(name = "fumigation")
public class Fumigation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 32, unique = true)
    public String code;

    @Column(name = "granary_id", nullable = false)
    public Long granaryId;

    /** 药剂名称，如 磷化铝 */
    @Column(name = "chemical_name", nullable = false, length = 64)
    public String chemicalName;

    /** 投药量（公斤） */
    @Column(nullable = false)
    public Double dosage;

    /** 计划密闭起始日 */
    @Column(name = "seal_date", nullable = false)
    public LocalDate sealDate;

    /** 计划散气日 */
    @Column(name = "aeration_date", nullable = false)
    public LocalDate aerationDate;

    /** 投药人 */
    @Column(name = "applicator", nullable = false, length = 32)
    public String applicator;

    /** 待密闭 / 密闭中 / 已完成（复检合格放行） */
    @Column(nullable = false, length = 16)
    public String status;

    /** 复检残气检测人 */
    @Column(name = "checker", length = 32)
    public String checker;

    /** 复检结论：合格 / 不合格 */
    @Column(name = "check_result", length = 16)
    public String checkResult;

    /** 复检日期 */
    @Column(name = "check_date")
    public LocalDate checkDate;
}
