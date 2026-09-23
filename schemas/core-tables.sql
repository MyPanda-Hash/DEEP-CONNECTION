-- 核心表结构(摘自 YINJIA-MES db/HSDZ_MES.sql;light-mes 对应见注释)

-- ===== yj_panel =====
IF OBJECT_ID('dbo.yj_panel','U') IS NULL CREATE TABLE dbo.[yj_panel] (
  [panel_code] varchar(40) NOT NULL,
  [panel_name] nvarchar(60) NOT NULL,
  [category] nvarchar(20) NOT NULL,
  [mode] varchar(20) NOT NULL,
  [line_table] sysname NOT NULL,
  [head_table] sysname NULL,
  [group_col] sysname NULL,
  [pk_col] sysname NOT NULL,
  [code_col] sysname NULL,
  [prefix] varchar(10) NULL,
  [date_col] sysname NULL,
  [page_size] int NULL,
  [detail_key] nvarchar(30) NULL,
  [config] nvarchar(max) NULL,
  [config_at] datetime NULL,
  [module_group] nvarchar(40) NULL,
  [panel_name_en] nvarchar(200) NULL
);

-- ===== yj_field =====
IF OBJECT_ID('dbo.yj_field','U') IS NULL CREATE TABLE dbo.[yj_field] (
  [id] int IDENTITY(1,1) NOT NULL,
  [panel_code] varchar(40) NOT NULL,
  [col_name] sysname NOT NULL,
  [label] nvarchar(60) NOT NULL,
  [data_type] nvarchar(20) NOT NULL DEFAULT (N'文本'),
  [dict_sql] nvarchar(500) NULL,
  [ref_panel] varchar(40) NULL,
  [ref_field] sysname NULL,
  [display_field] sysname NULL,
  [place] varchar(60) NOT NULL DEFAULT ('detail'),
  [seq] int NOT NULL DEFAULT ((0)),
  [width] int NULL,
  [editable] bit NOT NULL DEFAULT ((1)),
  [required] bit NOT NULL DEFAULT ((0)),
  [hidden] bit NOT NULL DEFAULT ((0)),
  [alias] nvarchar(60) NULL,
  [visible] bit NOT NULL DEFAULT ((1)),
  [label_en] nvarchar(200) NULL
);

-- ===== yj_translation =====
IF OBJECT_ID('dbo.yj_translation','U') IS NULL CREATE TABLE dbo.[yj_translation] (
  [id] int IDENTITY(1,1) NOT NULL,
  [scope] varchar(20) NOT NULL,
  [ref_key] nvarchar(200) NOT NULL,
  [locale] varchar(10) NOT NULL,
  [text] nvarchar(500) NOT NULL,
  [source] varchar(10) NOT NULL DEFAULT ('manual'),
  [created_at] datetime2 NOT NULL DEFAULT (sysdatetime()),
  [updated_at] datetime2 NOT NULL DEFAULT (sysdatetime())
);

-- ===== yj_locale =====
IF OBJECT_ID('dbo.yj_locale','U') IS NULL CREATE TABLE dbo.[yj_locale] (
  [locale] varchar(10) NOT NULL,
  [name_zh] nvarchar(50) NOT NULL,
  [name_native] nvarchar(50) NOT NULL,
  [enabled] bit NOT NULL DEFAULT ((1)),
  [sort] int NOT NULL DEFAULT ((100))
);

-- ===== yj_doc_status =====
IF OBJECT_ID('dbo.yj_doc_status','U') IS NULL CREATE TABLE dbo.[yj_doc_status] (
  [id] int IDENTITY(1,1) NOT NULL,
  [panel_code] varchar(40) NOT NULL,
  [doc_no] nvarchar(60) NOT NULL,
  [shr] nvarchar(40) NULL,
  [shsj] datetime NULL,
  [canceled] char(1) NOT NULL DEFAULT ('N'),
  [cancel_by] nvarchar(40) NULL,
  [cancel_at] datetime NULL,
  [update_at] datetime NOT NULL DEFAULT (getdate()),
  [pending] char(1) NULL DEFAULT ('N'),
  [pending_by] nvarchar(40) NULL,
  [pending_at] datetime2 NULL,
  [stopped] char(1) NULL DEFAULT ('N')
);

-- ===== yj_form_approval =====
IF OBJECT_ID('dbo.yj_form_approval','U') IS NULL CREATE TABLE dbo.[yj_form_approval] (
  [id] int IDENTITY(1,1) NOT NULL,
  [panel_code] nvarchar(40) NOT NULL,
  [form_no] nvarchar(60) NOT NULL,
  [action] nvarchar(20) NOT NULL,
  [result] nvarchar(10) NULL,
  [node_no] int NOT NULL DEFAULT ((1)),
  [operator] nvarchar(40) NULL,
  [opinion] nvarchar(max) NULL,
  [create_time] datetime2 NOT NULL DEFAULT (sysdatetime())
);

