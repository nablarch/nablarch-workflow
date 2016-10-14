package nablarch.integration.workflow.dao;

import java.util.List;

import nablarch.core.db.statement.SqlPStatement;

import nablarch.integration.workflow.definition.FlowNode;
import nablarch.integration.workflow.definition.Task;

/**
 * ワークフローインスタンスフローノードテーブルへアクセスするクラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class InstanceFlowNodeDao extends DaoSupport {

    /** テーブル定義情報 */
    private final WorkflowInstanceSchema schema;

    /** INSERT文 */
    private final String insertSql;

    /** DELETE文 */
    private final String deleteSql;

    /**
     * ワークフローインスタンスフローノードテーブルアクセスを生成する。
     *
     * @param schema テーブル定義情報
     */
    public InstanceFlowNodeDao(WorkflowInstanceSchema schema) {
        this.schema = schema;
        insertSql = createInsertSql();
        deleteSql = createDeleteSql();
    }

    /**
     * フローノードの情報を登録する。
     *
     * @param instanceId インスタンスID
     * @param workflowId ワークフローID
     * @param version バージョン
     * @param tasks タスクリスト
     */
    public void insert(String instanceId, String workflowId, long version, List<Task> tasks) {
        SqlPStatement statement = createStatement(insertSql);

        statement.setString(1, instanceId);
        statement.setString(2, workflowId);
        statement.setLong(3, version);
        for (FlowNode flowNode : tasks) {
            statement.setString(4, flowNode.getFlowNodeId());
            statement.addBatch();
        }
        statement.executeBatch();
    }

    /**
     * フローノードの情報を削除する。
     *
     * @param instanceId インスタンスID
     */
    public void delete(String instanceId) {
        SqlPStatement statement = createStatement(deleteSql);
        statement.setString(1, instanceId);
        statement.executeUpdate();
    }

    /**
     * INSERT文を生成する。
     *
     * @return 生成したSQL文
     */
    private String createInsertSql() {
        String templateSql = "INSERT INTO #tableName# ("
                + " #instanceId#,"
                + " #workflowId#,"
                + " #version#,"
                + " #flowNodeId#"
                + " ) VALUES ("
                + " ?, ?, ?, ?)";
        return templateSql.replaceAll("#tableName#", schema.getInstanceFlowNodeTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#workflowId#", schema.getWorkflowIdColumnName())
                .replaceAll("#version#", schema.getVersionColumnName())
                .replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName());
    }

    /**
     * DELETE文を生成する。
     *
     * @return 生成したSQL文
     */
    private String createDeleteSql() {
        String templateSql = "delete from #tableName#"
                + " where #instanceId# = ?";

        return templateSql.replaceAll("#tableName#", schema.getInstanceFlowNodeTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName());
    }
}
