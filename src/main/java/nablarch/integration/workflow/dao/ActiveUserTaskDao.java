package nablarch.integration.workflow.dao;

import java.util.ArrayList;
import java.util.List;

import nablarch.core.db.statement.SqlPStatement;
import nablarch.core.db.statement.SqlResultSet;
import nablarch.core.db.statement.SqlRow;

/**
 * アクティブユーザタスクテーブルアクセスクラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class ActiveUserTaskDao extends DaoSupport {

    /** テーブル定義情報 */
    private final WorkflowInstanceSchema schema;

    /** SELECT文 */
    private final String selectSql;

    /** ユーザIDを条件に含めたSELECT文 */
    private final String selectByPkSql;

    /** COUNT文 */
    private final String countSql;

    /** PKでのCOUNT文 */
    private final String countByPkSql;

    /** INSERT文 */
    private final String insertSql;

    /** DELETE文 */
    private final String deleteSql;

    /** DELETE文(インスタンスID、フローノードID、ユーザを条件に) */
    private final String deleteByPkSql;

    /**
     * アクティブユーザタスクテーブルアクセスを生成する。
     *
     * @param schema テーブル定義情報
     */
    public ActiveUserTaskDao(WorkflowInstanceSchema schema) {
        this.schema = schema;
        selectSql = createSelectSql();
        selectByPkSql = createSelectByPkSql();
        countSql = createCountSql();
        countByPkSql = createCountByPkSql();
        insertSql = createInsertSql();
        deleteSql = createDeleteSql();
        deleteByPkSql = createDeleteByPkSql();
    }

    /**
     * インスタンスIDに紐づくアクティブユーザタスクを取得する。
     *
     * @param instanceId インスタンスID
     * @return 取得したアクティブユーザタスク情報
     */
    public List<ActiveUserTaskEntity> find(String instanceId) {
        SqlPStatement statement = createStatement(selectSql);
        statement.setString(1, instanceId);

        ArrayList<ActiveUserTaskEntity> result = new ArrayList<ActiveUserTaskEntity>();
        for (SqlRow row : statement.executeQuery()) {
            result.add(new ActiveUserTaskEntity(
                    row.getString(schema.getInstanceIdColumnName()),
                    row.getString(schema.getFlowNodeIdColumnName()),
                    row.getString(schema.getAssignedUserColumnName()),
                    row.getBigDecimal(schema.getExecutionOrderColumnName()).intValue()
            ));
        }
        return result;
    }

    /**
     * 主キーに紐づくデータを取得する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param user ユーザID
     * @return 取得結果(存在しない場合はnull)
     */
    public ActiveUserTaskEntity find(String instanceId, String flowNodeId, String user) {
        SqlPStatement statement = createStatement(selectByPkSql);
        statement.setString(1, instanceId);
        statement.setString(2, flowNodeId);
        statement.setString(3, user);
        SqlResultSet retrieve = statement.retrieve(1, 1);
        if (retrieve.isEmpty()) {
            return null;
        }

        SqlRow row = retrieve.get(0);
        return new ActiveUserTaskEntity(
                row.getString(schema.getInstanceIdColumnName()),
                row.getString(schema.getFlowNodeIdColumnName()),
                row.getString(schema.getAssignedUserColumnName()),
                row.getBigDecimal(schema.getExecutionOrderColumnName()).intValue()
        );
    }

    /**
     * インスタンスIDに対応するレコード数を取得する。
     *
     * @param instanceId インスタンスID
     * @return レコード数
     */
    public int count(String instanceId) {
        SqlPStatement statement = createStatement(countSql);
        statement.setString(1, instanceId);
        SqlResultSet resultSet = statement.retrieve();
        return resultSet.get(0).getBigDecimal("user_count").intValue();
    }

    /**
     * インスタンスID、フローノード、担当ユーザに対応するレコード数を取得する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノード
     * @param user 担当ユーザ
     * @return レコード数
     */
    public int countByPk(String instanceId, String flowNodeId, String user) {
        SqlPStatement statement = createStatement(countByPkSql);
        statement.setString(1, instanceId);
        statement.setString(2, flowNodeId);
        statement.setString(3, user);
        SqlResultSet resultSet = statement.retrieve();
        return resultSet.get(0).getBigDecimal("user_count").intValue();
    }

    /**
     * アクティブユーザタスクを登録する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノード
     * @param users 担当ユーザ
     */
    public void insert(String instanceId, String flowNodeId, List<String> users) {

        SqlPStatement statement = createStatement(insertSql);
        statement.setString(1, instanceId);
        statement.setString(2, flowNodeId);
        statement.setInt(4, 0);
        for (String user : users) {
            statement.setString(3, user);
            statement.addBatch();
        }
        statement.executeBatch();
    }

    /**
     * アクティブユーザタスクを登録する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param user ユーザ
     * @param executionOrder 実行順
     */
    public void insert(String instanceId, String flowNodeId, String user, int executionOrder) {
        SqlPStatement statement = createStatement(insertSql);
        statement.setString(1, instanceId);
        statement.setString(2, flowNodeId);
        statement.setString(3, user);
        statement.setInt(4, executionOrder);
        statement.executeUpdate();
    }

    /**
     * アクティブユーザタスクを削除する。
     *
     * @param instanceId インスタンスID
     */
    public void delete(String instanceId) {
        SqlPStatement statement = createStatement(deleteSql);
        statement.setString(1, instanceId);
        statement.executeUpdate();
    }

    /**
     * アクティブユーザタスクを削除する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param user ユーザ
     */
    public void delete(String instanceId, String flowNodeId, String user) {
        SqlPStatement statement = createStatement(deleteByPkSql);
        statement.setString(1, instanceId);
        statement.setString(2, flowNodeId);
        statement.setString(3, user);
        statement.executeUpdate();
    }

    /**
     * SELECT文を生成する。
     *
     * @return 生成したSQL文
     */
    private String createSelectSql() {
        String templateSql = "SELECT * FROM #tableName# WHERE #instanceId# = ? ORDER BY #userId#, #executionOrder#";
        return templateSql.replaceAll("#tableName#", schema.getActiveUserTaskTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#userId#", schema.getAssignedUserColumnName())
                .replaceAll("#executionOrder#", schema.getExecutionOrderColumnName());
    }

    /**
     * ユーザIDを条件に含めたSELECT文を生成する。
     *
     * @return 生成したSQL文
     */
    private String createSelectByPkSql() {
        String templateSql = "SELECT * FROM #tableName# "
                + "WHERE #instanceId# = ?"
                + " and #flowNodeId# = ?"
                + " AND #userId# = ?";
        return templateSql.replaceAll("#tableName#", schema.getActiveUserTaskTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName())
                .replaceAll("#userId#", schema.getAssignedUserColumnName());
    }

    /**
     * COUNT文を生成する。
     *
     * @return 生成したSQL文
     */
    private String createCountSql() {
        String templateSql = "SELECT count(*) user_count FROM #tableName# WHERE #instanceId# = ?";
        return templateSql.replaceAll("#tableName#", schema.getActiveUserTaskTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName());
    }

    /**
     * PKでのCOUNT文を生成する。
     *
     * @return 生成したSQL文
     */
    private String createCountByPkSql() {
        String templateSql = "SELECT count(*) user_count FROM #tableName# WHERE #instanceId# = ? AND #flowNodeId# = ? AND #userId# = ?";
        return templateSql.replaceAll("#tableName#", schema.getActiveUserTaskTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName())
                .replaceAll("#userId#", schema.getAssignedUserColumnName());
    }

    /**
     * INSERT文を生成する。
     *
     * @return 生成したSQL文
     */
    private String createInsertSql() {
        String templateSql = "INSERT INTO #tableName# ("
                + " #instanceId#,"
                + " #flowNodeId#,"
                + " #assignedUserId#,"
                + " #executionOrder#"
                + " ) VALUES ("
                + " ?, ?, ?, ?)";

        return templateSql.replaceAll("#tableName#", schema.getActiveUserTaskTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName())
                .replaceAll("#assignedUserId#", schema.getAssignedUserColumnName())
                .replaceAll("#executionOrder#", schema.getExecutionOrderColumnName());
    }

    /**
     * DELETE文を生成する。
     *
     * @return 生成したSQL文
     */
    private String createDeleteSql() {
        String templateSql = "DELETE FROM #tableName#"
                + " WHERE #instanceId# = ?";
        return templateSql.replaceAll("#tableName#", schema.getActiveUserTaskTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName());
    }

    /**
     * delete文(インスタンスID、フローノードID、ユーザが条件)を生成する。
     *
     * @return 生成したSQL文
     */
    private String createDeleteByPkSql() {
        String templateSql = "DELETE FROM #tableName#"
                + " WHERE #instanceId# = ?"
                + " AND #flowNodeId# = ?"
                + " AND #assignedUserId# = ?";
        return templateSql.replaceAll("#tableName#", schema.getActiveUserTaskTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName())
                .replaceAll("#assignedUserId#", schema.getAssignedUserColumnName());
    }

}

