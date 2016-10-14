package nablarch.integration.workflow.dao;

import java.util.ArrayList;
import java.util.List;

import nablarch.core.db.statement.SqlPStatement;
import nablarch.core.db.statement.SqlResultSet;
import nablarch.core.db.statement.SqlRow;

/**
 * アクティブグループタスクテーブルアクセスクラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class ActiveGroupTaskDao extends DaoSupport {

    /** SELECT文 */
    private final String selectSql;

    /** グループIDを条件に検索を行うSELECT文 */
    private final String selectByPkSql;

    /** INSERT文 */
    private final String insertSql;

    /** DELETE文 */
    private final String deleteSql;

    /** 主キーを条件にレコードを削除するDELETE文。 */
    private final String deleteByPk;

    /** テーブル定義情報 */
    private final WorkflowInstanceSchema schema;

    /** レコード数取得count文 */
    private final String countSql;

    /** PKでのCOUNT文 */
    private final String countByPkSql;

    /**
     * アクティブグループタスクテーブルアクセスを生成する。
     *
     * @param schema テーブル定義情報
     */
    public ActiveGroupTaskDao(WorkflowInstanceSchema schema) {
        this.schema = schema;
        selectSql = createSelectSql();
        selectByPkSql = createSelectByPkSql();
        countSql = createCountSql();
        insertSql = createInsertSql();
        deleteSql = createDeleteSql();
        deleteByPk = createDeleteByPkSql();
        countByPkSql = createCountByPkSql();
    }

    /**
     * インスタンスIDに紐づくアクティブグループタスクを取得する。
     *
     * @param instanceId インスタンスID
     * @return アクティブグループタスク情報
     */
    public List<ActiveGroupTaskEntity> find(String instanceId) {
        SqlPStatement statement = createStatement(selectSql);
        statement.setString(1, instanceId);

        List<ActiveGroupTaskEntity> result = new ArrayList<ActiveGroupTaskEntity>();
        for (SqlRow row : statement.executeQuery()) {
            result.add(new ActiveGroupTaskEntity(
                    row.getString(schema.getInstanceIdColumnName()),
                    row.getString(schema.getFlowNodeIdColumnName()),
                    row.getString(schema.getAssignedGroupColumnName()),
                    row.getBigDecimal(schema.getExecutionOrderColumnName()).intValue()));
        }
        return result;
    }

    /**
     * 主キーを条件にアクティブグループタスクを取得する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param group グループ
     * @return アクティブグループタスク情報（条件に紐づくレコードが存在しない場合はnull)
     */
    public ActiveGroupTaskEntity find(String instanceId, String flowNodeId, String group) {
        SqlPStatement statement = createStatement(selectByPkSql);
        statement.setString(1, instanceId);
        statement.setString(2, flowNodeId);
        statement.setString(3, group);
        SqlResultSet retrieve = statement.retrieve();
        if (retrieve.isEmpty()) {
            return null;
        }
        SqlRow row = retrieve.get(0);
        return new ActiveGroupTaskEntity(
                row.getString(schema.getInstanceIdColumnName()),
                row.getString(schema.getFlowNodeIdColumnName()),
                row.getString(schema.getAssignedGroupColumnName()),
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
        SqlResultSet retrieve = statement.retrieve();
        return retrieve.get(0).getBigDecimal("group_count").intValue();
    }

    /**
     * インスタンスID、フローノード、担当グループに対応するレコード数を取得する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノード
     * @param group 担当グループ
     * @return レコード数
     */
    public int countByPk(String instanceId, String flowNodeId, String group) {
        SqlPStatement statement = createStatement(countByPkSql);
        statement.setString(1, instanceId);
        statement.setString(2, flowNodeId);
        statement.setString(3, group);
        SqlResultSet resultSet = statement.retrieve();
        return resultSet.get(0).getBigDecimal("group_count").intValue();
    }

    /**
     * アクティブグループタスクを登録する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param groups 担当グループ
     */
    public void insert(String instanceId, String flowNodeId, List<String> groups) {
        SqlPStatement statement = createStatement(insertSql);
        statement.setString(1, instanceId);
        statement.setString(2, flowNodeId);
        statement.setInt(4, 0);
        for (String group : groups) {
            statement.setString(3, group);
            statement.addBatch();
        }
        statement.executeBatch();
    }

    /**
     * アクティブグループタスクを登録する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param group グループ
     * @param executionOrder 実行順
     */
    public void insert(String instanceId, String flowNodeId, String group, int executionOrder) {
        SqlPStatement statement = createStatement(insertSql);
        statement.setString(1, instanceId);
        statement.setString(2, flowNodeId);
        statement.setString(3, group);
        statement.setInt(4, executionOrder);
        statement.executeUpdate();
    }

    /**
     * アクティブグループタスクを削除する。
     *
     * @param instanceId インスタンスID
     */
    public void delete(String instanceId) {
        SqlPStatement statement = createStatement(deleteSql);
        statement.setString(1, instanceId);
        statement.executeUpdate();
    }


    /**
     * アクティブグループタスクから引数で指定された条件に紐づくレコードを削除する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param groupId グループID
     */
    public void delete(String instanceId, String flowNodeId, String groupId) {
        SqlPStatement statement = createStatement(deleteByPk);
        statement.setString(1, instanceId);
        statement.setString(2, flowNodeId);
        statement.setString(3, groupId);
        statement.executeUpdate();
    }

    /**
     * SELECT文を生成する。
     *
     * @return 生成したSELECT文
     */
    private String createSelectSql() {
        String templateSql = "SELECT * FROM #tableName# WHERE #instanceId# = ?"
                + " ORDER BY #groupId#, #executionOrder#";
        return templateSql.replaceAll("#tableName#", schema.getActiveGroupTaskTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#groupId#", schema.getAssignedGroupColumnName())
                .replaceAll("#executionOrder#", schema.getExecutionOrderColumnName());
    }

    /**
     * グループIDが条件となるselect文を生成する。
     *
     * @return 生成したSELECT文
     */
    private String createSelectByPkSql() {
        String templateSql = "SELECT * FROM #tableName# WHERE #instanceId# = ? AND #flowNodeId# = ? AND #groupId# = ?";
        return templateSql.replaceAll("#tableName#", schema.getActiveGroupTaskTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName())
                .replaceAll("#groupId#", schema.getAssignedGroupColumnName());
    }

    /**
     * インスタンスIDに紐づくレコード数を取得するcount文を生成する。
     * @return 生成したSQL文
     */
    private String createCountSql() {
        String templateSql = "SELECT COUNT(*) group_count FROM #tableName# WHERE #instanceId# = ?";
        return templateSql.replaceAll("#tableName#", schema.getActiveGroupTaskTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName());
    }

    /**
     * PKでのCOUNT文を生成する。
     *
     * @return 生成したSQL文
     */
    private String createCountByPkSql() {
        String templateSql = "SELECT count(*) group_count FROM #tableName# WHERE #instanceId# = ? AND #flowNodeId# = ? AND #groupId# = ?";
        return templateSql.replaceAll("#tableName#", schema.getActiveGroupTaskTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName())
                .replaceAll("#groupId#", schema.getAssignedGroupColumnName());
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
                + " #assignedGroupId#,"
                + " #executionOrder#"
                + " )VALUES("
                + "?, ?, ?, ?)";
        return templateSql.replaceAll("#tableName#", schema.getActiveGroupTaskTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName())
                .replaceAll("#assignedGroupId#", schema.getAssignedGroupColumnName())
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

        return templateSql.replaceAll("#tableName#", schema.getActiveGroupTaskTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName());
    }

    /**
     * 主キーを条件にアクティブグループタスクを削除するSQL文を生成する。
     *
     * @return 生成したSQL文
     */
    private String createDeleteByPkSql() {
        String templateSql = "DELETE FROM #tableName#"
                + " WHERE #instanceId# = ?"
                + " AND #flowNodeId# = ?"
                + " AND #groupId# = ?";

        return templateSql.replaceAll("#tableName#", schema.getActiveGroupTaskTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName())
                .replaceAll("#groupId#", schema.getAssignedGroupColumnName());
    }
}
