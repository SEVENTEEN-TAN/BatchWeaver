-- db2 表结构：业务数据表

CREATE TABLE DEMO_USER (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    email NVARCHAR(100),
    birth_date DATE,
    created_at DATETIME DEFAULT GETDATE()
);

CREATE INDEX idx_demo_user_email ON DEMO_USER(email);
