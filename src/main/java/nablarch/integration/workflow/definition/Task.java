package nablarch.integration.workflow.definition;

import static nablarch.integration.workflow.util.WorkflowUtil.createInstance;
import static nablarch.integration.workflow.util.WorkflowUtil.find;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nablarch.integration.workflow.condition.CompletionCondition;
import nablarch.integration.workflow.condition.SingleTaskCompletionCondition;
import nablarch.integration.workflow.dao.ActiveGroupTaskEntity;
import nablarch.integration.workflow.dao.ActiveUserTaskEntity;
import nablarch.integration.workflow.dao.TaskAssignedGroupEntity;
import nablarch.integration.workflow.dao.TaskAssignedUserEntity;
import nablarch.integration.workflow.dao.WorkflowInstanceDao;
import nablarch.integration.workflow.util.WorkflowUtil.ListFilter;

/**
 * タスク定義を表すクラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class Task extends FlowNode {

    /** 非マルチインスタンス用完了条件 */
    private static final SingleTaskCompletionCondition SINGLE_TASK_COMPLETION_CONDITION = new SingleTaskCompletionCondition();

    /**
     * マルチインスタンスタイプを表す列挙型。
     */
    public enum MultiInstanceType {
        /** 非マルチインスタンス */
        NONE,
        /** 順次実行型マルチインインスタンス */
        SEQUENTIAL,
        /** 並行実行型マルチインスタンス */
        PARALLEL
    }

    /** マルチインスタンスタイプ */
    private final MultiInstanceType multiInstanceType;

    /** 完了条件 */
    private final CompletionCondition completionCondition;

    /**
     * タスク定義を生成する。
     *
     * @param flowNodeId フローノードID
     * @param flowNodeName フローノード名
     * @param laneId レーンID
     * @param multiInstanceType マルチインスタンスタイプ
     * @param completionCondition 終了条件の型名
     * @param sequenceFlows 自身をソースとするシーケンスフロー
     */
    public Task(
            String flowNodeId,
            String flowNodeName,
            String laneId,
            String multiInstanceType,
            String completionCondition, List<SequenceFlow> sequenceFlows) {
        super(flowNodeId, flowNodeName, laneId, sequenceFlows);
        this.multiInstanceType = MultiInstanceType.valueOf(multiInstanceType);
        this.completionCondition = createInstance(completionCondition);
        validate();
    }

    /**
     * タスク定義の状態チェックを行う。
     * <p/>
     * 精査内容:<br/>
     * <ul>
     * <li>シングルタスク(非マルチインスタンスタスク)の場合、終了条件は設定されていないこと</li>
     * </ul>
     */
    private void validate() {
        if (!isMultiInstanceType() && (completionCondition != null)) {
            throw new IllegalArgumentException(String.format(
                    "Single Task must be CompletionCondition is empty.flow_node_id = [%s], flow_node_name = [%s]",
                    getFlowNodeId(), getFlowNodeName()));
        }
    }

    /**
     * マルチインスタンスタイプを取得する。
     *
     * @return マルチインスタンスタイプ
     */
    public MultiInstanceType getMultiInstanceType() {
        return multiInstanceType;
    }

    /**
     * 終了条件を取得する。
     *
     * @return 終了条件
     */
    private CompletionCondition getCompletionCondition() {
        if (isMultiInstanceType()) {
            return completionCondition;
        } else {
            return SINGLE_TASK_COMPLETION_CONDITION;
        }
    }

    /**
     * マルチインスタンスタスクか否か。
     *
     * @return マルチインスタンスタスクの場合はtrue
     */
    public boolean isMultiInstanceType() {
        return multiInstanceType != MultiInstanceType.NONE;
    }

    /**
     * マルチインスタンスタスクでシーケンシャルタイプか否か
     *
     * @return マルチインスタンスタスクで、シーケンシャルタイプの場合はtrue
     */
    public boolean isSequentialType() {
        return multiInstanceType == MultiInstanceType.SEQUENTIAL;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * このタスクをアクティブフローノードに登録し、タスク担当ユーザ/グループから、アクティブユーザタスク/アクティブグループタスクを作成する。
     * マルチインスタンスタスクで、シーケンシャルタイプの場合は、タスク担当ユーザ/グループのうち実行順が先頭のユーザ/グループのアクティブタスクを作成し、
     * それ以外の場合は、すべてのユーザ/グループのアクティブタスクを作成する。
     *
     * @param instanceId アクティブ化処理を行う対象のワークフローインスタンスID
     * @param parameter アクティブ化時に使用するパラメータ
     */
    @Override
    public void activate(String instanceId, Map<String, ?> parameter) {
        super.activate(instanceId, parameter);

        // このノードをアクティブフローノードに登録する。
        getWorkflowInstanceDao().saveActiveFlowNode(instanceId, this);

        // アクティブタスクを更新する。
        // ユーザかタスクのいずれかしかアサインされていないはずなので、ユーザがアサインされている場合はグループ側の処理は実行しない。
        List<String> users = getAssignedUsers(instanceId);
        if (!users.isEmpty()) {
            refreshActiveUserTasks(instanceId, users);
        } else {
            List<String> groups = getAssignedGroups(instanceId);
            if (!groups.isEmpty()) {
                refreshActiveGroupTasks(instanceId, groups);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * {@code executor} に指定されたユーザのアクティブタスクを完了させ、シーケンシャルタイプの場合は、実行順が次のユーザのアクティブタスクを作成する。
     * その後、 {@link CompletionCondition#isCompletedUserTask(Map, String, Task)} を評価してその結果を返却する。
     *
     * @param instanceId 対象のワークフローインスタンスID
     * @param parameter ワークフローの進行時に使用するパラメータ
     * @param executor このノードでの処理を実行しているユーザ
     * @return 次のフローノードにワークフローを進行させて良い場合は {@code true}
     * @throws IllegalStateException 指定された実行ユーザのアクティブタスクが存在しない場合
     */
    @Override
    public boolean processNodeByUser(String instanceId, Map<String, ?> parameter, String executor) throws IllegalStateException {
        super.processNodeByUser(instanceId, parameter, executor);

        String taskId = getFlowNodeId();

        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        ActiveUserTaskEntity activeUserTask = dao.findActiveUserTaskByPk(executor, taskId, instanceId);
        if (activeUserTask == null) {
            throw new IllegalStateException(
                    "Active task is not found for user = [" + executor + "]. instance id = [" + instanceId + "], task id = [" + taskId + "].");
        }

        dao.deleteActiveUserTaskByUserId(instanceId, taskId, executor);

        // シーケンシャルの場合のみ、実行順が次になっている担当ユーザのアクティブユーザタスクを作成しておく。
        if (isSequentialType()) {
            activateNextUserTask(instanceId, activeUserTask.getExecutionOrder());
        }
        return getCompletionCondition().isCompletedUserTask(parameter, instanceId, this);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * {@code executor} に指定されたグループのアクティブタスクを完了させ、シーケンシャルタイプの場合は、実行順が次のグループのアクティブタスクを作成する。
     * その後、 {@link CompletionCondition#isCompletedGroupTask(Map, String, Task)} を評価してその結果を返却する。
     *
     * @param instanceId 対象のワークフローインスタンスID
     * @param parameter ワークフローの進行時に使用するパラメータ
     * @param executor このノードでの処理を実行しているグループ
     * @return 次のフローノードにワークフローを進行させて良い場合は {@code true}
     * @throws IllegalStateException 指定された実行グループのアクティブタスクが存在しない場合
     */
    @Override
    public boolean processNodeByGroup(String instanceId, Map<String, ?> parameter, String executor) throws IllegalStateException {
        super.processNodeByGroup(instanceId, parameter, executor);

        String taskId = getFlowNodeId();

        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        ActiveGroupTaskEntity activeGroupTask = dao.findActiveGroupTaskByPk(instanceId, taskId, executor);
        if (activeGroupTask == null) {
            throw new IllegalStateException(
                    "Active task is not found for group = [" + executor + "]. instance id = [" + instanceId + "], task id = [" + taskId + "].");
        }

        dao.deleteActiveGroupTaskByGroupId(instanceId, taskId, executor);

        // シーケンシャルの場合のみ、実行順が次になっている担当ユーザのアクティブユーザタスクを作成しておく。
        if (isSequentialType()) {
            activateNextGroupTask(instanceId, activeGroupTask.getExecutionOrder());
        }
        return getCompletionCondition().isCompletedGroupTask(parameter, instanceId, this);
    }

    /**
     * タスクに担当ユーザを割り当てる。マルチインスタンスでないタスクに対しては、複数ユーザを割り当てることは出来ない。
     * <p/>
     * マルチインスタンスタスクで、シーケンシャルタイプの場合は、指定された担当ユーザの順序がタスクの実行順となる。
     * すでにタスクに担当ユーザや担当グループが割り当てられている場合、それらの情報はクリアされ、今回設定した担当ユーザのみが有効となる。
     *
     * @param instanceId ワークフローインスタンスID
     * @param users 担当ユーザリスト
     * @throws IllegalArgumentException 指定されたタスクが存在しない場合、もしくはマルチインスタンスでないタスクに複数ユーザを割り当てようとした場合。
     */
    public void assignUsers(String instanceId, List<String> users) throws IllegalArgumentException {
        String flowNodeId = getFlowNodeId();

        if (!isMultiInstanceType() && (users.size() > 1)) {
            throw new IllegalArgumentException("Multiple users cannot be assigned to NOT Multi-Instance Tasks."
                    + " instance id = [" + instanceId + "], task id = [" + flowNodeId + "], users = [" + users + "].");
        }

        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        if (isSequentialType()) {
            dao.saveAssignedSequentialUser(instanceId, flowNodeId, users);
        } else {
            dao.saveAssignedUser(instanceId, flowNodeId, users);
        }
    }

    /**
     * アクティブタスクを、現在このタスクに割り当てられている担当ユーザのタスクに置き換える。
     * <p/>
     * マルチインスタンスタスクで、シーケンシャルタイプの場合は、割り当てられている担当ユーザのうち実行順が先頭の担当ユーザの
     * アクティブタスクが作成される。それ以外の場合は、割り当てられているすべての担当ユーザのアクティブタスクが作成される。
     *
     * @param instanceId ワークフローインスタンスID
     * @param users 担当ユーザリスト
     */
    public void refreshActiveUserTasks(String instanceId, List<String> users) {
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        if (isSequentialType() && !users.isEmpty()) {
            // 順次タスクの場合は、実行順が先頭の担当ユーザだけを最初のアクティブユーザタスクとして登録する。
            // ただし、担当ユーザが空の場合は、アクティブユーザタスクを削除したいので、リストをそのままdaoに渡す。
            dao.saveActiveUserTask(instanceId, getFlowNodeId(), users.get(0), 1);
        } else {
            dao.saveActiveUserTask(instanceId, getFlowNodeId(), users);
        }
    }

    /**
     * タスクに担当グループを割り当てる。マルチインスタンスでないタスクに対しては、複数グループを割り当てることは出来ない。
     * <p/>
     * マルチインスタンスタスクで、シーケンシャルタイプの場合は、指定された担当グループの順序がタスクの実行順となる。
     * すでにタスクに担当ユーザや担当グループが割り当てられている場合、それらの情報はクリアされ、今回設定した担当グループのみが有効となる。
     *
     * @param instanceId ワークフローインスタンスID
     * @param groups 担当グループリスト
     * @throws IllegalArgumentException 指定されたタスクが存在しない場合、もしくはマルチインスタンスでないタスクに複数グループを割り当てようとした場合。
     */
    public void assignGroups(String instanceId, List<String> groups) throws IllegalArgumentException {
        String flowNodeId = getFlowNodeId();

        if (!isMultiInstanceType() && (groups.size() > 1)) {
            throw new IllegalArgumentException("Multiple groups cannot be assigned to NOT Multi-Instance Tasks."
                    + " instance id = [" + instanceId + "], task id = [" + flowNodeId + "], groups = [" + groups + "].");
        }

        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        if (isSequentialType()) {
            dao.saveAssignedSequentialGroup(instanceId, flowNodeId, groups);
        } else {
            dao.saveAssignedGroup(instanceId, flowNodeId, groups);
        }
    }

    /**
     * アクティブタスクを、指定された担当グループのタスクに置き換える。
     * <p/>
     * マルチインスタンスタスクで、シーケンシャルタイプの場合は、割り当てられている担当グループのうち実行順が先頭の担当グループの
     * アクティブタスクが作成される。それ以外の場合は、割り当てられているすべての担当グループのアクティブタスクが作成される。
     *
     * @param instanceId ワークフローインスタンスID
     * @param groups 担当グループリスト
     */
    public void refreshActiveGroupTasks(String instanceId, List<String> groups) {
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        if (isSequentialType() && !groups.isEmpty()) {
            // 順次タスクの場合は、実行順が先頭の担当グループだけを最初のアクティブグループタスクとして登録する。
            // ただし、担当グループが空の場合は、アクティブグループタスクを削除したいので、リストをそのままdaoに渡す。
            dao.saveActiveGroupTask(instanceId, getFlowNodeId(), groups.get(0), 1);
        } else {
            dao.saveActiveGroupTask(instanceId, getFlowNodeId(), groups);
        }
    }

    /**
     * タスクに現在アサインされている担当ユーザを、別の担当ユーザに振り替える。
     *
     * @param instanceId 対象のワークフローのインスタンスID
     * @param oldUser 振替元の担当ユーザ
     * @param newUser 振替先の担当ユーザ
     * @throws IllegalStateException 指定されたタスクに、振替元の担当ユーザがアサインされていない場合
     */
    public void changeAssignedUser(String instanceId, String oldUser, String newUser) throws IllegalStateException {
        String taskId = getFlowNodeId();

        List<String> assigned = getAssignedUsers(instanceId);
        if (!assigned.contains(oldUser)) {
            throw new IllegalStateException("User is not assigned to task."
                    + " instance id = [" + instanceId + "], task id = [" + taskId + "], "
                    + "old user = [" + oldUser + "], assigned user = [" + assigned + "].");
        }

        getWorkflowInstanceDao().changeAssignedUser(instanceId, taskId, oldUser, newUser);
    }

    /**
     * 現在のアクティブユーザタスクのうち、 {@code oldUser} に指定されたユーザのタスクを、 {@code newUser} のタスクに更新する。
     * <p/>
     * {@code oldUser} のアクティブユーザタスクが見つからない場合には、何も処理を行わない。
     *
     * @param instanceId 対象のワークフローのインスタンスID
     * @param oldUser 振替元の担当ユーザ
     * @param newUser 振替先の担当ユーザ
     */
    public void changeActiveUserTask(String instanceId, String oldUser, String newUser) {
        getWorkflowInstanceDao().changeActiveUser(instanceId, getFlowNodeId(), oldUser, newUser);
    }

    /**
     * タスクに現在アサインされている担当グループを、別の担当グループに振り替える。
     *
     * @param instanceId 対象のワークフローのインスタンスID
     * @param oldGroup 振替元の担当グループ
     * @param newGroup 振替先の担当グループ
     * @throws IllegalStateException 指定されたタスクに、振替元の担当グループがアサインされていない場合
     */
    public void changeAssignedGroup(String instanceId, String oldGroup, String newGroup) throws IllegalStateException {
        String taskId = getFlowNodeId();

        List<String> assigned = getAssignedGroups(instanceId);
        if (!assigned.contains(oldGroup)) {
            throw new IllegalStateException("Group is not assigned to task."
                    + " instance id = [" + instanceId + "], task id = [" + taskId + "], "
                    + "old group = [" + oldGroup + "], assigned group = [" + assigned + "].");
        }

        getWorkflowInstanceDao().changeAssignedGroup(instanceId, taskId, oldGroup, newGroup);
    }

    /**
     * 現在のアクティブグループタスクのうち、 {@code oldGroup} に指定されたグループのタスクを、 {@code newGroup} のタスクに更新する。
     * <p/>
     * {@code oldGroup} のアクティブグループタスクが見つからない場合には、何も処理を行わない。
     *
     * @param instanceId 対象のワークフローのインスタンスID
     * @param oldGroup 振替元の担当グループ
     * @param newGroup 振替先の担当グループ
     */
    public void changeActiveGroupTask(String instanceId, String oldGroup, String newGroup) {
        getWorkflowInstanceDao().changeActiveGroup(instanceId, getFlowNodeId(), oldGroup, newGroup);
    }

    /**
     * タスクに割り当てられた担当ユーザを取得する。
     * <p/>
     * 担当ユーザは、実行順でソートされて返却される。
     *
     * @param instanceId 対象のワークフローのインスタンスID
     * @return 担当ユーザ
     */
    public List<String> getAssignedUsers(String instanceId) {
        List<TaskAssignedUserEntity> users = getWorkflowInstanceDao().findTaskAssignedUser(instanceId, getFlowNodeId());

        List<String> result = new ArrayList<String>(users.size());
        for (TaskAssignedUserEntity user : users) {
            result.add(user.getUserId());
        }
        return result;
    }

    /**
     * タスクに割り当てられた担当グループを取得する。
     * <p/>
     * 担当グループは、実行順でソートされて返却される。
     *
     * @param instanceId 対象のワークフローのインスタンスID
     * @return 担当グループ
     */
    public List<String> getAssignedGroups(String instanceId) {
        List<TaskAssignedGroupEntity> groups = getWorkflowInstanceDao().findTaskAssignedGroup(instanceId, getFlowNodeId());

        List<String> result = new ArrayList<String>();
        for (TaskAssignedGroupEntity group : groups) {
            result.add(group.getAssignedGroupId());
        }
        return result;
    }

    /**
     * 順次タスクであるアクティブユーザタスクで、実行順が次になっている担当ユーザのアクティブユーザタスクを作成する。
     *
     * @param instanceId 対象のワークフローインスタンスID
     * @param currentOrder 現在のタスクの実行順
     */
    private void activateNextUserTask(String instanceId, int currentOrder) {
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        // 実行順が次になっている担当ユーザのタスクを取得して、アクティブユーザタスクとして登録する。
        TaskAssignedUserEntity candidate = find(dao.findTaskAssignedUser(instanceId, getFlowNodeId()), new UserExecutionOrderFilter(currentOrder));
        if (candidate != null) {
            dao.saveActiveUserTask(instanceId, getFlowNodeId(), candidate.getUserId(), candidate.getExecutionOrder());
        }
    }

    /**
     * 順次タスクであるアクティブグループタスクで、実行順が次になっている担当グループのアクティブグループタスクを作成する。
     *
     * @param instanceId 対象のワークフローインスタンスID
     * @param currentOrder 現在のタスクの実行順
     */
    private void activateNextGroupTask(String instanceId, int currentOrder) {
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        // 実行順が次になっている担当グループのタスクを取得して、アクティブグループタスクとして登録する。
        TaskAssignedGroupEntity candidate = find(dao.findTaskAssignedGroup(instanceId, getFlowNodeId()), new GroupExecutionOrderFilter(currentOrder));
        if (candidate != null) {
            dao.saveActiveGroupTask(instanceId, getFlowNodeId(), candidate.getAssignedGroupId(), candidate.getExecutionOrder());
        }
    }

    /**
     * {@link TaskAssignedUserEntity#getExecutionOrder()} でフィルタを行う {@link ListFilter} 実装クラス。
     * <p/>
     * 指定された実行順より大きい実行順を持つ {@link TaskAssignedUserEntity} を返却する。
     */
    private static class UserExecutionOrderFilter implements ListFilter<TaskAssignedUserEntity> {

        /** フィルタ対象の実行順 */
        private final int order;

        /**
         * フィルタ対象の実行順を指定してインスタンスを生成する。
         *
         * @param order フィルタ対象の実行順
         */
        public UserExecutionOrderFilter(int order) {
            this.order = order;
        }

        /**
         * {@inheritDoc}
         * <p/>
         * 比較対象の {@link TaskAssignedUserEntity#getExecutionOrder()} がフィルタ対象の実行順より大きい値を返す場合 {@code true}
         *
         * @param other 比較対象
         * @return フィルタ対象よりも比較対象の実行順が大きい場合 {@code true}
         */
        @Override
        public boolean isMatch(TaskAssignedUserEntity other) {
            return order < other.getExecutionOrder();
        }
    }

    /**
     * {@link TaskAssignedGroupEntity#getExecutionOrder()} でフィルタを行う {@link ListFilter} 実装クラス。
     * <p/>
     * 指定された実行順より大きい実行順を持つ {@link TaskAssignedGroupEntity} を返却する。
     */
    private static class GroupExecutionOrderFilter implements ListFilter<TaskAssignedGroupEntity> {

        /** フィルタ対象の実行順 */
        private final int order;

        /**
         * フィルタ対象の実行順を指定してインスタンスを生成する。
         *
         * @param order フィルタ対象の実行順
         */
        public GroupExecutionOrderFilter(int order) {
            this.order = order;
        }

        /**
         * {@inheritDoc}
         * <p/>
         * 比較対象の {@link TaskAssignedGroupEntity#getExecutionOrder()} がフィルタ対象の実行順より大きい値を返す場合 {@code true}
         *
         * @param other 比較対象
         * @return フィルタ対象よりも比較対象の実行順が大きい場合 {@code true}
         */
        @Override
        public boolean isMatch(TaskAssignedGroupEntity other) {
            return order < other.getExecutionOrder();
        }
    }
}

