package com.grain.depot.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/** 出入库作业：一批粮进仓或者出仓。 */
@Entity
@Table(name = "stock_move")
public class StockMove {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 32, unique = true)
    public String code;

    @Column(name = "batch_id", nullable = false)
    public Long batchId;

    /** 入库 / 出库 */
    @Column(name = "move_type", nullable = false, length = 16)
    public String moveType;

    @Column(nullable = false)
    public Integer quantity;

    @Column(name = "move_date", nullable = false)
    public LocalDate moveDate;

    @Column(nullable = false, length = 32)
    public String operator;

    /** 待执行 / 已完成 */
    @Column(nullable = false, length = 16)
    public String status;
}
