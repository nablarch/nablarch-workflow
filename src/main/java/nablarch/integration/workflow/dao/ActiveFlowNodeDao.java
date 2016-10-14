package nablarch.integration.workflow.dao;

import nablarch.core.db.statement.SqlPStatement;
import nablarch.core.db.statement.SqlResultSet;
import nablarch.core.db.statement.SqlRow;

import nablarch.integration.workflow.definition.FlowNode;

/**
 * アクティブフローノードテーブルアクセスクラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class ActiveFlowNodeDao extends DaoSupport {

    /** テーブル定義情報 */
    private final WorkflowInstanceSchema schema;

    /** INSERT文 */
    private final String insertSql;

    /** DELETE文 */
    private final String deleteSql;

    /** SELECT文 */
    private final String selectSql;

    /**
     * アクティブフローノードテーブルアクセスを生成する。
     *
     * @param schema テーブル定義情報
     */
    public ActiveFlowNodeDao(WorkflowInstanceSchema schema) {
        this.schema = schema;
        insertSql = createInsertSql();
        deleteSql = createDeleteSql();
        selectSql = createSelectSql();
    }

    /**
     * アクティブフローノードを取得する。
     *
     * @param instanceId インスタンスID
     * @return 取得したアクティブフローノード情報
     */
    public ActiveFlowNodeEntity find(String instanceId) {
        SqlPStatement statement = createStatement(selectSql);
        statement.setString(1, instanceId);

        SqlResultSet resultSet = statement.retrieve();
        if (resultSet.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("active flow node was not found. instance id = [%s]", instanceId));
        }
        SqlRow row = resultSet.get(0);
        return new ActiveFlowNodeEntity(
                row.getString(schema.getInstanceIdColumnName()),
                row.getString(schema.getFlowNodeIdColumnName())
        );
    }

    /**
     * アクティブフローノードを登録する。
     *
     * @param instanceId インスタンスID
     * @param flowNode フローノード
     */
    public void insert(String instanceId, FlowNode flowNode) {
        SqlPStatement statement = createStatement(insertSql);
        statement.setString(1, instanceId);
        statement.setString(2, flowNode.getFlowNodeId());
        statement.executeUpdate();
    }

    /**
     * アクティブフローノードを削除する。
     *
     * @param instanceId インスタンスID
     */
    public void delete(String instanceId) {
        SqlPStatement statement = createStatement(deleteSql);
        statement.setString(1, instanceId);
        statement.executeUpdate();
    }

    /**
     * SELECT文を生成する。
     *
     * @return 生成したSQL文
     */
    private String createSelectSql() {
        String templateSql = "SELECT * from #tableName# WHERE #instanceId# = ?";
        return templateSql.replaceAll("#tableName#", schema.getActiveFlowNodeTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName());
    }


    /**
     * INSERT文を生成する。
     *
     * @return 生成したSQL文
     */
    private String createInsertSql() {
        String templateSql = "INSERT INTO #tableName# ("
                + " #instanceId#,"
                + " #flowNodeId#"
                + " ) VALUES ("
                + "?, ?)";

        return templateSql.replaceAll("#tableName#", schema.getActiveFlowNodeTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName());
    }

    /**
     * アクティブなフローノードをインスタンスIDをキーに削除するSQL文を生成する。
     *
     * @return 生成したSQL文
     */
    private String createDeleteSql() {
        String templateSql = "DELETE FROM #tableName#"
                + " WHERE #instanceId# = ?";

        return templateSql.replaceAll("#tableName#", schema.getActiveFlowNodeTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName());
    }
}
