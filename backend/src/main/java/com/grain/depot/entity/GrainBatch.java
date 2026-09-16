package com.grain.depot.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/** 粮食批次：一次收进来的粮，存在某个仓房里。 */
@Entity
@Table(name = "grain_batch")
public class GrainBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 32, unique = true)
    public String code;

    /** 小麦 / 玉米 / 稻谷 / 大豆 */
    @Column(nullable = false, length = 16)
    public String variety;

    @Column(name = "granary_id", nullable = false)
    public Long granaryId;

    /** 在库吨数 */
    @Column(nullable = false)
    public Integer quantity;

    @Column(name = "in_date", nullable = false)
    public LocalDate inDate;

    /** 水分百分比 */
    @Column(nullable = false)
    public Double moisture;

    /** 在储 / 已出库 */
    @Column(nullable = false, length = 16)
    public String status;
}
