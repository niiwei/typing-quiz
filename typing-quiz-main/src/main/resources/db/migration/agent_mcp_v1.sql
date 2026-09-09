-- 敲脑壳 Agent MCP v1 幂等迁移（MySQL 8）。执行本文件前必须先运行
-- agent_mcp_v1_preflight.sql；若预检有结果，应停止并人工处理重复分组。
-- 应用当前默认使用 Hibernate update；此脚本供受控部署或 DBA 审核执行。

CREATE TABLE IF NOT EXISTS personal_access_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    public_id VARCHAR(36) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    created_at DATETIME NOT NULL,
    last_used_at DATETIME NULL,
    revoked_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pat_public_id (public_id),
    KEY idx_pat_user (user_id)
);

CREATE TABLE IF NOT EXISTS agent_import_request (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    request_id VARCHAR(36) NOT NULL,
    payload_hash CHAR(64) NOT NULL,
    response_json LONGTEXT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_agent_import_user_request (user_id, request_id)
);

ALTER TABLE quiz ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE quiz_group ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE quiz_group ADD COLUMN IF NOT EXISTS normalized_name VARCHAR(255)
    GENERATED ALWAYS AS (LOWER(TRIM(name))) STORED;
SET @group_name_index_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'quiz_group'
      AND index_name = 'uk_quiz_group_user_normalized_name'
);
SET @group_name_index_sql := IF(
    @group_name_index_exists = 0,
    'CREATE UNIQUE INDEX uk_quiz_group_user_normalized_name ON quiz_group (user_id, normalized_name)',
    'SELECT 1'
);
PREPARE group_name_index_stmt FROM @group_name_index_sql;
EXECUTE group_name_index_stmt;
DEALLOCATE PREPARE group_name_index_stmt;
