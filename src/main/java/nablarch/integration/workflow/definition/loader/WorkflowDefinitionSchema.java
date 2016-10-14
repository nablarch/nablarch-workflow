package nablarch.integration.workflow.definition.loader;

/**
 * ワークフロー定義テーブルの定義情報を保持するクラス。
 * <p/>
 * 本クラスでは、ワークフロープロセス定義で必要となる
 * 以下のテーブルのテーブル名及びカラム名の情報を保持する。
 * <ul>
 * <li>ワークフロー定義</li>
 * <li>レーン</li>
 * <li>フローノード</li>
 * <li>イベント</li>
 * <li>タスク</li>
 * <li>ゲートウェイ</li>
 * <li>境界イベント</li>
 * <li>境界イベントトリガー</li>
 * <li>シーケンスフロー</li>
 * </ul>
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class WorkflowDefinitionSchema {

    //----- field:table name -----

    /** ワークフロー定義テーブル名 */
    private String workflowDefinitionTableName;

    /** レーンテーブル名 */
    private String laneTableName;

    /** フローノードテーブル名 */
    private String flowNodeTableName;

    /** イベントテーブル名 */
    private String eventTableName;

    /** タスクテーブル名 */
    private String taskTableName;

    /** ゲートウェイテーブル名 */
    private String gatewayTableName;

    /** 境界イベント定義テーブル名 */
    private String boundaryEventTableName;

    /** イベントトリガー定義テーブル名 */
    private String eventTriggerTableName;

    /** シーケンスフロー定義テーブル名 */
    private String sequenceFlowTableName;

    // ----- field:column name definition -----

    /** ワークフローIDカラム名 */
    private String workflowIdColumnName;

    /** ワークフロー名カラム名 */
    private String workflowNameColumnName;

    /** バージョンカラム名 */
    private String versionColumnName;

    /** 適用日カラム名 */
    private String effectiveDateColumnName;

    /** レーンIDカラム名 */
    private String laneIdColumnName;

    /** レーン名カラム名 */
    private String laneNameColumnName;

    /** フローノードIDカラム名 */
    private String flowNodeIdColumnName;

    /** フローノード名カラム名 */
    private String flowNodeNameColumnName;

    /** イベントタイプカラム名 */
    private String eventTypeColumnName;

    /** マルチインスタンスタイプカラム名 */
    private String multiInstanceTypeColumnName;

    /** ゲートウェイタイプカラム名 */
    private String gatewayTypeColumnName;

    /** 境界イベントトリガーIDカラム名 */
    private String boundaryEventTriggerIdColumnName;

    /** 境界イベントトリガー名カラム名 */
    private String boundaryEventTriggerNameColumnName;

    /** 接続先タスクIDカラム名 */
    private String attachedTaskIdColumnName;

    /** シーケンスフローIDカラム名 */
    private String sequenceFlowIdColumnName;

    /** シーケンスフロー名カラム名 */
    private String sequenceFlowNameColumnName;

    /** 遷移元フローノードIDカラム名 */
    private String sourceFlowNodeIdColumnName;

    /** 遷移先フローノードIDカラム名 */
    private String targetFlowNodeIdColumnName;

    /** フロー進行条件カラム名 */
    private String flowProceedConditionColumnName;

    /** 終了条件カラム名 */
    private String completionConditionColumnName;

    // ----- accessor: table name -----

    /**
     * プロセス定義テーブル名を取得する。
     *
     * @return プロセス定義テーブル名
     */
    public String getWorkflowDefinitionTableName() {
        return workflowDefinitionTableName;
    }

    /**
     * プロセス定義テーブル名を設定する。
     *
     * @param workflowDefinitionTableName プロセス定義テーブル名
     */
    public void setWorkflowDefinitionTableName(String workflowDefinitionTableName) {
        this.workflowDefinitionTableName = workflowDefinitionTableName;
    }

    /**
     * レーンテーブル名を取得する。
     *
     * @return レーンテーブル名
     */
    public String getLaneTableName() {
        return laneTableName;
    }

    /**
     * レーンテーブル名を設定する。
     *
     * @param laneTableName レーンテーブル名
     */
    public void setLaneTableName(String laneTableName) {
        this.laneTableName = laneTableName;
    }

    /**
     * フローノード定義テーブル名を取得する。
     *
     * @return フローノード定義テーブル名
     */
    public String getFlowNodeTableName() {
        return flowNodeTableName;
    }

    /**
     * フローノード定義テーブル名を設定する。
     *
     * @param flowNodeTableName フローノード定義テーブル名
     */
    public void setFlowNodeTableName(String flowNodeTableName) {
        this.flowNodeTableName = flowNodeTableName;
    }

    /**
     * イベントテーブル名を取得する。
     *
     * @return イベントテーブル名
     */
    public String getEventTableName() {
        return eventTableName;
    }

    /**
     * イベントテーブル名を設定する。
     *
     * @param eventTableName イベントテーブル名
     */
    public void setEventTableName(String eventTableName) {
        this.eventTableName = eventTableName;
    }

    /**
     * タスクテーブル名を取得する。
     *
     * @return タスクテーブル名
     */
    public String getTaskTableName() {
        return taskTableName;
    }

    /**
     * タスクテーブル名を設定する。
     *
     * @param taskTableName タスクテーブル名
     */
    public void setTaskTableName(String taskTableName) {
        this.taskTableName = taskTableName;
    }

    /**
     * ゲートウェイテーブル名を取得する。
     *
     * @return ゲートウェイテーブル名
     */
    public String getGatewayTableName() {
        return gatewayTableName;
    }

    /**
     * ゲートウェイテーブル名を設定する。
     *
     * @param gatewayTableName ゲートウェイテーブル名
     */
    public void setGatewayTableName(String gatewayTableName) {
        this.gatewayTableName = gatewayTableName;
    }

    /**
     * 境界イベント定義テーブル名を取得する。
     *
     * @return 境界イベント定義テーブル名
     */
    public String getBoundaryEventTableName() {
        return boundaryEventTableName;
    }

    /**
     * 境界イベント定義テーブル名を設定する。
     *
     * @param boundaryEventTableName 境界イベント定義テーブル名
     */
    public void setBoundaryEventTableName(String boundaryEventTableName) {
        this.boundaryEventTableName = boundaryEventTableName;
    }

    /**
     * イベントトリガー定義テーブル名を取得する。
     *
     * @return イベントトリガー定義テーブル名
     */
    public String getEventTriggerTableName() {
        return eventTriggerTableName;
    }

    /**
     * イベントトリガー定義テーブル名を設定する。
     *
     * @param eventTriggerTableName イベントトリガー定義テーブル名
     */
    public void setEventTriggerTableName(String eventTriggerTableName) {
        this.eventTriggerTableName = eventTriggerTableName;
    }

    /**
     * シーケンスフロー定義テーブル名を取得する。
     *
     * @return シーケンスフロー定義テーブル名
     */
    public String getSequenceFlowTableName() {
        return sequenceFlowTableName;
    }

    /**
     * シーケンスフロー定義テーブル名を取得する。
     *
     * @param sequenceFlowTableName シーケンスフロー定義テーブル名
     */
    public void setSequenceFlowTableName(String sequenceFlowTableName) {
        this.sequenceFlowTableName = sequenceFlowTableName;
    }

    // ----- accessor: column name -----

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
     * プロセス名カラム名を取得する。
     *
     * @return プロセス名カラム名
     */
    public String getWorkflowNameColumnName() {
        return workflowNameColumnName;
    }

    /**
     * プロセス名カラム名を設定する。
     *
     * @param workflowNameColumnName プロセス名カラム名
     */
    public void setWorkflowNameColumnName(String workflowNameColumnName) {
        this.workflowNameColumnName = workflowNameColumnName;
    }

    /**
     * バージョンカラム名を取得する。
     *
     * @return バージョンカラム名
     */
    public String getVersionColumnName() {
        return versionColumnName;
    }

    /**
     * バージョンカラム名を設定する。
     *
     * @param versionColumnName バージョンカラム名
     */
    public void setVersionColumnName(String versionColumnName) {
        this.versionColumnName = versionColumnName;
    }

    /**
     * 適用日カラム名を取得する。
     *
     * @return 適用日カラム名
     */
    public String getEffectiveDateColumnName() {
        return effectiveDateColumnName;
    }

    /**
     * 適用日カラム名を設定する。
     *
     * @param effectiveDateColumnName 適用日カラム名
     */
    public void setEffectiveDateColumnName(String effectiveDateColumnName) {
        this.effectiveDateColumnName = effectiveDateColumnName;
    }

    /**
     * レーンIDカラム名を取得する。
     *
     * @return レーンIDカラム名
     */
    public String getLaneIdColumnName() {
        return laneIdColumnName;
    }

    /**
     * レーンIDカラム名を設定する。
     *
     * @param laneIdColumnName レーンIDカラム名
     */
    public void setLaneIdColumnName(String laneIdColumnName) {
        this.laneIdColumnName = laneIdColumnName;
    }

    /**
     * レーン名カラム名を取得する。
     *
     * @return レーン名カラム名
     */
    public String getLaneNameColumnName() {
        return laneNameColumnName;
    }

    /**
     * レーン名カラム名を設定する。
     *
     * @param laneNameColumnName レーン名カラム名
     */
    public void setLaneNameColumnName(String laneNameColumnName) {
        this.laneNameColumnName = laneNameColumnName;
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
     * @param flowNodeIdColumnName フローノードIDカラム名
     */
    public void setFlowNodeIdColumnName(String flowNodeIdColumnName) {
        this.flowNodeIdColumnName = flowNodeIdColumnName;
    }

    /**
     * フローノード名カラム名を取得する。
     *
     * @return フローノード名カラム名
     */
    public String getFlowNodeNameColumnName() {
        return flowNodeNameColumnName;
    }

    /**
     * フローノード名カラム名を設定する。
     *
     * @param flowNodeNameColumnName フローノード名カラム名
     */
    public void setFlowNodeNameColumnName(String flowNodeNameColumnName) {
        this.flowNodeNameColumnName = flowNodeNameColumnName;
    }

    /**
     * イベントタイプカラム名を取得する。
     *
     * @return イベントタイプカラム名
     */
    public String getEventTypeColumnName() {
        return eventTypeColumnName;
    }

    /**
     * イベントタイプカラム名を設定する。
     *
     * @param eventTypeColumnName イベントタイプカラム名
     */
    public void setEventTypeColumnName(String eventTypeColumnName) {
        this.eventTypeColumnName = eventTypeColumnName;
    }

    /**
     * マルチインスタンスタイプカラム名を取得する。
     *
     * @return マルチインスタンスタイプカラム名
     */
    public String getMultiInstanceTypeColumnName() {
        return multiInstanceTypeColumnName;
    }

    /**
     * マルチインスタンスタイプカラム名を設定する。
     *
     * @param multiInstanceTypeColumnName マルチインスタンスタイプカラム名
     */
    public void setMultiInstanceTypeColumnName(String multiInstanceTypeColumnName) {
        this.multiInstanceTypeColumnName = multiInstanceTypeColumnName;
    }

    /**
     * ゲートウェイタイプカラム名を取得する。
     *
     * @return ゲートウェイタイプカラム名
     */
    public String getGatewayTypeColumnName() {
        return gatewayTypeColumnName;
    }

    /**
     * ゲートウェイタイプカラム名を設定する。
     *
     * @param gatewayTypeColumnName ゲートウェイタイプカラム名
     */
    public void setGatewayTypeColumnName(String gatewayTypeColumnName) {
        this.gatewayTypeColumnName = gatewayTypeColumnName;
    }

    /**
     * 境界イベントトリガーIDカラム名を取得する。
     *
     * @return 境界イベントトリガーIDカラム名
     */
    public String getBoundaryEventTriggerIdColumnName() {
        return boundaryEventTriggerIdColumnName;
    }

    /**
     * 境界イベントトリガーIDカラム名を設定する。
     *
     * @param boundaryEventTriggerIdColumnName 境界イベントトリガーIDカラム名
     */
    public void setBoundaryEventTriggerIdColumnName(String boundaryEventTriggerIdColumnName) {
        this.boundaryEventTriggerIdColumnName = boundaryEventTriggerIdColumnName;
    }

    /**
     * 境界イベントトリガー名カラム名を取得する。
     *
     * @return 境界イベントトリガー名カラム名
     */
    public String getBoundaryEventTriggerNameColumnName() {
        return boundaryEventTriggerNameColumnName;
    }

    /**
     * 境界イベントトリガー名カラム名を設定する。
     *
     * @param boundaryEventTriggerNameColumnName 境界イベントトリガー名カラム名
     */
    public void setBoundaryEventTriggerNameColumnName(String boundaryEventTriggerNameColumnName) {
        this.boundaryEventTriggerNameColumnName = boundaryEventTriggerNameColumnName;
    }

    /**
     * 接続先タスクIDカラム名を取得する。
     *
     * @return 接続先タスクIDカラム名
     */
    public String getAttachedTaskIdColumnName() {
        return attachedTaskIdColumnName;
    }

    /**
     * 接続先タスクIDカラム名を設定する。
     *
     * @param attachedTaskIdColumnName 接続先タスクIDカラム名
     */
    public void setAttachedTaskIdColumnName(String attachedTaskIdColumnName) {
        this.attachedTaskIdColumnName = attachedTaskIdColumnName;
    }

    /**
     * シーケンスフローIDカラム名を取得する。
     *
     * @return シーケンスフローIDカラム名
     */
    public String getSequenceFlowIdColumnName() {
        return sequenceFlowIdColumnName;
    }

    /**
     * シーケンスフローIDカラム名を設定する。
     *
     * @param sequenceFlowIdColumnName シーケンスフローIDカラム名
     */
    public void setSequenceFlowIdColumnName(String sequenceFlowIdColumnName) {
        this.sequenceFlowIdColumnName = sequenceFlowIdColumnName;
    }

    /**
     * シーケンスフロー名カラム名を取得する。
     *
     * @return シーケンスフロー名カラム名
     */
    public String getSequenceFlowNameColumnName() {
        return sequenceFlowNameColumnName;
    }

    /**
     * シーケンスフロー名カラム名を設定する。
     *
     * @param sequenceFlowNameColumnName シーケンスフロー名カラム名
     */
    public void setSequenceFlowNameColumnName(String sequenceFlowNameColumnName) {
        this.sequenceFlowNameColumnName = sequenceFlowNameColumnName;
    }

    /**
     * 遷移元フローノードIDカラム名を取得する。
     *
     * @return 遷移元フローノードIDカラム名
     */
    public String getSourceFlowNodeIdColumnName() {
        return sourceFlowNodeIdColumnName;
    }

    /**
     * 遷移元フローノードIDカラム名を設定する。
     *
     * @param sourceFlowNodeIdColumnName 遷移元フローノードIDカラム名
     */
    public void setSourceFlowNodeIdColumnName(String sourceFlowNodeIdColumnName) {
        this.sourceFlowNodeIdColumnName = sourceFlowNodeIdColumnName;
    }

    /**
     * 遷移先フローノードIDカラム名を取得する。
     *
     * @return 遷移先フローノードIDカラム名
     */
    public String getTargetFlowNodeIdColumnName() {
        return targetFlowNodeIdColumnName;
    }

    /**
     * 遷移先フローノードIDカラム名を設定する。
     *
     * @param targetFlowNodeIdColumnName 遷移先フローノードIDカラム名
     */
    public void setTargetFlowNodeIdColumnName(String targetFlowNodeIdColumnName) {
        this.targetFlowNodeIdColumnName = targetFlowNodeIdColumnName;
    }

    /**
     * フロー進行条件カラム名を取得する。
     *
     * @return フロー進行条件カラム名
     */
    public String getFlowProceedConditionColumnName() {
        return flowProceedConditionColumnName;
    }

    /**
     * フロー進行条件カラム名を設定する。
     *
     * @param flowProceedConditionColumnName フロー進行条件カラム名
     */
    public void setFlowProceedConditionColumnName(String flowProceedConditionColumnName) {
        this.flowProceedConditionColumnName = flowProceedConditionColumnName;
    }

    /**
     * 終了条件カラム名を取得する。
     *
     * @return 終了条件カラム名
     */
    public String getCompletionConditionColumnName() {
        return completionConditionColumnName;
    }

    /**
     * 終了条件カラム名を設定する。
     *
     * @param completionConditionColumnName 終了条件カラム名
     */
    public void setCompletionConditionColumnName(String completionConditionColumnName) {
        this.completionConditionColumnName = completionConditionColumnName;
    }
}

