DROP TABLE IF EXISTS fumigation;
DROP TABLE IF EXISTS stock_move;
DROP TABLE IF EXISTS temp_record;
DROP TABLE IF EXISTS grain_batch;
DROP TABLE IF EXISTS granary;

CREATE TABLE granary (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  capacity INT NOT NULL,
  status VARCHAR(16) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_granary_code (code)
);

CREATE TABLE grain_batch (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  variety VARCHAR(16) NOT NULL,
  granary_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  in_date DATE NOT NULL,
  moisture DOUBLE NOT NULL,
  status VARCHAR(16) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_batch_code (code)
);

CREATE TABLE temp_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  granary_id BIGINT NOT NULL,
  record_date DATE NOT NULL,
  temperature DOUBLE NOT NULL,
  humidity DOUBLE NOT NULL,
  recorder VARCHAR(32) NOT NULL,
  result VARCHAR(16) NOT NULL,
  during_fumigation TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_temp_granary_date (granary_id, record_date)
);

CREATE TABLE stock_move (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  batch_id BIGINT NOT NULL,
  move_type VARCHAR(16) NOT NULL,
  quantity INT NOT NULL,
  move_date DATE NOT NULL,
  operator VARCHAR(32) NOT NULL,
  status VARCHAR(16) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_move_code (code)
);

CREATE TABLE fumigation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  granary_id BIGINT NOT NULL,
  chemical VARCHAR(64) NOT NULL,
  dosage DOUBLE NOT NULL,
  plan_seal_date DATE NOT NULL,
  plan_air_date DATE NOT NULL,
  operator VARCHAR(32) NOT NULL,
  status VARCHAR(16) NOT NULL,
  seal_date DATE DEFAULT NULL,
  sealed_granary_id BIGINT DEFAULT NULL,
  release_inspector VARCHAR(32) DEFAULT NULL,
  release_result VARCHAR(16) DEFAULT NULL,
  release_date DATE DEFAULT NULL,
  release_remark VARCHAR(255) DEFAULT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_fumigation_code (code),
  UNIQUE KEY uk_fumigation_sealed_granary (sealed_granary_id)
);

INSERT INTO granary (code, name, capacity, status) VALUES
('G-01', '一号仓', 500, '在储'),
('G-02', '二号仓', 300, '在储'),
('G-03', '三号仓', 200, '空仓'),
('G-04', '四号仓', 400, '维修');

INSERT INTO grain_batch (code, variety, granary_id, quantity, in_date, moisture, status) VALUES
('GB-2026-01', '小麦', 1, 300, '2026-09-10', 12.5, '在储'),
('GB-2026-02', '玉米', 1, 150, '2026-09-12', 14.0, '在储'),
('GB-2026-03', '稻谷', 2, 280, '2026-09-08', 13.8, '在储'),
('GB-2026-04', '大豆', 3, 0, '2026-08-20', 11.0, '已出库');

INSERT INTO temp_record (granary_id, record_date, temperature, humidity, recorder, result, during_fumigation) VALUES
(1, '2026-09-16', 22.5, 55, '李保管', '正常', 0),
(2, '2026-09-16', 24.0, 58, '王保管', '正常', 0),
(1, '2026-09-15', 23.1, 54, '李保管', '正常', 0);

INSERT INTO stock_move (code, batch_id, move_type, quantity, move_date, operator, status) VALUES
('SM-0001', 3, '出库', 100, '2026-09-15', '王保管', '已完成'),
('SM-0002', 1, '入库', 50, '2026-09-16', '李保管', '待执行');
