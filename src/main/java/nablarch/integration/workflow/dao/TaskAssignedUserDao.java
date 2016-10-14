package nablarch.integration.workflow.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nablarch.core.db.statement.SqlPStatement;
import nablarch.core.db.statement.SqlResultSet;
import nablarch.core.db.statement.SqlRow;

/**
 * タスク担当ユーザテーブルアクセスクラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class TaskAssignedUserDao extends DaoSupport {

    /** テーブル定義情報 */
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
    private final String deleteByInstanceIdAndFlowNodeId;

    /** delete文(主キーが条件) */
    private final String deleteByPk;


    /**
     * タスク担当ユーザテーブルアクセスを生成する。
     *
     * @param schema テーブル定義情報
     */
    public TaskAssignedUserDao(WorkflowInstanceSchema schema) {
        this.schema = schema;
        selectSql = createSelectSql();
        selectByPkSql = createSelectByPkSql();
        countSql = createCountSql();
        insertSql = createInsertSql();
        deleteSql = createDeleteSql();
        deleteByInstanceIdAndFlowNodeId = createDeleteByInstanceIdAndFlowNodeIdSql();
        deleteByPk = createDeleteByPkSql();
    }

    /**
     * 担当ユーザを取得する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @return 取得した担当ユーザ情報
     */
    public List<TaskAssignedUserEntity> find(String instanceId, String flowNodeId) {
        SqlPStatement statement = createStatement(selectSql);
        statement.setString(1, instanceId);
        statement.setString(2, flowNodeId);

        List<TaskAssignedUserEntity> result = new ArrayList<TaskAssignedUserEntity>();
        for (SqlRow row : statement.executeQuery()) {
            result.add(new TaskAssignedUserEntity(
                    row.getString(schema.getInstanceIdColumnName()),
                    row.getString(schema.getFlowNodeIdColumnName()),
                    row.getString(schema.getAssignedUserColumnName()),
                    row.getBigDecimal(schema.getExecutionOrderColumnName()).intValue()
            ));
        }
        return result;
    }

    /**
     * 担当ユーザを取得する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param user ユーザ
     * @return 取得したユーザ情報(存在しない場合はnull)
     */
    public TaskAssignedUserEntity find(String instanceId, String flowNodeId, String user) {
        SqlPStatement statement = createStatement(selectByPkSql);
        statement.setString(1, instanceId);
        statement.setString(2, flowNodeId);
        statement.setString(3, user);
        SqlResultSet retrieve = statement.retrieve();
        if (retrieve.isEmpty()) {
            return null;
        }
        SqlRow row = retrieve.get(0);
        return new TaskAssignedUserEntity(
                row.getString(schema.getInstanceIdColumnName()),
                row.getString(schema.getFlowNodeIdColumnName()),
                row.getString(schema.getAssignedUserColumnName()),
                row.getBigDecimal(schema.getExecutionOrderColumnName()).intValue()
        );
    }

    /**
     * タスク担当ユーザ数を取得する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @return タスク担当ユーザ数
     */
    public int count(String instanceId, String flowNodeId) {
        SqlPStatement statement = createStatement(countSql);
        statement.setString(1, instanceId);
        statement.setString(2, flowNodeId);
        SqlResultSet resultSet = statement.retrieve();
        return resultSet.get(0).getBigDecimal("task_user_count").intValue();
    }

    /**
     * 担当ユーザを登録する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param users 担当ユーザリスト
     */
    public void insert(String instanceId, String flowNodeId, Collection<String> users) {
        SqlPStatement statement = createStatement(insertSql);
        statement.setString(1, instanceId);
        statement.setString(2, flowNodeId);
        for (String user : users) {
            statement.setString(3, user);
            statement.setInt(4, 0);
            statement.addBatch();
        }
        statement.executeBatch();
    }

    /**
     * 担当ユーザを指定された順を実行順として保存する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param users 担当ユーザリスト(格納順が実行順となる)
     */
    public void insertSequential(String instanceId, String flowNodeId, List<String> users) {
        SqlPStatement statement = createStatement(insertSql);
        statement.setString(1, instanceId);
        statement.setString(2, flowNodeId);
        int executionOrder = 0;
        for (String user : users) {
            statement.setString(3, user);
            executionOrder++;
            statement.setInt(4, executionOrder);
            statement.addBatch();
        }
        statement.executeBatch();
    }

    /**
     * 担当ユーザ情報を登録する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードId
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
     * 担当ユーザを削除する。
     *
     * @param instanceId インスタンスID
     */
    public void delete(String instanceId) {
        SqlPStatement statement = createStatement(deleteSql);
        statement.setString(1, instanceId);
        statement.executeUpdate();
    }

    /**
     * 担当ユーザを削除する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     */
    public void delete(String instanceId, String flowNodeId) {
        SqlPStatement statement = createStatement(deleteByInstanceIdAndFlowNodeId);
        statement.setString(1, instanceId);
        statement.setString(2, flowNodeId);
        statement.executeUpdate();
    }

    /**
     * 担当ユーザを削除する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param user ユーザ
     */
    public void delete(String instanceId, String flowNodeId, String user) {
        SqlPStatement statement = createStatement(deleteByPk);
        statement.setString(1, instanceId);
        statement.setString(2, flowNodeId);
        statement.setString(3, user);
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
                + " #flowNodeId#,"
                + " #assignedUser#,"
                + " #executionOrder#"
                + " ) VALUES ("
                + " ?, ?, ?, ?)";

        return templateSql.replaceAll("#tableName#", schema.getAssignedUserTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName())
                .replaceAll("#assignedUser#", schema.getAssignedUserColumnName())
                .replaceAll("#executionOrder#", schema.getExecutionOrderColumnName());

    }

    /**
     * DELETE文を生成する。
     *
     * @return 生成したDELETE文
     */
    private String createDeleteSql() {
        String templateSql = "DELETE FROM #tableName#"
                + " WHERE #instanceId# = ?";

        return templateSql.replaceAll("#tableName#", schema.getAssignedUserTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName());
    }

    /**
     * 割り当て担当者からインスタンスIDとフローノードIDを条件にデータを削除するSQL文を生成する。
     *
     * @return 生成したSQL文
     */
    private String createDeleteByInstanceIdAndFlowNodeIdSql() {
        String templateSql = "DELETE FROM #tableName#"
                + " WHERE #instanceId# = ?"
                + " AND #flowNodeId# = ?";
        return templateSql.replaceAll("#tableName#", schema.getAssignedUserTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName());
    }

    /**
     * 主キーを条件にデータを削除するDELETE文を生成する。
     *
     * @return 生成したSQL文
     */
    private String createDeleteByPkSql() {
        String templateSql = "DELETE FROM #tableName#"
                + " WHERE #instanceId# = ?"
                + " AND #flowNodeId# = ?"
                + " and #assignedUser# = ?";
        return templateSql.replaceAll("#tableName#", schema.getAssignedUserTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName())
                .replaceAll("#assignedUser#", schema.getAssignedUserColumnName());
    }

    /**
     * インスタンスIDとフローノードIDを条件に担当者を検索するSQL文を生成する。
     *
     * @return 生成したSQL文
     */
    private String createSelectSql() {
        String templateSql = "SELECT * FROM #tableName# WHERE #instanceId# = ? AND #flowNodeId# = ? ORDER BY #executionOrder#";

        return templateSql.replaceAll("#tableName#", schema.getAssignedUserTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName())
                .replaceAll("#executionOrder#", schema.getExecutionOrderColumnName());
    }

    /**
     * 主キーを条件に担当者を検索するSQL文を生成する。
     *
     * @return 生成したSQL文
     */
    private String createSelectByPkSql() {
        String templateSql = "SELECT * FROM #tableName# "
                + "WHERE #instanceId# = ? "
                + " AND #flowNodeId# = ?"
                + " AND #assignedUser# = ?";

        return templateSql.replaceAll("#tableName#", schema.getAssignedUserTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName())
                .replaceAll("#assignedUser#", schema.getAssignedUserColumnName());
    }


    /**
     * COUNT文を生成する。
     *
     * @return 生成したCOUNT文
     */
    private String createCountSql() {
        String templateSql = "SELECT COUNT(*) task_user_count FROM #tableName# WHERE #instanceId# = ? AND #flowNodeId# = ? ";

        return templateSql.replaceAll("#tableName#", schema.getAssignedUserTableName())
                .replaceAll("#instanceId#", schema.getInstanceIdColumnName())
                .replaceAll("#flowNodeId#", schema.getFlowNodeIdColumnName());
    }

}


