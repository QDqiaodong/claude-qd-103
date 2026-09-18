package com.grain.depot.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/** 粮情测温：仓房里每天量一次温湿度。 */
@Entity
@Table(name = "temp_record")
public class TempRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "granary_id", nullable = false)
    public Long granaryId;

    @Column(name = "record_date", nullable = false)
    public LocalDate recordDate;

    /** 粮温，摄氏度 */
    @Column(nullable = false)
    public Double temperature;

    /** 仓内湿度百分比 */
    @Column(nullable = false)
    public Double humidity;

    @Column(nullable = false, length = 32)
    public String recorder;

    /** 正常 / 超温 */
    @Column(nullable = false, length = 16)
    public String result;

    /** 这条测温是不是熏蒸密闭期间登的（由服务端判定，不用手填）。 */
    @Column(name = "during_fumigation", nullable = false)
    public Boolean duringFumigation;
}
