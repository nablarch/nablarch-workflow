package nablarch.integration.workflow.dao;

/**
 * ワークフローインスタンステーブルの定義を表すクラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class WorkflowInstanceSchema {

    // ----- table name -----

    /** ワークフローインスタンステーブル名 */
    private String instanceTableName;

    /** インスタンスフローノードテーブル名 */
    private String instanceFlowNodeTableName;

    /** タスク担当ユーザテーブル名 */
    private String assignedUserTableName;

    /** タスク担当グループテーブル名 */
    private String assignedGroupTableName;

    /** アクティブフローノードのテーブル名 */
    private String activeFlowNodeTableName;

    /** アクティブユーザタスクテーブル名 */
    private String activeUserTaskTableName;

    /** アクティブグループタスクテーブル名 */
    private String activeGroupTaskTableName;

    // ----- column name -----

    /** インスタンスIDカラム名 */
    private String instanceIdColumnName;

    /** ワークフローIDカラム名 */
    private String workflowIdColumnName;

    /** バージョン番号カラム名 */
    private String versionColumnName;

    /** フローノードIDカラム名 */
    private String flowNodeIdColumnName;

    /** 担当者のカラム名 */
    private String assignedUserColumnName;

    /** 実行順カラム名 */
    private String executionOrderColumnName;

    /** 担当グループのカラム名 */
    private String assignedGroupColumnName;

    /**
     * ワークフローインスタンステーブル名を取得する。
     *
     * @return ワークフローインスタンステーブル名を取得する。
     */
    public String getInstanceTableName() {
        return instanceTableName;
    }

    /**
     * ワークフローインスタンステーブル名を設定する。
     *
     * @param instanceTableName ワークフローインスタンステーブル名
     */
    public void setInstanceTableName(String instanceTableName) {
        this.instanceTableName = instanceTableName;
    }

    /**
     * インスタンスフローノードテーブル名を取得する。
     *
     * @return インスタンスフローノードテーブル名
     */
    public String getInstanceFlowNodeTableName() {
        return instanceFlowNodeTableName;
    }

    /**
     * インスタンスフローノードテーブル名を設定する。
     *
     * @param instanceFlowNodeTableName インスタンスフローノードテーブル名
     */
    public void setInstanceFlowNodeTableName(String instanceFlowNodeTableName) {
        this.instanceFlowNodeTableName = instanceFlowNodeTableName;
    }

    /**
     * タスク担当ユーザテーブル名を取得する。
     *
     * @return タスク担当ユーザテーブル名
     */
    public String getAssignedUserTableName() {
        return assignedUserTableName;
    }

    /**
     * タスク担当ユーザテーブル名を設定する。
     *
     * @param assignedUserTableName タスク担当ユーザテーブル名
     */
    public void setAssignedUserTableName(String assignedUserTableName) {
        this.assignedUserTableName = assignedUserTableName;
    }

    /**
     * タスク担当グループテーブル名を取得する。
     *
     * @return タスク担当グループテーブル名
     */
    public String getAssignedGroupTableName() {
        return assignedGroupTableName;
    }

    /**
     * タスク担当グループテーブル名を設定する。
     *
     * @param assignedGroupTableName タスク担当グループテーブル名
     */
    public void setAssignedGroupTableName(String assignedGroupTableName) {
        this.assignedGroupTableName = assignedGroupTableName;
    }

    /**
     * アクティブフローノードテーブルのテーブル名を取得する。
     *
     * @return アクティブフローノードテーブルのテーブル名
     */
    public String getActiveFlowNodeTableName() {
        return activeFlowNodeTableName;
    }

    /**
     * アクティブフローノードテーブルのテーブル名を設定する。
     *
     * @param activeFlowNodeTableName アクティブフローノードテーブルのテーブル名
     */
    public void setActiveFlowNodeTableName(String activeFlowNodeTableName) {
        this.activeFlowNodeTableName = activeFlowNodeTableName;
    }

    /**
     * アクティブユーザタスクテーブル名を取得する。
     *
     * @return アクティブユーザタスクテーブル名
     */
    public String getActiveUserTaskTableName() {
        return activeUserTaskTableName;
    }

    /**
     * アクティブユーザタスクテーブル名を設定する。
     *
     * @param activeUserTaskTableName アクティブユーザタスクテーブル名
     */
    public void setActiveUserTaskTableName(String activeUserTaskTableName) {
        this.activeUserTaskTableName = activeUserTaskTableName;
    }

    /**
     * アクティブグループタスクテーブル名を取得する。
     *
     * @return アクティブグループタスクテーブル名
     */
    public String getActiveGroupTaskTableName() {
        return activeGroupTaskTableName;
    }

    /**
     * アクティブグループタスクテーブル名を取得する。
     *
     * @param activeGroupTaskTableName アクティブグループタスクテーブル名
     */
    public void setActiveGroupTaskTableName(String activeGroupTaskTableName) {
        this.activeGroupTaskTableName = activeGroupTaskTableName;
    }

    /**
     * インスタンスIDカラム名を取得する。
     *
     * @return インスタンスIDカラム名
     */
    public String getInstanceIdColumnName() {
        return instanceIdColumnName;
    }

    /**
     * インスタンスIDカラム名を設定する。
     *
     * @param instanceIdColumnName インスタンスIDカラム名
     */
    public void setInstanceIdColumnName(String instanceIdColumnName) {
        this.instanceIdColumnName = instanceIdColumnName;
    }

    /**
     * ワークフローIDカラム名を取得する。
     *
     * @return ワークフローIDカラム名
     */
    public String getWorkflowIdColumnName() {
        return workflowIdColumnName;
    }

    /**
     * ワークフローIDカラム名を設定する。
     *
     * @param workflowIdColumnName ワークフローIDカラム名
     */
    public void setWorkflowIdColumnName(String workflowIdColumnName) {
        this.workflowIdColumnName = workflowIdColumnName;
    }

    /**
     * バージョン番号カラム名を取得する。
     *
     * @return バージョン番号カラム名
     */
    public String getVersionColumnName() {
        return versionColumnName;
    }

    /**
     * バージョン番号カラム名を設定する。
     *
     * @param versionColumnName バージョン番号カラム名
     */
    public void setVersionColumnName(String versionColumnName) {
        this.versionColumnName = versionColumnName;
    }

    /**
     * フローノードIDカラム名を取得する。
     *
     * @return フローノードIDカラム名
     */
    public String getFlowNodeIdColumnName() {
        return flowNodeIdColumnName;
    }

    /**
     * フローノードIDカラム名を設定する。
     *
     * @param flowNodeIdColumnName フローノードIDカラム名カラム名
     */
    public void setFlowNodeIdColumnName(String flowNodeIdColumnName) {
        this.flowNodeIdColumnName = flowNodeIdColumnName;
    }

    /**
     * 担当者のカラム名を取得する。
     *
     * @return 担当者のカラム名
     */
    public String getAssignedUserColumnName() {
        return assignedUserColumnName;
    }

    /**
     * 担当者のカラム名を取得する。
     *
     * @param assignedUserColumnName 担当者のカラム名
     */
    public void setAssignedUserColumnName(String assignedUserColumnName) {
        this.assignedUserColumnName = assignedUserColumnName;
    }

    /**
     * 実行順カラム名を取得する。
     *
     * @return 実行順カラム名
     */
    public String getExecutionOrderColumnName() {
        return executionOrderColumnName;
    }

    /**
     * 実行順カラム名を設定する。
     *
     * @param executionOrderColumnName 実行順カラム名
     */
    public void setExecutionOrderColumnName(String executionOrderColumnName) {
        this.executionOrderColumnName = executionOrderColumnName;
    }

    /**
     * 担当グループのカラム名を取得する。
     *
     * @return 担当グループのカラム名
     */
    public String getAssignedGroupColumnName() {
        return assignedGroupColumnName;
    }

    /**
     * 担当グループのカラム名を設定する。
     *
     * @param assignedGroupColumnName 担当グループのカラム名
     */
    public void setAssignedGroupColumnName(String assignedGroupColumnName) {
        this.assignedGroupColumnName = assignedGroupColumnName;
    }

}