-- ===== yj_role_panel =====
IF OBJECT_ID('dbo.yj_role_panel','U') IS NULL CREATE TABLE dbo.[yj_role_panel] (
  [id] int IDENTITY(1,1) NOT NULL,
  [role_id] int NOT NULL,
  [panel_code] nvarchar(40) NOT NULL,
  [can_approve] char(1) NOT NULL DEFAULT ('N'),
  [perms] nvarchar(500) NULL
);

-- ===== yj_user =====
IF OBJECT_ID('dbo.yj_user','U') IS NULL CREATE TABLE dbo.[yj_user] (
  [id] int IDENTITY(1,1) NOT NULL,
  [username] nvarchar(40) NOT NULL,
  [password_hash] nvarchar(200) NOT NULL,
  [real_name] nvarchar(60) NULL,
  [is_admin] char(1) NOT NULL DEFAULT ('N'),
  [dept_id] int NULL,
  [role_id] int NULL,
  [enabled] char(1) NOT NULL DEFAULT ('1')
);

-- ===== yj_usage_log =====
IF OBJECT_ID('dbo.yj_usage_log','U') IS NULL CREATE TABLE dbo.[yj_usage_log] (
  [id] bigint IDENTITY(1,1) NOT NULL,
  [user_name] nvarchar(50) NOT NULL,
  [real_name] nvarchar(50) NOT NULL,
  [event_type] nvarchar(10) NOT NULL,
  [panel_name] nvarchar(100) NULL,
  [action_name] nvarchar(50) NULL,
  [doc_no] nvarchar(200) NULL,
  [ip] nvarchar(50) NULL,
  [created_at] datetime2 NOT NULL DEFAULT (sysdatetime())
);

-- ===== form_flow_link =====
IF OBJECT_ID('dbo.form_flow_link','U') IS NULL CREATE TABLE dbo.[form_flow_link] (
  [id] int IDENTITY(1,1) NOT NULL,
  [source_panel_code] varchar(50) NOT NULL,
  [source_form_no] nvarchar(60) NOT NULL,
  [source_detail_key] nvarchar(50) NULL,
  [source_line_key] nvarchar(100) NOT NULL,
  [target_panel_code] varchar(50) NOT NULL,
  [target_form_no] nvarchar(60) NOT NULL,
  [target_detail_key] nvarchar(50) NULL,
  [target_line_key] nvarchar(100) NULL,
  [inventory_code] nvarchar(50) NULL,
  [source_quantity] decimal(18,6) NULL,
  [linked_quantity] decimal(18,6) NULL,
  [link_status] varchar(20) NOT NULL DEFAULT ('ACTIVE'),
  [create_by] nvarchar(50) NULL,
  [create_time] datetime2 NOT NULL DEFAULT (sysdatetime()),
  [release_time] datetime2 NULL
);

-- ===== bs_dept =====
IF OBJECT_ID('dbo.bs_dept','U') IS NULL CREATE TABLE dbo.[bs_dept] (
  [id] int IDENTITY(1,1) NOT NULL,
  [部门编码] nvarchar(200) NOT NULL,
  [部门名称] nvarchar(200) NOT NULL,
  [负责人] nvarchar(200) NULL,
  [停用] bit NULL,
  [备注] nvarchar(500) NULL,
  [状态] nvarchar(10) NOT NULL DEFAULT (N'启用'),
  [asp_user1] nvarchar(50) NULL,
  [asp_time1] datetime2 NULL,
  [asp_user2] nvarchar(50) NULL,
  [asp_time2] datetime2 NULL,
  [asp_cancel] char(1) NULL DEFAULT ('N')
);

-- ===== bs_inv =====
IF OBJECT_ID('dbo.bs_inv','U') IS NULL CREATE TABLE dbo.[bs_inv] (
  [id] int IDENTITY(1,1) NOT NULL,
  [所属类别] nvarchar(100) NOT NULL,
  [存货编码] nvarchar(200) NOT NULL,
  [存货名称] nvarchar(200) NOT NULL,
  [规格型号] nvarchar(200) NULL,
  [计价方式] nvarchar(100) NULL,
  [品牌] nvarchar(200) NULL,
  [计量单位] nvarchar(100) NULL,
  [属性] nvarchar(100) NULL,
  [参考成本] decimal(18,4) NULL,
  [最新成本] decimal(18,4) NULL,
  [建档日期] date NULL,
  [停用] bit NULL,
  [备注] nvarchar(500) NULL,
  [状态] nvarchar(10) NOT NULL DEFAULT (N'启用'),
  [asp_user1] nvarchar(50) NULL,
  [asp_time1] datetime2 NULL,
  [asp_user2] nvarchar(50) NULL,
  [asp_time2] datetime2 NULL,
  [asp_cancel] char(1) NULL DEFAULT ('N')
);

