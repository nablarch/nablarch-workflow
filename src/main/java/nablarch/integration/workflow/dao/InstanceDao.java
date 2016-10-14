package nablarch.integration.workflow.dao;

import nablarch.core.db.statement.SqlPStatement;
import nablarch.core.db.statement.SqlResultSet;

/**
 * ワークフローインスタンステーブルへアクセスするクラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class InstanceDao extends DaoSupport {

    /** テーブル定義 */
    private final WorkflowInstanceSchema schema;

    /** FIND文 */
    private final String selectSql;

    /** INSERT文 */
    private final String insertSql;

    /** DELETE文 */
    private final String deleteSql;

    /**
     * ワークフローインスタンステーブルアクセスを生成する。
     *
     * @param schema テーブル定義情報
     */
    public InstanceDao(WorkflowInstanceSchema schema) {
        this.schema = schema;
        insertSql = createInsertSql();
        deleteSql = createDeleteSql();
        selectSql = createSelectSql();
    }

    /**
     * インスタンス情報を検索する。
     *
     * @param instanceId インスタンスID
     * @return インスタンス情報
     */
    public WorkflowInstanceEntity find(String instanceId) {
        SqlPStatement statement = createStatement(selectSql);
        statement.setString(1, instanceId);
        SqlResultSet result = statement.retrieve();

        if (result.isEmpty()) {
            return null;
        }

        return new WorkflowInstanceEntity(
                result.get(0).getString(schema.getInstanceIdColumnName()),
                result.get(0).getString(schema.getWorkflowIdColumnName()),
                result.get(0).getBigDecimal(schema.getVersionColumnName()).longValue()
        );
    }

    /**
     * ワークフローインスタンス情報を登録する。
     *
     * @param instanceId インスタンスID
     * @param workflowId ワークフローID
     * @param version バージョン番号
     */
    public void insert(String instanceId, String workflowId, int version) {
        SqlPStatement statement = createStatement(insertSql);
        statement.setString(1, instanceId);
        statement.setString(2, workflowId);
        statement.setInt(3, version);
        statement.executeUpdate();
    }

    /**
     * ワークフローインスタンスを削除する。
     *
     * @param instanceId インスタンスID
     */
    public void delete(String instanceId) {
        SqlPStatement statement = createStatement(deleteSql);
        statement.setString(1, instanceId);
        statement.executeUpdate();
    }

    /**
     * FIND用SQL文を生成する。
     *
     * @return 生成したSQL文
     */
    private String createSelectSql() {
        String templateSql = "SELECT * FROM #tableName# WHERE #instanceId# = ?";
        return templateSql.replaceAll("#tableName#", schema.getInstanceTableName())
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
                + " #workflowId#,"
                + " #version#"
                + " ) VALUES ("
                + " ?, ?, ?)";
        return templateSql.replaceAll("#tableName#", schema.getInstanceTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#workflowId#", schema.getWorkflowIdColumnName())
                .replaceAll("#version#", schema.getVersionColumnName());
    }

    /**
     * DELETE文を生成する。
     *
     * @return 生成したSQL文
     */
    private String createDeleteSql() {
        String templateSql = "DELETE FROM #tableName#"
                + " WHERE #instanceId# = ?";

        return templateSql.replaceAll("#tableName#", schema.getInstanceTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName());
    }
}
