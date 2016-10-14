package nablarch.integration.workflow;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import nablarch.core.db.statement.SqlRow;

import nablarch.integration.workflow.definition.FlowNode;
import nablarch.integration.workflow.definition.Task;
import nablarch.integration.workflow.testhelper.WorkflowTestRule;
import nablarch.integration.workflow.util.WorkflowUtil;
import nablarch.integration.workflow.dao.WorkflowInstanceDao;
import nablarch.integration.workflow.definition.WorkflowDefinition;
import nablarch.integration.workflow.testhelper.WorkflowDbAccessSupport;

/**
 * ワークフロー関連のテストサポートクラス。
 *
 * @author Ryo Tanaka
 * @since 1.4.2
 */
public final class WorkflowTestSupport {

    /** データベースアクセス */
    private static WorkflowDbAccessSupport db = new WorkflowTestRule().getWorkflowDao();

    /**
     * ワークフローインスタンスが正しく生成されているかどうかを検証する。
     *
     * @param message アサート用メッセージ
     * @param workflow 検証対象ワークフローインスタンス
     */
    public static void assertWorkflowInstance(String message, WorkflowInstance workflow) {
        assertThat(message + "：ワークフローインスタンスが生成されること。", workflow, not(nullValue()));
        assertThat(message + "：ワークフローインスタンスのインスタンスIDが採番されていること。", workflow.getInstanceId(), not(nullValue()));

        boolean isPersisted = WorkflowUtil.contains(db.findWorkflowInstance(), new WorkflowInstanceFilter(workflow));
        assertThat(message + "：ワークフローインスタンスの情報がデータベースに登録されていること。", isPersisted, is(true));
    }

    /**
     * ワークフローのアクティブフローノードが期待通りになっているかどうかを検証する。
     *
     * @param message アサート用メッセージ
     * @param workflow 検証対象ワークフローインスタンス
     * @param expected 期待するアクティブフローノード
     */
    public static void assertActiveFlowNode(String message, WorkflowInstance workflow, String expected) {
        assertThat(message + "：アクティブフローノード", workflow.isActive(expected), is(true));

        boolean isPersisted = WorkflowUtil.contains(db.findActiveFlowNode(), new WorkflowInstanceFlowNodeFilter(workflow, expected));
        assertThat(message + "：アクティブフローノードがデータベースに登録されていること。", isPersisted, is(true));
    }

    /**
     * ワークフローのアクティブユーザタスク、アクティブグループタスクが期待通りになっているかどうかを検証する。
     *
     * @param message アサート用メッセージ
     * @param workflow 検証対象ワークフローインスタンス
     * @param expectedUsers アクティブユーザタスクに登録されていることを期待するユーザ
     * @param expectedGroups アクティブグループタスクに登録されていることを期待するグループ
     */
    public static void assertCurrentTasks(String message, WorkflowInstance workflow, List<String> expectedUsers, List<String> expectedGroups) {
        List<SqlRow> userTasks = WorkflowUtil.filterList(db.findActiveUserTask(), new WorkflowInstanceFilter(workflow));
        Collection<String> actualUserTasks = new ArrayList<String>(userTasks.size());
        for (SqlRow row : userTasks) {
            actualUserTasks.add(row.getString("ASSIGNED_USER_ID"));
        }

        List<SqlRow> groupTask = WorkflowUtil.filterList(db.findActiveGroupTask(), new WorkflowInstanceFilter(workflow));
        Collection<String> actualGroupTask = new ArrayList<String>(groupTask.size());
        for (SqlRow row : groupTask) {
            actualGroupTask.add(row.getString("ASSIGNED_GROUP_ID"));
        }

        // アクティブユーザタスクで順番を意識してアサートする必要はない。（並列の場合は順不同。順次の場合は一つだけアクティブのはずだから。）
        assertThat(message + "：担当ユーザ", actualUserTasks, containsInAnyOrder(expectedUsers.toArray()));
        assertThat(message + "：担当グループ", actualGroupTask, containsInAnyOrder(expectedGroups.toArray()));
    }

    /**
     * ワークフローのタスクにアサインされているユーザ、グループが期待通りになっているかどうかを検証する。
     *
     * @param message アサート用メッセージ
     * @param workflow 検証対象ワークフローインスタンス
     * @param taskId 検証対象タスクのフローノードID
     * @param expectedUsers タスク担当ユーザに登録されていることを期待するユーザ
     * @param expectedGroups タスク担当グループに登録されていることを期待するグループ
     */
    public static void assertAssignment(String message, WorkflowInstance workflow, String taskId, List<String> expectedUsers,
            List<String> expectedGroups) {
        List<SqlRow> userTasks = WorkflowUtil.filterList(db.findAssignedUser(), new WorkflowInstanceFlowNodeFilter(workflow, taskId));
        Collections.sort(userTasks, new ExecutionOrderComparator());
        List<String> actualUserTasks = new ArrayList<String>(userTasks.size());
        for (SqlRow row : userTasks) {
            actualUserTasks.add(row.getString("ASSIGNED_USER_ID"));
        }

        List<SqlRow> groupTask = WorkflowUtil.filterList(db.findAssignedGroup(), new WorkflowInstanceFlowNodeFilter(workflow, taskId));
        Collections.sort(groupTask, new ExecutionOrderComparator());
        List<String> actualGroupTask = new ArrayList<String>(groupTask.size());
        for (SqlRow row : groupTask) {
            actualGroupTask.add(row.getString("ASSIGNED_GROUP_ID"));
        }

        // タスクが順次タスク以外の場合は、順不同でアサートする必要がある。
        WorkflowDefinition definition =
                WorkflowConfig.get().getWorkflowDefinitionHolder().getWorkflowDefinition(workflow.getWorkflowId(), workflow.getVersion());
        Task task = definition.findTask(taskId);
        if (task.isSequentialType()) {
            assertThat(message + "：担当ユーザ", actualUserTasks, is(expectedUsers));
            assertThat(message + "：担当グループ", actualGroupTask, is(expectedGroups));
        } else {
            assertThat(message + "：担当ユーザ", actualUserTasks, containsInAnyOrder(expectedUsers.toArray()));
            assertThat(message + "：担当グループ", actualGroupTask, containsInAnyOrder(expectedGroups.toArray()));
        }
    }

