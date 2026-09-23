// ============ 状态机(照搬 light-mes:草稿⇄已审核 + 审批流) ============

    private Map<String, Object> audit(PanelRegistry.PanelDef def, Map<String, Object> formData) {
        if (!def.isDoc()) throw new IllegalStateException("档案面板无审核动作");
        String no = requireNo(formData);
        ensureDocExists(def, no);
        Map<String, Object> st = docStatusOf(def.code(), no);
        if ("已作废".equals(st.get("status"))) throw new IllegalStateException("已作废单据不可审核");
        if ("已中止".equals(st.get("status"))) throw new IllegalStateException("已中止单据不可审核，请先恢复");
        if ("已审核".equals(st.get("status"))) throw new IllegalStateException("单据已是已审核状态");
        if ("审批中".equals(st.get("status"))) throw new IllegalStateException("审批中单据不可直接审核，请走审批流");
        jdbc.update("MERGE yj_doc_status AS t USING (VALUES (?, ?)) AS s(panel_code, doc_no) "
                        + "ON t.panel_code = s.panel_code AND t.doc_no = s.doc_no "
                        + "WHEN MATCHED THEN UPDATE SET shr = ?, shsj = GETDATE(), canceled = 'N', pending = 'N', update_at = GETDATE() "
                        + "WHEN NOT MATCHED THEN INSERT (panel_code, doc_no, shr, shsj, canceled, pending, update_at) "
                        + "VALUES (s.panel_code, s.doc_no, ?, GETDATE(), 'N', 'N', GETDATE());",
                def.code(), no, currentUserName(), currentUserName());
        return result(no, "已审核");
    }

    private Map<String, Object> unaudit(PanelRegistry.PanelDef def, Map<String, Object> formData) {
        String no = requireNo(formData);
        Map<String, Object> st = docStatusOf(def.code(), no);
        if (!"已审核".equals(st.get("status"))) throw new IllegalStateException("仅已审核状态可弃审");
        jdbc.update("UPDATE yj_doc_status SET shr = NULL, shsj = NULL, update_at = GETDATE()"
                + " WHERE panel_code = ? AND doc_no = ?", def.code(), no);
        recordApproval(def.code(), no, "UNAUDIT", "PENDING", opinionOf(formData));
        return result(no, "草稿");
    }

    // ---- 中止(对齐 PANDA/T+ 整单中止、生产加工单中止执行):仅已审核可中止,恢复保留原审核留痕 ----

    /** 中止:已审核 → 已中止(留痕 stop_by/stop_at;shr 保留,取消中止后回到已审核) */
    private Map<String, Object> stop(PanelRegistry.PanelDef def, Map<String, Object> formData) {
        String no = requireNo(formData);
        ensureDocExists(def, no);
        Map<String, Object> st = docStatusOf(def.code(), no);
        String status = String.valueOf(st.get("status"));
        if ("已作废".equals(status)) throw new IllegalStateException("已作废单据不可中止");
        if ("已中止".equals(status)) throw new IllegalStateException("单据已是已中止状态");
        if (!"已审核".equals(status)) throw new IllegalStateException("仅已审核状态可中止");
        jdbc.update("MERGE yj_doc_status AS t USING (VALUES (?, ?)) AS s(panel_code, doc_no) "
                        + "ON t.panel_code = s.panel_code AND t.doc_no = s.doc_no "
                        + "WHEN MATCHED THEN UPDATE SET stopped = 'Y', stop_by = ?, stop_at = GETDATE(), update_at = GETDATE() "
                        + "WHEN NOT MATCHED THEN INSERT (panel_code, doc_no, stopped, stop_by, stop_at, update_at) "
                        + "VALUES (s.panel_code, s.doc_no, 'Y', ?, GETDATE(), GETDATE());",
                def.code(), no, currentUserName(), currentUserName());
        recordApproval(def.code(), no, "STOP", "STOPPED", opinionOf(formData));
        return result(no, "已中止");
    }

    /** 取消中止(生产加工单「草稿」按钮):已中止 → 恢复(shr 保留则已审核,否则草稿) */
    private Map<String, Object> unstop(PanelRegistry.PanelDef def, Map<String, Object> formData) {
        String no = requireNo(formData);
        Map<String, Object> st = docStatusOf(def.code(), no);
        if (!"已中止".equals(st.get("status"))) throw new IllegalStateException("仅已中止状态可恢复");
        jdbc.update("UPDATE yj_doc_status SET stopped = 'N', stop_by = NULL, stop_at = NULL, update_at = GETDATE()"
                + " WHERE panel_code = ? AND doc_no = ?", def.code(), no);
        recordApproval(def.code(), no, "UNSTOP", "UNSTOPPED", opinionOf(formData));
        Map<String, Object> after = docStatusOf(def.code(), no);
        return result(no, String.valueOf(after.get("status")));
    }

    // ---- 审批流(照搬 light-mes PxService):提交/通过/驳回全留痕,防伪校验 ----

    /** 提交审批:仅草稿 → 审批中 */
    private Map<String, Object> submitApproval(PanelRegistry.PanelDef def, Map<String, Object> formData) {
        String no = requireNo(formData);
        ensureDocExists(def, no);
        Map<String, Object> st = docStatusOf(def.code(), no);
        if (!"草稿".equals(st.get("status"))) throw new IllegalStateException("仅草稿状态可提交审批");
        String operator = currentUserName();
        jdbc.update("MERGE yj_doc_status AS t USING (VALUES (?, ?)) AS s(panel_code, doc_no) "
                        + "ON t.panel_code = s.panel_code AND t.doc_no = s.doc_no "
                        + "WHEN MATCHED THEN UPDATE SET pending = 'Y', pending_by = ?, pending_at = GETDATE(), shr = NULL, shsj = NULL, canceled = 'N', update_at = GETDATE() "
                        + "WHEN NOT MATCHED THEN INSERT (panel_code, doc_no, pending, pending_by, pending_at, canceled, update_at) "
                        + "VALUES (s.panel_code, s.doc_no, 'Y', ?, GETDATE(), 'N', GETDATE());",
                def.code(), no, operator, operator);
        recordApproval(def.code(), no, "SUBMIT", "PENDING", opinionOf(formData));
        return result(no, "审批中");
    }

    /** 审批通过:仅审批中 → 已审核(需管理员/审批权限;审核人=当前登录人) */
    private Map<String, Object> approveApproval(PanelRegistry.PanelDef def, Map<String, Object> formData) {
        String no = requireNo(formData);
        Map<String, Object> st = docStatusOf(def.code(), no);
        if (!"审批中".equals(st.get("status"))) throw new IllegalStateException("仅审批中状态可审批通过");
        requirePendingSubmission(def.code(), no);
        requireApprover();
        String operator = currentUserName();
        String opinion = opinionOf(formData);
        jdbc.update("UPDATE yj_doc_status SET pending = 'N', shr = ?, shsj = GETDATE(), update_at = GETDATE()"
                + " WHERE panel_code = ? AND doc_no = ?", operator, def.code(), no);
        recordApproval(def.code(), no, "APPROVE", "APPROVED", opinion);
        return result(no, "已审核");
    }

    /** 审批驳回:仅审批中 → 草稿(意见必填,驳回后修改可重新提交) */
    private Map<String, Object> rejectApproval(PanelRegistry.PanelDef def, Map<String, Object> formData) {
        String no = requireNo(formData);
        Map<String, Object> st = docStatusOf(def.code(), no);
        if (!"审批中".equals(st.get("status"))) throw new IllegalStateException("仅审批中状态可审批驳回");
        requirePendingSubmission(def.code(), no);
        requireApprover();
        String opinion = opinionOf(formData);
        if (opinion.isEmpty()) throw new IllegalStateException("审批驳回必须填写审批意见");
        jdbc.update("UPDATE yj_doc_status SET pending = 'N', update_at = GETDATE()"
                + " WHERE panel_code = ? AND doc_no = ?", def.code(), no);
        recordApproval(def.code(), no, "REJECT", "REJECTED", opinion);
        return result(no, "草稿");
    }

    /** 审批情况:返回该单据全部审批记录(时间升序) */
    private Map<String, Object> approvalHistory(PanelRegistry.PanelDef def, Map<String, Object> formData) {
        String no = requireNo(formData);
        Map<String, Object> out = new HashMap<>();
        out.put("编号", no);
        out.put("list", queryApprovalHistory(def.code(), no));
        return out;
    }

    /** 审批通过/驳回必须紧跟一次有效提交,防止仅改状态后伪造审批结果(light-mes requirePendingSubmission) */
    private void requirePendingSubmission(String panelCode, String formNo) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT TOP 1 action, result FROM yj_form_approval WHERE panel_code = ? AND form_no = ? ORDER BY id DESC",
                panelCode, formNo);
        if (rows.isEmpty() || !"SUBMIT".equals(rows.get(0).get("action"))
                || !"PENDING".equals(rows.get(0).get("result"))) {
            throw new IllegalStateException("单据尚未提交审批，不能审批通过或驳回");
        }
    }

    /** 审批权限:YINJIA 以 yj_user.is_admin 承载(light-mes 为角色 can_approve) */
    private void requireApprover() {
        String user = currentUserName();
        List<String> admins = jdbc.query(
                "SELECT username FROM yj_user WHERE username = ? AND is_admin = 'Y'",
                (rs, i) -> rs.getString(1), user);
        if (admins.isEmpty()) throw new org.springframework.security.access.AccessDeniedException("当前用户无审批权限");
    }

    private void recordApproval(String panelCode, String formNo, String action, String result, String opinion) {
        jdbc.update("INSERT INTO yj_form_approval (panel_code, form_no, action, result, node_no, operator, opinion, create_time) "
                        + "VALUES (?,?,?,?,1,?,?,SYSDATETIME())",
                panelCode, formNo, action, result, currentUserName(),
                opinion == null || opinion.isEmpty() ? null : opinion);
    }

    private String opinionOf(Map<String, Object> formData) {
        Object v = formData == null ? null : formData.get("审批意见");
        return v == null ? "" : String.valueOf(v).trim();
    }

    public List<Map<String, Object>> queryApprovalHistory(String panelCode, String formNo) {
        return jdbc.query("SELECT action, result, node_no, operator, opinion, create_time FROM yj_form_approval"
                        + " WHERE panel_code = ? AND form_no = ? ORDER BY id ASC",
                (rs, i) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("action", rs.getString("action"));
                    m.put("result", rs.getString("result"));
                    m.put("operator", rs.getString("operator"));
                    m.put("opinion", rs.getString("opinion"));
                    m.put("nodeNo", rs.getInt("node_no"));
                    m.put("createTime", rs.getTimestamp("create_time") == null ? ""
                            : rs.getTimestamp("create_time").toLocalDateTime()
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    return m;
                }, panelCode, formNo);
    }

    /** 删除:单据=作废(仅草稿可删,对齐 light-mes);档案=当前行软删 */
    private Map<String, Object> delete(PanelRegistry.PanelDef def, Map<String, Object> formData) {
        String user = currentUserName();
        if (def.isDoc()) {
            String no = requireNo(formData);
            Map<String, Object> st = docStatusOf(def.code(), no);
            if (!"草稿".equals(st.get("status"))) throw new IllegalStateException("仅草稿状态可删除（已审核请先弃审）");
            jdbc.update("MERGE yj_doc_status AS t USING (VALUES (?, ?)) AS s(panel_code, doc_no) "
                            + "ON t.panel_code = s.panel_code AND t.doc_no = s.doc_no "
                            + "WHEN MATCHED THEN UPDATE SET canceled = 'Y', cancel_by = ?, cancel_at = GETDATE(), update_at = GETDATE() "
                            + "WHEN NOT MATCHED THEN INSERT (panel_code, doc_no, canceled, cancel_by, cancel_at, update_at) "
                            + "VALUES (s.panel_code, s.doc_no, 'Y', ?, GETDATE(), GETDATE());",
                    def.code(), no, user, user);
            // 选单流转占用释放:下游草稿作废,来源行重新可选(对齐 T+ 选单占用语义)
            jdbc.update("UPDATE form_flow_link SET link_status='RELEASED', release_time=SYSDATETIME()"
                    + " WHERE target_panel_code = ? AND target_form_no = ? AND link_status = 'ACTIVE'", def.code(), no);
            return result(no, "已作废");
        }
        Object no = formData.get("编号");
        if (no != null && !String.valueOf(no).isBlank()) {
            jdbc.update("UPDATE " + def.lineTable() + " SET asp_cancel='Y', asp_user2=?, asp_time2=GETDATE()"
                    + " WHERE " + def.codeCol() + " = ?", user, no);
        }
        return result(String.valueOf(no), "已作废");
    }

    public void deleteForms(String panelCode, List<String> rowCodes) {
        PanelRegistry.PanelDef def = registry.panel(panelCode);
        for (String code : rowCodes) {
            Map<String, Object> fd = new HashMap<>();
            fd.put("编号", code);
            delete(def, fd);
        }
    }

    // ============ 工具 ============

    private String requireNo(Map<String, Object> formData) {
        Object no = formData == null ? null : formData.get("编号");
        if (no == null || String.valueOf(no).isBlank()) throw new IllegalArgumentException("缺少表单编号");
        return String.valueOf(no);
    }

    private void ensureDocExists(PanelRegistry.PanelDef def, String no) {
        String table = def.hasHeadTable() ? def.headTable() : def.lineTable();
        Integer c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + def.groupCol() + " = ? AND ISNULL(asp_cancel,'N')<>'Y'",
                Integer.class, no);
        if (c == null || c == 0) throw new IllegalArgumentException("表单数据不存在：" + no);
    }

    /** 单据状态查询(供生单等领域动作校验来源单状态) */
    public Map<String, Object> docStatus(String panelCode, String no) {
        return docStatusOf(panelCode, no);
    }

    /** 状态推导:已作废 > 已中止(stopped) > 已审核(shr) > 审批中(pending) > 草稿 */
    private Map<String, Object> docStatusOf(String panelCode, String no) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT shr, canceled, stopped, pending, pending_by, pending_at FROM yj_doc_status WHERE panel_code = ? AND doc_no = ?",
                panelCode, no);
        Map<String, Object> out = new HashMap<>();
        Map<String, Object> r = rows.isEmpty() ? null : rows.get(0);
        if (r == null) {
            out.put("status", "草稿");
        } else if ("Y".equals(r.get("canceled"))) {
            out.put("status", "已作废");
        } else if ("Y".equals(r.get("stopped"))) {
            out.put("status", "已中止");
        } else if (r.get("shr") != null) {
            out.put("status", "已审核");
        } else if ("Y".equals(r.get("pending"))) {
            out.put("status", "审批中");
        } else {
            out.put("status", "草稿");
        }
        if (r != null) out.put("row", r);
        return out;
    }

    private Map<String, Object> result(String no, String status) {
        Map<String, Object> out = new HashMap<>();
        out.put("编号", no);
        out.put("单据状态", status);
        return out;
    }

    private String currentUserName() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getName() != null && !auth.getName().isBlank() ? auth.getName() : "system";
    }
}
