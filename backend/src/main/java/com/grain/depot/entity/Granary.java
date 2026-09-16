package com.grain.depot.entity;

import jakarta.persistence.*;

/** 仓房：一个能存粮的屋子，有容量上限。 */
@Entity
@Table(name = "granary")
public class Granary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 32, unique = true)
    public String code;

    @Column(nullable = false, length = 64)
    public String name;

    /** 能存多少吨 */
    @Column(nullable = false)
    public Integer capacity;

    /** 空仓 / 在储 / 维修 */
    @Column(nullable = false, length = 16)
    public String status;
}