    /**
     * 指定されたフローノードをアクティブとして、ワークフローインスタンスを生成する。データベースのアクティブフローノード情報は生成しない。
     *
     * @param dummyInstanceId ダミーのインスタンスID
     * @param workflowId ワークフロー定義のワークフローID
     * @param activeNodeId アクティブフローノードとするフローノードのID
     * @return ワークフローインスタンス
     */
    public static WorkflowInstance prepareWorkflowWithoutDb(String dummyInstanceId, String workflowId, String activeNodeId) {
        WorkflowDefinition definition = WorkflowConfig.get().getWorkflowDefinitionHolder().getWorkflowDefinition(workflowId);
        return new BasicWorkflowInstance(dummyInstanceId, definition, definition.findFlowNode(activeNodeId));
    }

    /**
     * データベースのワークフローインスタンス情報と、アクティブフローノード情報も含めて、指定されたフローノードをアクティブとして、ワークフローインスタンスを生成する。
     *
     * @param workflowId ワークフロー定義のワークフローID
     * @param activeNodeId アクティブフローノードとするフローノードのID
     * @return ワークフローインスタンス
     */
    public static WorkflowInstance prepareWorkflowWithDb(String workflowId, String activeNodeId) {
        WorkflowDefinition definition = WorkflowConfig.get().getWorkflowDefinitionHolder().getWorkflowDefinition(workflowId);
        FlowNode activeNode = definition.findFlowNode(activeNodeId);
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        String instanceId = dao.createWorkflowInstance(definition.getWorkflowId(), definition.getVersion(), definition.getTasks());
        dao.saveActiveFlowNode(instanceId, activeNode);
        return new BasicWorkflowInstance(instanceId, definition, activeNode);
    }

    /**
     * ワークフローインスタンス系テーブルへのアクセスクラスを取得する。
     *
     * @return ワークフローインスタンスDAO
     */
    public static WorkflowInstanceDao getWorkflowInstanceDao() {
        return WorkflowConfig.get().getWorkflowInstanceDao();
    }

    /**
     * ワークフローインスタンスIDで {@link SqlRow} のフィルタを行う {@link WorkflowUtil.ListFilter}
     */
    public static class WorkflowInstanceFilter implements WorkflowUtil.ListFilter<SqlRow> {

        /**
         * フィルタ対象ワークフローIDを持つ、ワークフローインスタンス
         */
        private final WorkflowInstance workflow;

        /**
         * @param workflow フィルタするワークフローインスタンス
         */
        public WorkflowInstanceFilter(WorkflowInstance workflow) {
            this.workflow = workflow;
        }

        /**
         * @param other 比較対象
         * @return フィルタ用ワークフローとインスタンスIDが一致する場合 {@link true}
         */
        @Override
        public boolean isMatch(SqlRow other) {
            return workflow.getInstanceId().equals(other.getString("INSTANCE_ID"));
        }
    }

    /**
     * ワークフローインスタンスIDおよびフローノードIDで {@link SqlRow} のフィルタを行う {@link WorkflowUtil.ListFilter}
     */
    public static class WorkflowInstanceFlowNodeFilter implements WorkflowUtil.ListFilter<SqlRow> {

        /**
         * ワークフローインスタンスIDでフィルタを行う {@link WorkflowInstanceFilter}
         */
        private final WorkflowInstanceFilter instanceFilter;

        /**
         * フィルタ対象フローノードID
         */
        private final String flowNodeId;

        /**
         * @param workflow フィルタするワークフローインスタンス
         * @param flowNodeId フィルタするフローノードID
         */
        public WorkflowInstanceFlowNodeFilter(WorkflowInstance workflow, String flowNodeId) {
            this.instanceFilter = new WorkflowInstanceFilter(workflow);
            this.flowNodeId = flowNodeId;
        }

        /**
         * @param other 比較対象
         * @return インスタンスIDおよびフローノードIDが一致する場合 {@link true}
         */
        @Override
        public boolean isMatch(SqlRow other) {
            return instanceFilter.isMatch(other) && flowNodeId.equals(other.get("FLOW_NODE_ID"));
        }
    }

    /**
     * 実行順で {@link SqlRow} の比較を行う {@link Comparator} 実装クラス。
     */
    private static class ExecutionOrderComparator implements Comparator<SqlRow> {

        @Override
        public int compare(SqlRow o1, SqlRow o2) {
            return o1.getBigDecimal("EXECUTION_ORDER").intValue() - o2.getBigDecimal("EXECUTION_ORDER").intValue();
        }
    }

    /**
     * 隠蔽コンストラクタ
     */
    private WorkflowTestSupport() {
    }
}
