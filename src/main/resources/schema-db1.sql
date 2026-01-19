-- db1 表结构：Spring Batch 元数据表 + 业务表
-- Spring Batch 元数据表由框架自动创建（spring.batch.jdbc.initialize-schema=always）

-- 业务表示例（可选，如果 db1 也存储业务数据）
CREATE TABLE DEMO_USER (
    id INT PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    email NVARCHAR(100),
    birth_date DATE,
    created_at DATETIME DEFAULT GETDATE()
);

CREATE INDEX idx_demo_user_email ON DEMO_USER(email);
