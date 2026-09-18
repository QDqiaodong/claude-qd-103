package com.grain.depot;

import static org.junit.jupiter.api.Assertions.*;

import com.grain.depot.dto.BizException;
import com.grain.depot.entity.*;
import com.grain.depot.repository.*;
import com.grain.depot.service.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class FumigationFlowTests {

    @Autowired FumigationService fumigationService;
    @Autowired FumigationRepository fumigations;
    @Autowired GranaryService granaryService;
    @Autowired GranaryRepository granaries;
    @Autowired GrainBatchService batchService;
    @Autowired StockMoveService moveService;
    @Autowired StockMoveRepository moves;
    @Autowired TempService tempService;
    @Autowired GrainBatchRepository batches;
    @Autowired TempRecordRepository temps;
    @Autowired javax.sql.DataSource dataSource;

    LocalDate today = LocalDate.now(FumigationService.ZONE);

    /** 五个测试共用一个 Spring 上下文，每条之前把数据清回种子状态。 */
    @BeforeEach
    void reset() {
        org.springframework.jdbc.core.JdbcTemplate jdbc =
                new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        for (String t : List.of("fumigation", "stock_move", "temp_record", "grain_batch", "granary")) {
            jdbc.execute("DELETE FROM " + t);
            jdbc.execute("ALTER TABLE " + t + " ALTER COLUMN id RESTART WITH 1");
        }
        jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");

        Granary g1 = new Granary(); g1.code = "G-01"; g1.name = "一号仓"; g1.capacity = 500; g1.status = "在储";
        Granary g2 = new Granary(); g2.code = "G-02"; g2.name = "二号仓"; g2.capacity = 300; g2.status = "在储";
        Granary g3 = new Granary(); g3.code = "G-03"; g3.name = "三号仓"; g3.capacity = 200; g3.status = "空仓";
        Granary g4 = new Granary(); g4.code = "G-04"; g4.name = "四号仓"; g4.capacity = 400; g4.status = "维修";
        granaries.save(g1);
        granaries.save(g2);
        granaries.save(g3);
        granaries.save(g4);

        GrainBatch b1 = new GrainBatch();
        b1.code = "GB-2026-01"; b1.variety = "小麦"; b1.granaryId = 1L; b1.quantity = 300;
        b1.inDate = today.minusDays(8); b1.moisture = 12.5; b1.status = "在储";
        GrainBatch b2 = new GrainBatch();
        b2.code = "GB-2026-02"; b2.variety = "玉米"; b2.granaryId = 1L; b2.quantity = 150;
        b2.inDate = today.minusDays(6); b2.moisture = 14.0; b2.status = "在储";
        GrainBatch b3 = new GrainBatch();
        b3.code = "GB-2026-03"; b3.variety = "稻谷"; b3.granaryId = 2L; b3.quantity = 280;
        b3.inDate = today.minusDays(10); b3.moisture = 13.8; b3.status = "在储";
        batches.save(b1);
        batches.save(b2);
        batches.save(b3);

        StockMove waiting = new StockMove();
        waiting.code = "SM-0002"; waiting.batchId = 1L; waiting.moveType = "入库";
        waiting.quantity = 50; waiting.moveDate = today.minusDays(2); waiting.operator = "李保管";
        waiting.status = "待执行";
        moves.save(waiting);
    }

    Fumigation draft(Long granaryId, String code, LocalDate planAir) {
        Fumigation f = new Fumigation();
        f.code = code;
        f.granaryId = granaryId;
        f.chemical = "磷化铝";
        f.dosage = 12.0;
        f.planSealDate = today;
        f.planAirDate = planAir;
        f.operator = "李保管";
        return f;
    }

    @Test
    void 开单必须写齐六个要素() {
        Fumigation f = draft(1L, "FM-T001", today.plusDays(7));
        Fumigation saved = fumigationService.create(f);
        assertNotNull(saved.id);
        assertEquals("待密闭", saved.status);
        assertEquals("磷化铝", saved.chemical);
        assertEquals(12.0, saved.dosage);
        assertEquals(today, saved.planSealDate);
        assertEquals(today.plusDays(7), saved.planAirDate);
        assertEquals("李保管", saved.operator);
        assertEquals(1L, saved.granaryId);
        assertNull(saved.sealedGranaryId);

        Fumigation noChemical = draft(2L, "FM-T002", today.plusDays(5));
        noChemical.chemical = "  ";
        assertEquals("请填药剂名称",
                assertThrows(BizException.class, () -> fumigationService.create(noChemical)).getMessage());

        Fumigation badDosage = draft(2L, "FM-T003", today.plusDays(5));
        badDosage.dosage = 0.0;
        assertThrows(BizException.class, () -> fumigationService.create(badDosage));

        Fumigation noOperator = draft(2L, "FM-T004", today.plusDays(5));
        noOperator.operator = "";
        assertThrows(BizException.class, () -> fumigationService.create(noOperator));

        // 散气日不能早于密闭起始日
        Fumigation badDate = draft(2L, "FM-T005", today.minusDays(1));
        assertEquals("计划散气日不能早于计划密闭起始日",
                assertThrows(BizException.class, () -> fumigationService.create(badDate)).getMessage());

        // 只有在储的仓能安排熏蒸
        Fumigation empty = draft(3L, "FM-T006", today.plusDays(5));
        assertTrue(assertThrows(BizException.class, () -> fumigationService.create(empty))
                .getMessage().contains("只有在储的仓"));
        Fumigation repairing = draft(4L, "FM-T007", today.plusDays(5));
        assertThrows(BizException.class, () -> fumigationService.create(repairing));
    }

    @Test
    void 密闭期间出入库开新批次转维修全停_测温标记密闭期() {
        fumigationService.create(draft(1L, "FM-B001", today.plusDays(7)));
        Fumigation sealed = fumigationService.start(
                fumigations.findByGranaryIdOrderByIdDesc(1L).get(0).id);
        assertEquals("密闭中", sealed.status);
        assertEquals(1L, sealed.sealedGranaryId);
        assertEquals(today, sealed.sealDate);

        // 1) 已经停在待执行的出入库单不许再执行（种子数据 SM-0002 是仓1入库待执行）
        Long waitingMoveId = moves.findAllByOrderByIdDesc().stream()
                .filter(m -> "SM-0002".equals(m.code)).findFirst().orElseThrow().id;
        BizException ex1 = assertThrows(BizException.class, () -> moveService.execute(waitingMoveId));
        assertTrue(ex1.getMessage().contains("正在熏蒸密闭中"));

        // 2) 新开出入库也要挡住（入库、出库都拦）
        StockMove in = new StockMove();
        in.code = "SM-TB01"; in.batchId = 1L; in.moveType = "入库";
        in.quantity = 10; in.moveDate = today; in.operator = "李保管";
        assertTrue(assertThrows(BizException.class, () -> moveService.create(in))
                .getMessage().contains("禁止出入库"));
        StockMove out = new StockMove();
        out.code = "SM-TB02"; out.batchId = 2L; out.moveType = "出库";
        out.quantity = 10; out.moveDate = today; out.operator = "李保管";
        assertThrows(BizException.class, () -> moveService.create(out));

        // 3) 往这间仓再建在储批次挡住
        GrainBatch b = new GrainBatch();
        b.code = "GB-TB01"; b.variety = "小麦"; b.granaryId = 1L; b.quantity = 10;
        b.inDate = today; b.moisture = 12.0;
        assertTrue(assertThrows(BizException.class, () -> batchService.create(b))
                .getMessage().contains("不能再建在储批次"));

        // 4) 仓房不能改成维修
        Granary g = granaries.findById(1L).orElseThrow();
        g.status = "维修";
        assertTrue(assertThrows(BizException.class, () -> granaryService.update(1L, g))
                .getMessage().contains("不能改状态"));
        // 仓房本体状态没被动过
        assertEquals("在储", granaries.findById(1L).orElseThrow().status);

        // 5) 测温允许登记，而且盖密闭期的戳
        TempRecord tr = new TempRecord();
        tr.granaryId = 1L; tr.recordDate = LocalDate.of(2030, 1, 2);
        tr.temperature = 21.0; tr.humidity = 50.0; tr.recorder = "李保管";
        TempRecord savedTr = tempService.create(tr);
        assertTrue(savedTr.duringFumigation);
        // 同仓同一天不能混进第二条
        TempRecord tr2 = new TempRecord();
        tr2.granaryId = 1L; tr2.recordDate = LocalDate.of(2030, 1, 2);
        tr2.temperature = 22.0; tr2.humidity = 51.0; tr2.recorder = "李保管";
        assertTrue(assertThrows(BizException.class, () -> tempService.create(tr2))
                .getMessage().contains("一天只量一次"));

        // 同间仓不能再开始第二张密闭单：直接塞一张待密闭单，只验 start 这道闸
        Fumigation second = draft(1L, "FM-B002", today.plusDays(9));
        second.status = "待密闭";
        second.version = 0L;
        Fumigation secondDraft = fumigations.save(second);
        BizException ex2 = assertThrows(BizException.class,
                () -> fumigationService.start(secondDraft.id));
        assertTrue(ex2.getMessage().contains("已经在密闭中"));
        // 列表里仍然只有一张密闭中
        long sealedCount = fumigations.findByGranaryIdOrderByIdDesc(1L).stream()
                .filter(x -> "密闭中".equals(x.status)).count();
        assertEquals(1, sealedCount);
    }

    @Test
    void 计划散气日没到点不了放行_不合格继续停_合格才恢复() {
        // 仓2：散气日在五天后，提前复检必须被挡
        fumigationService.create(draft(2L, "FM-R001", today.plusDays(5)));
        Long id = fumigations.findByGranaryIdOrderByIdDesc(2L).get(0).id;
        fumigationService.start(id);

        Fumigation early = new Fumigation();
        early.releaseInspector = "赵安监";
        early.releaseResult = "合格";
        BizException ex = assertThrows(BizException.class, () -> fumigationService.release(id, early));
        assertTrue(ex.getMessage().contains("还没到"));
        // 仍然密闭中
        assertEquals("密闭中", fumigations.findById(id).orElseThrow().status);

        // 仓1：今天就是散气日，走复检
        fumigationService.create(draft(1L, "FM-R002", today));
        Long id3 = fumigations.findByGranaryIdOrderByIdDesc(1L).get(0).id;
        fumigationService.start(id3);

        // 缺检测人
        Fumigation noInspector = new Fumigation();
        noInspector.releaseResult = "合格";
        assertEquals("请填残气检测人",
                assertThrows(BizException.class, () -> fumigationService.release(id3, noInspector)).getMessage());

        // 不合格：单子仍停在密闭中，作业继续停
        Fumigation fail = new Fumigation();
        fail.releaseInspector = "赵安监";
        fail.releaseResult = "不合格";
        fail.releaseRemark = "残气浓度偏高";
        Fumigation afterFail = fumigationService.release(id3, fail);
        assertEquals("密闭中", afterFail.status);
        assertEquals("不合格", afterFail.releaseResult);
        assertEquals(1L, afterFail.sealedGranaryId);
        // 出入库仍然停
        StockMove out = new StockMove();
        out.code = "SM-RT01"; out.batchId = 1L; out.moveType = "出库";
        out.quantity = 5; out.moveDate = today; out.operator = "李保管";
        assertThrows(BizException.class, () -> moveService.create(out));

        // 再来一次，合格放行
        Fumigation pass = new Fumigation();
        pass.releaseInspector = "赵安监";
        pass.releaseResult = "合格";
        Fumigation afterPass = fumigationService.release(id3, pass);
        assertEquals("已放行", afterPass.status);
        assertNull(afterPass.sealedGranaryId);
        assertEquals("合格", afterPass.releaseResult);
        assertEquals(today, afterPass.releaseDate);

        // 恢复后：新开出入库能开、能执行
        StockMove out2 = new StockMove();
        out2.code = "SM-RT02"; out2.batchId = 1L; out2.moveType = "出库";
        out2.quantity = 5; out2.moveDate = today; out2.operator = "李保管";
        StockMove created = moveService.create(out2);
        assertEquals("待执行", created.status);
        StockMove executed = moveService.execute(created.id);
        assertEquals("已完成", executed.status);
    }

    @Test
    void 并发开始密闭只有一笔成功() throws Exception {
        fumigationService.create(draft(1L, "FM-C001", today.plusDays(7)));
        fumigationService.create(draft(1L, "FM-C002", today.plusDays(8)));
        List<Long> ids = fumigations.findByGranaryIdOrderByIdDesc(1L).stream()
                .filter(x -> "待密闭".equals(x.status)).map(x -> x.id).toList();
        assertEquals(2, ids.size());

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final Long id = ids.get(i % 2);
            futures.add(pool.submit(() -> {
                latch.await();
                try {
                    fumigationService.start(id);
                    ok.incrementAndGet();
                } catch (BizException ignored) {
                    // 后到的那笔应当失败并写明已经在密闭
                }
                return null;
            }));
        }
        latch.countDown();
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(1, ok.get(), "同一间仓并发开始密闭只能成功一笔");
        long sealedCount = fumigations.findByGranaryIdOrderByIdDesc(1L).stream()
                .filter(x -> "密闭中".equals(x.status)).count();
        assertEquals(1, sealedCount, "刷新列表也不能冒出第二张密闭单");
    }

    @Test
    void 并发复检放行只有一笔成功_失败的提示已放行() throws Exception {
        fumigationService.create(draft(1L, "FM-P001", today));
        Long id = fumigations.findByGranaryIdOrderByIdDesc(1L).get(0).id;
        fumigationService.start(id);

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        List<String> errors = new java.util.concurrent.CopyOnWriteArrayList<>();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                latch.await();
                Fumigation input = new Fumigation();
                input.releaseInspector = "赵安监";
                input.releaseResult = "合格";
                try {
                    fumigationService.release(id, input);
                    ok.incrementAndGet();
                } catch (BizException e) {
                    errors.add(e.getMessage());
                }
                return null;
            }));
        }
        latch.countDown();
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(1, ok.get(), "两边一起点放行只能成功一笔");
        assertTrue(errors.stream().anyMatch(m -> m.contains("已经复检放行过了")),
                "失败的那笔必须说已经放过了，实际：" + errors);
        assertEquals("已放行", fumigations.findById(id).orElseThrow().status);
        assertNull(fumigations.findById(id).orElseThrow().sealedGranaryId);
    }
}