-- ===== bd_so_order =====
IF OBJECT_ID('dbo.bd_so_order','U') IS NULL CREATE TABLE dbo.[bd_so_order] (
  [id] int IDENTITY(1,1) NOT NULL,
  [单据编号] nvarchar(200) NULL,
  [单据日期] date NULL,
  [客户] nvarchar(200) NULL,
  [客户编码] nvarchar(100) NULL,
  [结算客户] nvarchar(100) NULL,
  [部门] nvarchar(100) NULL,
  [部门负责人] nvarchar(200) NULL,
  [业务员] nvarchar(100) NULL,
  [项目] nvarchar(200) NULL,
  [预计交货日期] date NULL,
  [联系人] nvarchar(200) NULL,
  [备注] nvarchar(500) NULL,
  [单据状态] nvarchar(10) NOT NULL DEFAULT (N'草稿'),
  [审核人] nvarchar(50) NULL,
  [审核时间] datetime2 NULL,
  [审批人] nvarchar(50) NULL,
  [审批时间] datetime2 NULL,
  [asp_user1] nvarchar(50) NULL,
  [asp_time1] datetime2 NULL,
  [asp_cancel] char(1) NULL DEFAULT ('N'),
  [asp_user2] nvarchar(50) NULL,
  [asp_time2] datetime2 NULL
);

-- ===== bl_so_order =====
IF OBJECT_ID('dbo.bl_so_order','U') IS NULL CREATE TABLE dbo.[bl_so_order] (
  [id] int IDENTITY(1,1) NOT NULL,
  [单据编号] nvarchar(100) NULL,
  [品牌] nvarchar(200) NULL,
  [存货名称] nvarchar(200) NULL,
  [存货编码] nvarchar(100) NULL,
  [规格型号] nvarchar(200) NULL,
  [数量] decimal(18,4) NULL,
  [销售单位] nvarchar(100) NULL,
  [单价] decimal(18,4) NULL,
  [税率%] decimal(18,4) NULL,
  [含税单价] decimal(18,4) NULL,
  [金额] decimal(18,4) NULL,
  [含税金额] decimal(18,4) NULL,
  [折扣金额] decimal(18,4) NULL,
  [预计交货日期] date NULL,
  [现存量] decimal(18,4) NULL,
  [备注] nvarchar(200) NULL,
  [asp_user1] nvarchar(50) NULL,
  [asp_cancel] char(1) NULL DEFAULT ('N'),
  [asp_user2] nvarchar(50) NULL,
  [asp_time2] datetime2 NULL,
  [asp_time1] datetime2 NULL
);

-- ===== px_column_pref (light-mes 用户级列偏好) =====
-- ============================================================
-- 2026-09-01 阶段 C：「表格调整」列定制（顺序/显隐/别名）按用户持久化
--  新表 px_column_pref：panel_code + owner（用户名，'' 预留全局默认）维度保存
--  每列一行：seq 顺序（步长 10）/ alias 别名 / visible 显隐
--  无行 = 默认列序（读取出口叠加，绝不写回 panel_config.config）
-- 幂等：CREATE TABLE IF NOT EXISTS，可重复执行
-- ============================================================
-- 适用库：light_mes（MySQL 8.x）

CREATE TABLE IF NOT EXISTS px_column_pref (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  panel_code  VARCHAR(50)  NOT NULL COMMENT '面板编码',
  owner       VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '用户名（'' 预留全局默认行）',
  col_name    VARCHAR(100) NOT NULL COMMENT '列名（中文列名即数据键）',
  seq         INT          DEFAULT 100 COMMENT '顺序（步长 10）',
  alias       VARCHAR(100) NULL COMMENT '栏名别名（空=默认列名）',
  visible     TINYINT(1)   DEFAULT 1 COMMENT '1显示 0隐藏',
  update_by   VARCHAR(50)  NULL,
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_col_pref (panel_code, owner, col_name),
  KEY idx_col_pref (panel_code, owner)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT '表格列定制表（按用户）';

-- 校验：无种子（无行=默认列序）
SELECT COUNT(*) AS pref_rows FROM px_column_pref;
