# 粮库 · 仓房与粮情管理系统

粮库日常保管台账：**仓房台账**、**粮食批次**、**粮情测温**、**熏蒸作业**、**出入库作业**。

## 技术栈

Spring Boot 3.3（Java 17）+ MySQL 8.0 + Redis 7 + Vue 3 + Element Plus + Vite + nginx，全栈 `docker compose` 一键启动。

## 启动

```bash
./start.sh              # 等价于 docker compose up -d --build
```

| 入口 | 地址 |
| --- | --- |
| 前端页面 | http://127.0.0.1:8213/ |
| 后端接口 | http://127.0.0.1:8313/api/ |
| MySQL | 127.0.0.1:3513（库 `grain_depot`） |
| Redis | 127.0.0.1:6513 |

## 停止

```bash
docker compose down       # 保留数据卷
docker compose down -v    # 连数据卷一起删，下次启动重新灌种子数据
```

## 业务模块

### 1. 仓房台账（`granary`）

仓房编号 `G-xx` 全库唯一，每间仓房记「仓容」（吨），状态为 `空仓 / 在储 / 维修`。
列表同时显示这间仓当前实际在储多少吨（按在储批次汇总）。
**仓容不能改到比当前在储量小**；**仓里还有粮时不允许转维修**。

- 页面：仓房台账（`/granaries`）
- 接口：`GET /api/granaries`、`POST /api/granaries`、`PUT /api/granaries/{id}`

### 2. 粮食批次（`grain_batch`）

批次号 `GB-xxxx-xx` 全库唯一，记品种（`小麦 / 玉米 / 稻谷 / 大豆`）、在库吨数、入库日期与水分。
写入时校验：**入库后该仓在储总量不能超过仓容**；维修中的仓不能入粮；水分要在 0～30 之间。
出库时要先把在库数出空，才能把批次标成 `已出库`。

- 页面：粮食批次（`/batches`）
- 接口：`GET /api/batches`、`POST /api/batches`、`PUT /api/batches/{id}`

### 3. 粮情测温（`temp_record`）

每间仓房每天量一次粮温与仓内湿度，记记录人。
**同一间仓房同一天只能登记一条**；**粮温到 26℃ 及以上自动记为「超温」**（这个结论由服务端判，不用手填）。
维修中的仓房不用测温；熏蒸密闭期间可以测温，服务端自动标成「密闭期测温」，当天仍只准一条。

- 页面：粮情测温（`/temps`）
- 接口：`GET /api/temps`、`POST /api/temps`

### 4. 熏蒸作业（`fumigation`）

虫情来了给在储仓投药密闭，状态机：`待密闭 → 密闭中 → 已放行`。
开单一次写齐：**哪间在储仓、药剂名称、投药量、计划密闭起始日、计划散气日、投药人**（单号自动生成，可改）。

密闭管控按安监处的规矩落地：

- **同一间仓不能同时挂两张「密闭中」的单**：开始密闭时先锁仓房行再查占用，
  `sealed_granary_id` 列上还有唯一约束兜底——柜台两边各点一次开始，只有一笔成功，
  失败的提示「这间仓已经在密闭」，列表也不会冒出第二张。
- 密闭开始后，这间仓上**待执行的出入库单不许再执行，新开出入库也挡住**；
  **往这间仓再建在储批次挡住**；**仓房不能改成维修**。
- **测温仍允许登记**，服务端按该仓是否密闭自动盖上「密闭期测温」的戳，
  同仓同一天仍然只能一条，密闭期测温混不进第二条日常测温。
- **计划散气日没到，复检放行点不了**（前端置灰，后端再核日期）。
  到了之后填**残气检测人和结论**：不合格单子仍停在密闭中、作业继续停，可再来复检；
  合格才解除密闭，这间仓重新允许出入库和转维修。
- 复检放行两边一起点：实体上有 `@Version` 乐观锁，**只能成功一次**，
  失败的那笔收到「已经复检放行过了」。

- 页面：熏蒸作业（`/fumigations`）
- 接口：`GET /api/fumigations`、`POST /api/fumigations`、`POST /api/fumigations/{id}/start`、`POST /api/fumigations/{id}/release`

### 5. 出入库作业（`stock_move`）

作业单号 `SM-xxxx` 全库唯一，类型分 `入库 / 出库`，状态 `待执行 → 已完成`。
开单时先校验：**出库吨数不能超过该批次当前在库量**。
**真正加减在库数发生在「执行」那一刻**——执行时再核一遍：
出库不能把在库扣成负数（扣到 0 自动把批次置为 `已出库`）；
入库不能让该仓突破仓容。同一张单子只能执行一次。

- 页面：出入库作业（`/moves`）
- 接口：`GET /api/moves`、`POST /api/moves`、`POST /api/moves/{id}/execute`

## 目录

```
backend/src/main/java/com/grain/depot/
├── config/       CORS 配置
├── controller/   REST 入口
├── dto/          BizException + 统一错误响应
├── entity/       5 张业务表
├── repository/   Spring Data JPA
└── service/      业务规则（容量校验、唯一性、状态机、库存加减）
backend/src/main/resources/schema.sql   建表 + 种子数据（挂进 MySQL initdb）
frontend/src/views/                     4 个业务页面
```
