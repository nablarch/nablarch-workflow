package nablarch.integration.workflow.definition.loader;

import java.util.ArrayList;
import java.util.List;

import nablarch.core.db.connection.AppDbConnection;
import nablarch.core.db.statement.ResultSetIterator;
import nablarch.core.db.statement.SqlPStatement;
import nablarch.core.db.statement.SqlRow;
import nablarch.core.db.transaction.SimpleDbTransactionExecutor;
import nablarch.core.db.transaction.SimpleDbTransactionManager;
import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;

import nablarch.integration.workflow.definition.BoundaryEvent;
import nablarch.integration.workflow.definition.Event;
import nablarch.integration.workflow.definition.Gateway;
import nablarch.integration.workflow.definition.Lane;
import nablarch.integration.workflow.definition.SequenceFlow;
import nablarch.integration.workflow.definition.Task;
import nablarch.integration.workflow.util.WorkflowUtil;
import nablarch.integration.workflow.definition.WorkflowDefinition;

/**
 * データベースからワークフローの定義情報をロードするクラス。
 * <p/>
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class DatabaseWorkflowDefinitionLoader
        implements WorkflowDefinitionLoader {

    /** ロガー */
    private static final Logger LOG = LoggerManager.get(DatabaseWorkflowDefinitionLoader.class);

    /** 初期化済みか否か */
    private boolean isInitialized;

    /** ワークフロー定義テーブル群の定義情報 */
    private WorkflowDefinitionSchema workflowDefinitionSchema;

    /** データベーストランザクション */
    private SimpleDbTransactionManager transactionManager;

    /** ワークフローテーブルから全情報を取得するためのSQL文 */
    private String findAllWorkflowSql;

    /** レーン情報を取得するためのSQL文 */
    private String findLaneSql;

    /** イベントノード情報を取得するためのSQL文 */
    private String findEventSql;

    /** タスク情報を取得するためのSQL文 */
    private String findTaskSql;

    /** シーケンスフロー情報を取得するためのSQL文 */
    private String findSequenceFlowSql;

    /** ゲートウェイ情報を取得するためのSQL文 */
    private String findGatewaySql;

    /** イベント定義情報を取得するためのSQL文 */
    private String findBoundaryEventSql;

    /**
     * ワークフロー定義をロードする。
     *
     * @return ワークフロー定義
     */
    @Override
    public List<WorkflowDefinition> load() {
        if (!isInitialized) {
            // 初期化が行われていない場合には、強制的に初期化処理を実施する。
            initialize();
            isInitialized = true;
        }

        return new SimpleDbTransactionExecutor<List<WorkflowDefinition>>(transactionManager) {
            @Override
            public List<WorkflowDefinition> execute(AppDbConnection connection) {
                List<WorkflowDefinition> result = new ArrayList<WorkflowDefinition>();
                for (SqlRow workflow : findAllWorkflowDefinition(connection)) {

                    String workflowId = workflow.getString(
                            workflowDefinitionSchema.getWorkflowIdColumnName());
                    int version = workflow.getBigDecimal(
                            workflowDefinitionSchema.getVersionColumnName()).intValue();

                    WorkflowDefinition workflowDefinition = new WorkflowDefinition(
                            workflowId,
                            version,
                            workflow.getString(workflowDefinitionSchema.getWorkflowNameColumnName()),
                            workflow.getString(workflowDefinitionSchema.getEffectiveDateColumnName())
                    );

                    workflowDefinition.setLanes(
                            findLane(connection, workflowId, version));
                    List<SequenceFlow> sequenceFlows = findSequenceFlow(connection, workflowId, version);
                    workflowDefinition.setSequenceFlows(sequenceFlows);
                    workflowDefinition.setEvents(
                            findEvent(connection, workflowId, version, sequenceFlows));
                    workflowDefinition.setTasks(
                            findTask(connection, workflowId, version, sequenceFlows));
                    workflowDefinition.setGateways(
                            findGateway(connection, workflowId, version, sequenceFlows));
                    workflowDefinition.setBoundaryEvents(
                            findBoundaryEvent(connection, workflowId, version, sequenceFlows)
                    );

                    LOG.logInfo(String.format(
                            "load workflow definition. workflowId = [%s], workflowName = [%s], version = [%d], effectiveDate = [%S]",
                            workflowId, workflowDefinition.getWorkflowName(), version,
                            workflowDefinition.getEffectiveDate()));
                    result.add(workflowDefinition);
                }
                return result;
            }
        }
                .doTransaction();
    }

    /**
     * ワークフロー定義情報を全て取得する。
     *
     * @param connection データベース接続
     * @return 取得したワークフロー定義情報
     */
    protected ResultSetIterator findAllWorkflowDefinition(AppDbConnection connection) {
        SqlPStatement statement = connection.prepareStatement(findAllWorkflowSql);
        return statement.executeQuery();
    }

    /**
     * ワークフローに紐づくレーン情報を取得する。
     *
     * @param connection データベース接続
     * @param workflowId ワークフローID
     * @param version バージョン番号
     * @return レーン定義情報
     */
    protected List<Lane> findLane(AppDbConnection connection, String workflowId, long version) {

        SqlPStatement statement = connection.prepareStatement(findLaneSql);
        statement.setString(1, workflowId);
        statement.setLong(2, version);

        List<Lane> result = new ArrayList<Lane>();
        for (SqlRow row : statement.executeQuery()) {
            result.add(new Lane(
                    row.getString(workflowDefinitionSchema.getLaneIdColumnName()),
                    row.getString(workflowDefinitionSchema.getLaneNameColumnName())
            ));
        }
        return result;
    }


    /**
     * ワークフローに紐づくイベント情報を取得する。
     *
     * @param connection データベース接続
     * @param workflowId ワークフローID
     * @param version バージョン番号
     * @param sequenceFlows シーケンスフロー定義リスト
     * @return イベント情報
     */
    protected List<Event> findEvent(
            AppDbConnection connection, String workflowId, long version, List<SequenceFlow> sequenceFlows) {

        SqlPStatement statement = connection.prepareStatement(findEventSql);
        statement.setString(1, workflowId);
        statement.setLong(2, version);

        List<Event> result = new ArrayList<Event>();
        for (SqlRow row : statement.executeQuery()) {
            String flowNodeId = row.getString(workflowDefinitionSchema.getFlowNodeIdColumnName());
            List<SequenceFlow> connectionFlow = WorkflowUtil.filterList(
                    sequenceFlows, new SequenceFlowListFilter(flowNodeId));
            result.add(new Event(
                            flowNodeId,
                            row.getString(workflowDefinitionSchema.getFlowNodeNameColumnName()),
                            row.getString(workflowDefinitionSchema.getLaneIdColumnName()),
                            row.getString(workflowDefinitionSchema.getEventTypeColumnName()),
                            connectionFlow
                    )
            );
        }
        return result;
    }

    /**
     * ワークフローに紐づくタスク定義を取得する。
     *
     * @param connection データベース接続
     * @param workflowId ワークフローID
     * @param version バージョン
     * @param sequenceFlows シーケンスフロー定義リスト
     * @return タスク定義
     */
    protected List<Task> findTask(
            AppDbConnection connection, String workflowId, long version, List<SequenceFlow> sequenceFlows) {

        SqlPStatement statement = connection.prepareStatement(findTaskSql);
        statement.setString(1, workflowId);
        statement.setLong(2, version);
        ResultSetIterator resultSetIterator = statement.executeQuery();

        List<Task> result = new ArrayList<Task>();
        for (SqlRow row : resultSetIterator) {
            String flowNodeId = row.getString(workflowDefinitionSchema.getFlowNodeIdColumnName());
            List<SequenceFlow> connectionFlow = WorkflowUtil.filterList(
                    sequenceFlows, new SequenceFlowListFilter(flowNodeId));
            result.add(new Task(
                    flowNodeId,
                    row.getString(workflowDefinitionSchema.getFlowNodeNameColumnName()),
                    row.getString(workflowDefinitionSchema.getLaneIdColumnName()),
                    row.getString(workflowDefinitionSchema.getMultiInstanceTypeColumnName()),
                    row.getString(workflowDefinitionSchema.getCompletionConditionColumnName()),
                    connectionFlow));
        }
        return result;
    }

    /**
     * ワークフローに紐づくゲートウェイ情報を取得する。
     *
     * @param connection データベース接続
     * @param workflowId ワークフローID
     * @param version バージョン
     * @param sequenceFlows シーケンスフロー定義リスト
     * @return ゲートウェイ情報
     */
    protected List<Gateway> findGateway(
            AppDbConnection connection, String workflowId, long version, List<SequenceFlow> sequenceFlows) {

        SqlPStatement statement = connection.prepareStatement(findGatewaySql);
        statement.setString(1, workflowId);
        statement.setLong(2, version);

        List<Gateway> result = new ArrayList<Gateway>();
        for (SqlRow row : statement.executeQuery()) {
            String flowNodeId = row.getString(workflowDefinitionSchema.getFlowNodeIdColumnName());
            List<SequenceFlow> connectionFlow = WorkflowUtil.filterList(
                    sequenceFlows, new SequenceFlowListFilter(flowNodeId));
            result.add(new Gateway(
                            flowNodeId,
                            row.getString(workflowDefinitionSchema.getFlowNodeNameColumnName()),
                            row.getString(workflowDefinitionSchema.getLaneIdColumnName()),
                            row.getString(workflowDefinitionSchema.getGatewayTypeColumnName()),
                            connectionFlow)
            );
        }
        return result;
    }

    /**
     * ワークフローに紐づくシーケンスフロー情報を取得する。
     *
     * @param connection データベース接続
     * @param workflowId ワークフローID
     * @param version バージョン番号
     * @return 取得したシーケンスフロー情報
     */
    protected List<SequenceFlow> findSequenceFlow(
            AppDbConnection connection, String workflowId, long version) {

        SqlPStatement statement = connection.prepareStatement(findSequenceFlowSql);
        statement.setString(1, workflowId);
        statement.setLong(2, version);
        ResultSetIterator resultSetIterator = statement.executeQuery();

        List<SequenceFlow> result = new ArrayList<SequenceFlow>();
        for (SqlRow row : resultSetIterator) {
            result.add(
                    new SequenceFlow(
                            row.getString(workflowDefinitionSchema.getSequenceFlowIdColumnName()),
                            row.getString(workflowDefinitionSchema.getSequenceFlowNameColumnName()),
                            row.getString(workflowDefinitionSchema.getSourceFlowNodeIdColumnName()),
                            row.getString(workflowDefinitionSchema.getTargetFlowNodeIdColumnName()),
                            row.getString(workflowDefinitionSchema.getFlowProceedConditionColumnName())
                    )
            );
        }
        return result;
    }

    /**
     * ワークフローに紐づく境界イベント情報を取得する。
     *
     * @param connection データベース接続
     * @param workflowId ワークフローID
     * @param version バージョン番号
     * @param sequenceFlows シーケンスフロー定義リスト
     * @return 取得した境界イベント情報
     */
    protected  List<BoundaryEvent> findBoundaryEvent(
            AppDbConnection connection, String workflowId, long version, List<SequenceFlow> sequenceFlows) {
        SqlPStatement statement = connection.prepareStatement(findBoundaryEventSql);
        statement.setString(1, workflowId);
        statement.setLong(2, version);

        List<BoundaryEvent> result = new ArrayList<BoundaryEvent>();
        for (SqlRow row : statement.executeQuery()) {
            String flowNodeId = row.getString(workflowDefinitionSchema.getFlowNodeIdColumnName());
            List<SequenceFlow> connectionFlow = WorkflowUtil.filterList(
                    sequenceFlows, new SequenceFlowListFilter(flowNodeId));
            result.add(new BoundaryEvent(
                            flowNodeId,
                            row.getString(workflowDefinitionSchema.getFlowNodeNameColumnName()),
                            row.getString(workflowDefinitionSchema.getLaneIdColumnName()),
                            row.getString(workflowDefinitionSchema.getBoundaryEventTriggerIdColumnName()),
                            row.getString(workflowDefinitionSchema.getBoundaryEventTriggerNameColumnName()),
                            row.getString(workflowDefinitionSchema.getAttachedTaskIdColumnName()),
                            connectionFlow)
            );
        }
        return result;
    }

    /**
     * 初期化処理を行う。
     * <p/>
     * 各テーブルからデータをロードするローダの初期化処理を実行する。
     */
    private void initialize() {
        isInitialized = true;
        findAllWorkflowSql = createFindAllWorkflowSql(workflowDefinitionSchema);
        findLaneSql = createFindLaneSql(workflowDefinitionSchema);
        findEventSql = createFindEventSql(workflowDefinitionSchema);
        findTaskSql = createFindTaskSql(workflowDefinitionSchema);
        findGatewaySql = createFindGatewaySql(workflowDefinitionSchema);

        findSequenceFlowSql = createFindSequenceFlowSql(workflowDefinitionSchema);
        findBoundaryEventSql = createFindBoundaryEventSql(workflowDefinitionSchema);
    }

    /**
     * ワークフロー定義を全レコードを取得するSQL文を生成する。
     * <p/>
     *
     * @param schema ワークフロー定義テーブル情報
     * @return 生成したSQL文
     */
    protected String createFindAllWorkflowSql(WorkflowDefinitionSchema schema) {
        String sqlTemplate = "SELECT "
                + " #workflowId#,"
                + " #version#,"
                + " #workflowName#,"
                + " #effectiveDate#"
                + " FROM #workflow#"
                + " ORDER BY #workflowId#, #version#";

        return sqlTemplate.replaceAll("#workflowId#", schema.getWorkflowIdColumnName())
                .replaceAll("#version#", schema.getVersionColumnName())
                .replaceAll("#workflowName#", schema.getWorkflowNameColumnName())
                .replaceAll("#effectiveDate#", schema.getEffectiveDateColumnName())
                .replaceAll("#workflow#", schema.getWorkflowDefinitionTableName());
    }

    /**
     * レーン情報を取得するためのSQL文を生成する。
     *
     * @param schema ワークフロー定義テーブル情報
     * @return 生成したSQL文
     */
    protected String createFindLaneSql(WorkflowDefinitionSchema schema) {
        String templateSql = "SELECT "
                + " #laneId#,"
                + " #laneName#"
                + " FROM #lane#"
                + " WHERE #workflowId# = ?"
                + " AND #version# = ?"
                + " ORDER BY #laneId#";
        return templateSql.replaceAll("#laneId#", schema.getLaneIdColumnName())
                .replaceAll("#laneName#", schema.getLaneNameColumnName())
                .replaceAll("#lane#", schema.getLaneTableName())
                .replaceAll("#workflowId#", schema.getWorkflowIdColumnName())
                .replaceAll("#version#", schema.getVersionColumnName());
    }

    /**
     * イベントノード情報とイベントノードに関連する情報を取得するSQLを生成する。
     * <p/>
     * イベントノード情報に関連した、以下の情報も取得する。
     * <ul>
     * <li>フローノード定義</li>
     * </ul>
     *
     * @param schema ワークフロー定義テーブル情報
     * @return 生成したSQL文
     */
    protected String createFindEventSql(WorkflowDefinitionSchema schema) {
        String templateSql = "SELECT"
                + " flow.#flowNodeId#,"
                + " flow.#flowNodeName#,"
                + " flow.#laneId#,"
                + " event.#eventNodeType#"
                + " FROM #flowNode# flow"
                + " INNER JOIN #event# event"
                + " ON flow.#workflowId# = event.#workflowId#"
                + " AND flow.#version# = event.#version#"
                + " AND flow.#flowNodeId# = event.#flowNodeId#"
                + " WHERE flow.#workflowId# = ?"
                + " AND flow.#version# = ?"
                + " ORDER BY #flowNodeId#";

        return templateSql.replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName())
                .replaceAll("#flowNodeName#", schema.getFlowNodeNameColumnName())
                .replaceAll("#laneId#", schema.getLaneIdColumnName())
                .replaceAll("#eventNodeType#", schema.getEventTypeColumnName())
                .replaceAll("#workflowId#", schema.getWorkflowIdColumnName())
                .replaceAll("#version#", schema.getVersionColumnName())
                .replaceAll("#flowNode#", schema.getFlowNodeTableName())
                .replaceAll("#event#", schema.getEventTableName());
    }

    /**
     * アクティビティ情報とアクティビティに関連する情報を取得するSQLを生成する。
     * <p/>
     * アクティビティ情報に関連した、以下の情報も取得する。
     * <ul>
     * <li>フローノード定義</li>
     * </ul>
     *
     * @param schema ワークフロー定義テーブル情報
     * @return 生成したSQL文
     */
    protected String createFindTaskSql(WorkflowDefinitionSchema schema) {
        String templateSql = "SELECT"
                + " flow.#flowNodeId#,"
                + " flow.#flowNodeName#,"
                + " flow.#laneId#,"
                + " task.#multiInstanceType#,"
                + " task.#completionCondition#"
                + " FROM #flowNode# flow"
                + " INNER JOIN #task# task"
                + " ON flow.#workflowId# = task.#workflowId#"
                + " AND flow.#version# = task.#version#"
                + " AND flow.#flowNodeId# = task.#flowNodeId#"
                + " WHERE flow.#workflowId# = ?"
                + " AND flow.#version# = ?"
                + " ORDER BY #flowNodeId#";

        return templateSql.replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName())
                .replaceAll("#flowNodeName#", schema.getFlowNodeNameColumnName())
                .replaceAll("#laneId#", schema.getLaneIdColumnName())
                .replaceAll("#multiInstanceType#", schema.getMultiInstanceTypeColumnName())
                .replaceAll("#completionCondition#", schema.getCompletionConditionColumnName())
                .replaceAll("#workflowId#", schema.getWorkflowIdColumnName())
                .replaceAll("#version#", schema.getVersionColumnName())
                .replaceAll("#flowNode#", schema.getFlowNodeTableName())
                .replaceAll("#task#", schema.getTaskTableName());
    }

    /**
     * ゲートウェイとゲートウェイに関連する情報を取得するSQLを生成する。
     * <p/>
     * ゲートウェイに関連した、以下の情報も取得する。
     * <ul>
     * <li>フローノード定義</li>
     * </ul>
     *
     * @param schema ワークフロー定義テーブル情報
     * @return 生成したSQL文
     */
    protected String createFindGatewaySql(WorkflowDefinitionSchema schema) {
        String templateSql = "SELECT"
                + " flow.#flowNodeId#,"
                + " flow.#flowNodeName#,"
                + " flow.#laneId#,"
                + " gateway.#gatewayType#"
                + " FROM #flowNode# flow"
                + " INNER JOIN #gateway# gateway"
                + " ON flow.#workflowId# = gateway.#workflowId#"
                + " AND flow.#version# = gateway.#version#"
                + " AND flow.#flowNodeId# = gateway.#flowNodeId#"
                + " WHERE flow.#workflowId# = ?"
                + " AND flow.#version# = ?"
                + " ORDER BY #flowNodeId#";

        return templateSql.replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName())
                .replaceAll("#flowNodeName#", schema.getFlowNodeNameColumnName())
                .replaceAll("#laneId#", schema.getLaneIdColumnName())
                .replaceAll("#gatewayType#", schema.getGatewayTypeColumnName())
                .replaceAll("#workflowId#", schema.getWorkflowIdColumnName())
                .replaceAll("#version#", schema.getVersionColumnName())
                .replaceAll("#flowNode#", schema.getFlowNodeTableName())
                .replaceAll("#gateway#", schema.getGatewayTableName());
    }

    /**
     * シーケンスフロー情報を取得するためのSQL文を生成する。
     *
     * @param schema ワークフロー定義テーブル情報
     * @return 生成したSQL文
     */
    protected String createFindSequenceFlowSql(WorkflowDefinitionSchema schema) {
        String templateSql = "SELECT "
                + " #sequenceFlowId#,"
                + " #sourceFlowNodeId#,"
                + " #targetFlowNodeId#,"
                + " #sequenceFlowName#,"
                + " #flowCondition#"
                + " FROM #sequenceFlow#"
                + " WHERE #workflowId# = ?"
                + " AND #version# = ?"
                + " ORDER BY #sequenceFlowId#";
        return templateSql.replaceAll("#sequenceFlowId#", schema.getSequenceFlowIdColumnName())
                .replaceAll("#sourceFlowNodeId#", schema.getSourceFlowNodeIdColumnName())
                .replaceAll("#targetFlowNodeId#", schema.getTargetFlowNodeIdColumnName())
                .replaceAll("#sequenceFlowName#", schema.getSequenceFlowNameColumnName())
                .replaceAll("#sequenceFlow#", schema.getSequenceFlowTableName())
                .replaceAll("#workflowId#", schema.getWorkflowIdColumnName())
                .replaceAll("#version#", schema.getVersionColumnName())
                .replaceAll("#flowCondition#", schema.getFlowProceedConditionColumnName());
    }

    /**
     * イベント定義取得用のSQL文を生成する。
     *
     * @param schema ワークフロー定義テーブル情報
     * @return 生成したSQL文
     */
    protected String createFindBoundaryEventSql(WorkflowDefinitionSchema schema) {
        String templateSql = "SELECT "
                + " eventTrigger.#triggerId#,"
                + " eventTrigger.#triggerName#,"
                + " flow.#flowNodeId#,"
                + " flow.#flowNodeName#,"
                + " flow.#laneId#,"
                + " #attachedTaskId#"
                + " FROM #trigger# eventTrigger"
                + " INNER JOIN #boundary# boundary"
                + " ON eventTrigger.#workflowId# = boundary.#workflowId#"
                + " AND eventTrigger.#version# = boundary.#version#"
                + " AND eventTrigger.#triggerId# = boundary.#triggerId#"
                + " INNER JOIN #flowNode# flow"
                + " ON flow.#workflowId# = boundary.#workflowId#"
                + " AND flow.#version# = boundary.#version#"
                + " AND flow.#flowNodeId# = boundary.#flowNodeId#"
                + " WHERE eventTrigger.#workflowId# = ?"
                + " AND eventTrigger.#version# = ?"
                + " ORDER BY #triggerId#";

        return templateSql.replaceAll("#workflowId#", schema.getWorkflowIdColumnName())
                .replaceAll("#version#", schema.getVersionColumnName())
                .replaceAll("#triggerId#", schema.getBoundaryEventTriggerIdColumnName())
                .replaceAll("#triggerName#", schema.getBoundaryEventTriggerNameColumnName())
                .replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName())
                .replaceAll("#flowNodeName#", schema.getFlowNodeNameColumnName())
                .replaceAll("#laneId#", schema.getLaneIdColumnName())
                .replaceAll("#attachedTaskId#", schema.getAttachedTaskIdColumnName())
                .replaceAll("#trigger#", schema.getEventTriggerTableName())
                .replaceAll("#boundary#", schema.getBoundaryEventTableName())
                .replaceAll("#flowNode#", schema.getFlowNodeTableName());
    }

    /**
     * ワークフロー定義テーブルの定義情報を設定する。
     *
     * @param workflowDefinitionSchema ワークフロー定義テーブルの定義情報
     */
    public void setWorkflowDefinitionSchema(WorkflowDefinitionSchema workflowDefinitionSchema) {
        this.workflowDefinitionSchema = workflowDefinitionSchema;
    }

    /**
     * データベース接続を設定する。
     *
     * @param transactionManager データベース接続
     */
    public void setTransactionManager(SimpleDbTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * シーケンスフロー定義リストから遷移元フローノードIDが一致するシーケンスフローをフィルターするクラス。
     */
    private static final class SequenceFlowListFilter implements WorkflowUtil.ListFilter<SequenceFlow> {

        /** フィルター条件のフローノードID */
        private final String flowNodeId;

        /**
         * フローノードIDを条件にフィルタ条件を生成する。
         *
         * @param flowNodeId フローノードID
         */
        private SequenceFlowListFilter(String flowNodeId) {
            this.flowNodeId = flowNodeId;
        }

        @Override
        public boolean isMatch(SequenceFlow other) {
            return flowNodeId.equals(other.getSourceFlowNodeId());
        }
    }
}

