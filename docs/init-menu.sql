-- 菜单初始化 SQL（适用于 Erupt UPMS）
-- 作用：创建“质检管理 -> 通话记录”菜单入口
-- 说明：进入通话记录列表页后，顶部会出现“导入表格”按钮，可直接打开 Excel / CSV 导入页面
-- 说明：
-- 1）一级菜单仅作为目录使用，所以 type / value 置空
-- 2）二级菜单使用 Erupt 的表格视图，对应实体类名 CallRecord
-- 3）脚本按 code 做幂等控制，可重复执行

-- 1. 创建一级菜单：质检管理
INSERT INTO e_upms_menu (
    name, status, parent_menu_id, type, value, sort, icon, code, param, create_time, update_time
)
SELECT
    '质检管理', 1, NULL, NULL, NULL, 500, 'fa fa-headphones', 'CALL_QC_ROOT', NULL, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM e_upms_menu WHERE code = 'CALL_QC_ROOT'
);

-- 2. 创建二级菜单：通话记录
INSERT INTO e_upms_menu (
    name, status, parent_menu_id, type, value, sort, icon, code, param, create_time, update_time
)
SELECT
    '通话记录',
    1,
    p.id,
    'table',
    'CallRecord',
    10,
    'fa fa-phone',
    'CALL_QC_CALL_RECORD',
    NULL,
    NOW(),
    NOW()
FROM e_upms_menu p
WHERE p.code = 'CALL_QC_ROOT'
  AND NOT EXISTS (
    SELECT 1 FROM e_upms_menu WHERE code = 'CALL_QC_CALL_RECORD'
  );

-- 3. 可选：把默认超管 erupt 的首页菜单指向“通话记录”
UPDATE e_upms_user u
SET u.erupt_menu_id = (
    SELECT m.id FROM e_upms_menu m WHERE m.code = 'CALL_QC_CALL_RECORD' LIMIT 1
)
WHERE u.account = 'erupt'
  AND EXISTS (SELECT 1 FROM e_upms_menu m WHERE m.code = 'CALL_QC_CALL_RECORD');

-- 4. 可选：如果你不是用超管账号，而是用普通角色账号登录，
--    需要把菜单分配给对应角色。下面是模板，执行前把 ROLE_CODE_HERE 改成真实角色编码。
-- INSERT INTO e_upms_role_menu (role_id, menu_id)
-- SELECT r.id, m.id
-- FROM e_upms_role r
-- JOIN e_upms_menu m ON m.code IN ('CALL_QC_ROOT', 'CALL_QC_CALL_RECORD')
-- WHERE r.code = 'ROLE_CODE_HERE'
--   AND NOT EXISTS (
--       SELECT 1 FROM e_upms_role_menu rm
--       WHERE rm.role_id = r.id AND rm.menu_id = m.id
--   );


-- 5. 创建二级菜单：通话记录导入
INSERT INTO e_upms_menu (
    name, status, parent_menu_id, type, value, sort, icon, code, param, create_time, update_time
)
SELECT
    '通话记录导入',
    1,
    p.id,
    'router',
    '/pages/call-record-import',
    20,
    'fa fa-file-excel-o',
    'CALL_QC_CALL_RECORD_IMPORT',
    NULL,
    NOW(),
    NOW()
FROM e_upms_menu p
WHERE p.code = 'CALL_QC_ROOT'
  AND NOT EXISTS (
    SELECT 1 FROM e_upms_menu WHERE code = 'CALL_QC_CALL_RECORD_IMPORT'
  );

-- 6. 如需把“通话记录导入”分配给普通角色，追加这个菜单编码
-- INSERT INTO e_upms_role_menu (role_id, menu_id)
-- SELECT r.id, m.id
-- FROM e_upms_role r
-- JOIN e_upms_menu m ON m.code IN ('CALL_QC_ROOT', 'CALL_QC_CALL_RECORD', 'CALL_QC_CALL_RECORD_IMPORT')
-- WHERE r.code = 'ROLE_CODE_HERE'
--   AND NOT EXISTS (
--       SELECT 1 FROM e_upms_role_menu rm
--       WHERE rm.role_id = r.id AND rm.menu_id = m.id
--   );
