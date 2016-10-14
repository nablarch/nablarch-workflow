package nablarch.integration.workflow.dao;

import java.util.ArrayList;
import java.util.List;

import nablarch.core.db.statement.SqlPStatement;
import nablarch.core.db.statement.SqlResultSet;
import nablarch.core.db.statement.SqlRow;

/**
 * タスク担当グループテーブルアクセスクラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class TaskAssignedGroupDao extends DaoSupport {

    /** ワークフローインスタンステーブル定義情報 */
    private final WorkflowInstanceSchema schema;

    /** SELECT文 */
    private final String selectSql;

    /** SELECT文(主キーが条件) */
    private final String selectByPkSql;

    /** COUNT文 */
    private final String countSql;

    /** INSERT文 */
    private final String insertSql;

    /** DELETE文 */
    private final String deleteSql;

    /** DELETE文(インスタンスIDとフローノードIDが条件) */
    private final String deleteByInstanceIdAndFlowNodeIdSql;

    /** DELETE文(主キーが条件) */
    private final String deleteByPkSql;

    /**
     * タスク担当グループテーブルアクセスを生成する。
     *
     * @param schema ワークフローインスタンステーブル定義情報
     */
    public TaskAssignedGroupDao(WorkflowInstanceSchema schema) {
        this.schema = schema;

        selectSql = createSelectSql();
        selectByPkSql = createSelectByPkSql();
        countSql = createCountSql();
        insertSql = createInsertSql();
        deleteSql = createDeleteSql();
        deleteByInstanceIdAndFlowNodeIdSql = createDeleteAssignedGroupByInstanceIdAndFlowNodeIdSql();
        deleteByPkSql = createDeleteByPkSql();
    }

    /**
     * インスタンスIDとフローノードIDに紐づく担当グループ情報を取得する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @return 担当グループ情報
     */
    public List<TaskAssignedGroupEntity> find(String instanceId, String flowNodeId) {
        SqlPStatement statement = createStatement(selectSql);
        statement.setString(1, instanceId);
        statement.setString(2, flowNodeId);

        List<TaskAssignedGroupEntity> result = new ArrayList<TaskAssignedGroupEntity>();
        for (SqlRow row : statement.executeQuery()) {
            result.add(createTaskAssignedGroupEntity(row));
        }
        return result;
    }

    /**
     * 主キーに紐づく担当グループ情報を取得する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param group グループ
     * @return 担当グループ情報(存在しない場合はnull)
     */
    public TaskAssignedGroupEntity find(String instanceId, String flowNodeId, String group) {
        SqlPStatement statement = createStatement(selectByPkSql);
        statement.setString(1, instanceId);
        statement.setString(2, flowNodeId);
        statement.setString(3, group);
        SqlResultSet retrieve = statement.retrieve();
        if (retrieve.isEmpty()) {
            return null;
        }
        SqlRow row = retrieve.get(0);
        return createTaskAssignedGroupEntity(row);
    }

    /**
     * 検索結果からタスク担当グループエンティティを生成する。
     *
     * @param row 検索結果の1行を保持するオブジェクト
     * @return 生成したエンティティ
     */
    private TaskAssignedGroupEntity createTaskAssignedGroupEntity(SqlRow row) {
        return new TaskAssignedGroupEntity(
                row.getString(schema.getInstanceIdColumnName()),
                row.getString(schema.getFlowNodeIdColumnName()),
                row.getString(schema.getAssignedGroupColumnName()),
                row.getBigDecimal(schema.getExecutionOrderColumnName()).intValue()
        );
    }

    /**
     * インスタンスID、フローノードIDに紐づくレコード数を取得する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @return 条件に紐づくレコード数
     */
    public int count(String instanceId, String flowNodeId) {
        SqlPStatement statement = createStatement(countSql);
        statement.setString(1, instanceId);
        statement.setString(2, flowNodeId);
        SqlResultSet retrieve = statement.retrieve();
        return retrieve.get(0).getBigDecimal("group_count").intValue();
    }

    /**
     * 担当グループを登録する。
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
     * 担当グループを指定された順を実行順として保存する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param groups 担当グループ
     */
    public void insertSequential(String instanceId, String flowNodeId, List<String> groups) {
        SqlPStatement statement = createStatement(insertSql);
        statement.setString(1, instanceId);
        statement.setString(2, flowNodeId);

        int executionOrder = 0;
        for (String group : groups) {
            executionOrder++;
            statement.setString(3, group);
            statement.setInt(4, executionOrder);
            statement.addBatch();
        }
        statement.executeBatch();
    }

    /**
     * 担当グループを登録する。
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param group 担当グループ
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
     * 担当グループを削除する。
     *
     * @param instanceId インスタンスID
     */
    public void delete(String instanceId) {
        SqlPStatement statement = createStatement(deleteSql);
        statement.setString(1, instanceId);
        statement.executeUpdate();
    }

    /**
     * 担当グループを削除する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     */
    public void delete(String instanceId, String flowNodeId) {
        SqlPStatement statement = createStatement(deleteByInstanceIdAndFlowNodeIdSql);
        statement.setString(1, instanceId);
        statement.setString(2, flowNodeId);
        statement.executeUpdate();
    }

    /**
     * 担当グループを削除する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param group グループ
     */
    public void delete(String instanceId, String flowNodeId, String group) {
        SqlPStatement statement = createStatement(deleteByPkSql);
        statement.setString(1, instanceId);
        statement.setString(2, flowNodeId);
        statement.setString(3, group);
        statement.executeUpdate();
    }

    /**
     * SELECT文を生成する。
     *
     * @return 生成したSQL文
     */
    private String createSelectSql() {
        String templateSql = "SELECT * from #tableName# WHERE #instanceId# = ? AND #flowNodeId# = ? ORDER BY #executionOrder#";
        return templateSql.replaceAll("#tableName#", schema.getAssignedGroupTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName())
                .replaceAll("#executionOrder#", schema.getExecutionOrderColumnName());
    }

    /**
     * 主キーを条件に検索するSELECT文を生成する。
     *
     * @return 生成したSQL文
     */
    private String createSelectByPkSql() {
        String templateSql = "SELECT * from #tableName#"
                + " WHERE #instanceId# = ?"
                + " AND #flowNodeId# = ?"
                + " AND #assignedGroup# = ?";
        return templateSql.replaceAll("#tableName#", schema.getAssignedGroupTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName())
                .replaceAll("#assignedGroup#", schema.getAssignedGroupColumnName());
    }

    /**
     * COUNT文を生成する。
     *
     * @return 生成したSQL文
     */
    private String createCountSql() {
        String templateSql = "SELECT COUNT(*) group_count from #tableName# WHERE #instanceId# = ? AND #flowNodeId# = ?";
        return templateSql.replaceAll("#tableName#", schema.getAssignedGroupTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName());
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
                + " #assignedGroup#,"
                + " #executionOrder#"
                + " ) VALUES ("
                + " ?, ?, ?, ?)";

        return templateSql.replaceAll("#tableName#", schema.getAssignedGroupTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName())
                .replaceAll("#assignedGroup#", schema.getAssignedGroupColumnName())
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
        return templateSql.replaceAll("#tableName#", schema.getAssignedGroupTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName());
    }

    /**
     * インスタンスIDとフローノードIDを条件にデータを削除するSQL文を生成する。
     *
     * @return 生成したSQL文
     */
    private String createDeleteAssignedGroupByInstanceIdAndFlowNodeIdSql() {
        String templateSql = "DELETE FROM #tableName#"
                + " WHERE #instanceId# = ?"
                + " AND #flowNodeId# = ?";
        return templateSql.replaceAll("#tableName#", schema.getAssignedGroupTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName());
    }

    /**
     * 主キーを条件にデータを削除するSQL文を生成する。
     *
     * @return 生成したSQL文
     */
    private String createDeleteByPkSql() {
        String templateSql = "DELETE FROM #tableName#"
                + " WHERE #instanceId# = ?"
                + " AND #flowNodeId# = ?"
                + " AND #assignedGroup# = ?";
        return templateSql.replaceAll("#tableName#", schema.getAssignedGroupTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName())
                .replaceAll("#assignedGroup#", schema.getAssignedGroupColumnName());
    }

}

