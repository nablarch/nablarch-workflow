package nablarch.integration.workflow;

import java.util.List;
import java.util.Map;

import nablarch.integration.workflow.definition.FlowNode;
import nablarch.integration.workflow.definition.Task;
import nablarch.integration.workflow.util.WorkflowUtil;
import nablarch.integration.workflow.dao.WorkflowInstanceDao;
import nablarch.integration.workflow.definition.BoundaryEvent;
import nablarch.integration.workflow.definition.Event;
import nablarch.integration.workflow.definition.Event.EventType;
import nablarch.integration.workflow.definition.WorkflowDefinition;

/**
 * {@link WorkflowInstance} の基本実装クラス。
 *
 * @author Ryo Tanaka
 * @since 1.4.2
 */
public class BasicWorkflowInstance extends WorkflowInstanceSupport {

    /** インスタンスID */
    private final String instanceId;

    /** 管理対象ワークフローインスタンスのワークフロー定義 */
    private final WorkflowDefinition definition;

    /** 管理対象ワークフローインスタンスのアクティブフローノード */
    private FlowNode active;

    /**
     * {@inheritDoc}
     * <p/>
     * アクティブフローノードの {@link FlowNode#processNodeByUser(String, Map, String)} を実行して、ワークフローを現在のフローノードから進行させるかどうかを判断する。
     * 現在のフローノードから進行させる場合には、次のタスクもしくは停止イベントに進行させ、進行先フローノードのアクティブ化処理を実行する。
     */
    @Override
    public void completeUserTask(Map<String, ?> parameter, String assigned) throws IllegalStateException {
        if (isCompleted()) {
            throw new IllegalStateException("Workflow is already completed. " + this);
        }

        boolean completed = active.processNodeByUser(instanceId, parameter, assigned);

        if (completed) {
            proceedToNextNode(parameter);
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * アクティブフローノードの {@link FlowNode#processNodeByGroup(String, Map, String)} を実行して、ワークフローを現在のフローノードから進行させるかどうかを判断する。
     * 現在のフローノードから進行させる場合には、次のタスクもしくは停止イベントに進行させ、進行先フローノードのアクティブ化処理を実行する。
     */
    @Override
    public void completeGroupTask(Map<String, ?> parameter, String assigned) throws IllegalStateException {
        if (isCompleted()) {
            throw new IllegalStateException("Workflow is already completed. " + this);
        }

        boolean completed = active.processNodeByGroup(instanceId, parameter, assigned);

        if (completed) {
            proceedToNextNode(parameter);
        }
    }

    @Override
    public void triggerEvent(String eventTriggerId, Map<String, ?> parameter) throws IllegalStateException {
        List<BoundaryEvent> events = WorkflowUtil.filterList(definition.getBoundaryEvent(eventTriggerId), new WorkflowUtil.ListFilter<BoundaryEvent>() {
            @Override
            public boolean isMatch(BoundaryEvent other) {
                return other.getAttachedTaskId().equals(active.getFlowNodeId());
            }
        });
        if (events.isEmpty()) {
            throw new IllegalStateException(
                    "Boundary Event is not found for the event trigger. event trigger id = [" + eventTriggerId + "], active flow node = ["
                            + active.getFlowNodeId() + "]. " + this);
        }
        active = events.get(0);
        proceedToNextNode(parameter);
    }

    @Override
    public void assignUsers(String taskId, List<String> users) throws IllegalStateException, IllegalArgumentException {
        if (isCompleted()) {
            throw new IllegalStateException("Cannot assign users to a completed workflow. users = [" + users + "], " + this);
        }

        Task task = definition.findTask(taskId);
        task.assignUsers(instanceId, users);

        if (isActive(taskId)) {
            task.refreshActiveUserTasks(instanceId, users);
        }
    }

    @Override
    public void assignGroups(String taskId, List<String> groups) throws IllegalStateException, IllegalArgumentException {
        if (isCompleted()) {
            throw new IllegalStateException("Cannot assign groups to a completed workflow. groups = [" + groups + "], " + this);
        }

        Task task = definition.findTask(taskId);
        task.assignGroups(instanceId, groups);

        if (isActive(taskId)) {
            task.refreshActiveGroupTasks(instanceId, groups);
        }
    }

    @Override
    public void assignUsersToLane(String laneId, List<String> users) throws IllegalStateException, IllegalArgumentException {
        List<Task> tasks = WorkflowUtil.filterList(definition.getTasks(), new LaneFilter(laneId));
        for (Task task : tasks) {
            assignUsers(task.getFlowNodeId(), users);
        }
    }

    @Override
    public void assignGroupsToLane(String laneId, List<String> groups) throws IllegalStateException, IllegalArgumentException {
        List<Task> tasks = WorkflowUtil.filterList(definition.getTasks(), new LaneFilter(laneId));
        for (Task task : tasks) {
            assignGroups(task.getFlowNodeId(), groups);
        }
    }

    @Override
    public void changeAssignedUser(String taskId, String oldUser, String newUser) throws IllegalArgumentException, IllegalStateException {
        Task task = definition.findTask(taskId);
        task.changeAssignedUser(instanceId, oldUser, newUser);

        if (isActive(taskId)) {
            task.changeActiveUserTask(instanceId, oldUser, newUser);
        }
    }

    @Override
    public void changeAssignedGroup(String taskId, String oldGroup, String newGroup) throws IllegalArgumentException, IllegalStateException {
        Task task = definition.findTask(taskId);
        task.changeAssignedGroup(instanceId, oldGroup, newGroup);

        if (isActive(taskId)) {
            task.changeActiveGroupTask(instanceId, oldGroup, newGroup);
        }
    }

    @Override
    public List<String> getAssignedUsers(String taskId) throws IllegalArgumentException {
        return definition.findTask(taskId).getAssignedUsers(instanceId);
    }

    @Override
    public List<String> getAssignedGroups(String taskId) throws IllegalArgumentException {
        return definition.findTask(taskId).getAssignedGroups(instanceId);
    }

    @Override
    public boolean hasActiveUserTask(String user) {
        return getWorkflowInstanceDao().getActiveUserTaskCountByPk(instanceId, active.getFlowNodeId(), user) != 0;
    }

    @Override
    public boolean hasActiveGroupTask(String group) {
        return getWorkflowInstanceDao().getActiveGroupTaskCountByPk(instanceId, active.getFlowNodeId(), group) != 0;
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public String getWorkflowId() {
        return definition.getWorkflowId();
    }

    @Override
    public long getVersion() {
        return definition.getVersion();
    }

    @Override
    public boolean isActive(String flowNodeId) {
        return active.getFlowNodeId().equals(flowNodeId);
    }

    @Override
    public boolean isCompleted() {
        return isCompletedNode(active);
    }

    /**
     * 次のノードをアクティブにする。
     * <p/>
     * 次のノードが、終了イベントの場合にはワークフローインスタンス自体を処理完了に変更する。
     *
     * @param parameter パラメータ
     */
    void proceedToNextNode(Map<String, ?> parameter) {
        FlowNode candidate = definition.findFlowNode(active.getNextFlowNodeId(instanceId, parameter));
        while (!(candidate instanceof Task) && !isCompletedNode(candidate)) {
            candidate = definition.findFlowNode(candidate.getNextFlowNodeId(instanceId, parameter));
        }

        candidate.activate(instanceId, parameter);
        active = candidate;
    }

    /**
     * 指定されたフローノードがアクティブになった場合、ワークフローが完了状態になるかどうかを返却する。
     *
     * @param node 対象のフローノード
     * @return 対象のフローノードがアクティブになった場合、ワークフローを完了状態として扱う場合 {@code true}
     */
    private boolean isCompletedNode(FlowNode node) {
        return (node instanceof Event) && (((Event) node).getEventType() == EventType.TERMINATE);
    }

    /**
     * ワークフローインスタンスを生成する。
     *
     * @param instanceId インスタンスID
     * @param definition ワークフロー定義
     * @param activeNode アクティブフローノード
     */
    public BasicWorkflowInstance(String instanceId, WorkflowDefinition definition, FlowNode activeNode) {
        this.instanceId = instanceId;
        this.definition = definition;
        this.active = activeNode;
    }

    /**
     * 以下の情報を含む、ワークフローインスタンスの文字列表現を返却する。
     * <p/>
     * * インスタンスID
     * * ワークフローID
     * * バージョン
     * * アクティブフローノードのフローノードID
     *
     * @return ワークフローインスタンスの文字列表現
     */
    @Override
    public String toString() {
        return "instance id = [" + getInstanceId()
                + "], workflow id = [" + getWorkflowId()
                + "], version = [" + getVersion()
                + "], active flow node id = [" + active.getFlowNodeId() + "]";
    }

    /**
     * ワークフローインスタンス系テーブルへのアクセスクラスを取得する。
     *
     * @return ワークフローインスタンスDAO
     */
    private static WorkflowInstanceDao getWorkflowInstanceDao() {
        return WorkflowConfig.get().getWorkflowInstanceDao();
    }

    /**
     * レーンIDでフィルタを行う{@link WorkflowUtil.ListFilter}実装クラス。
     */
    private static class LaneFilter implements WorkflowUtil.ListFilter<Task> {

        /** レーンID */
        private final String laneId;

        /**
         * レーンIDでのフィルタクラスを生成する。
         *
         * @param laneId レーンID
         */
        public LaneFilter(String laneId) {
            this.laneId = laneId;
        }

        @Override
        public boolean isMatch(Task other) {
            return other.getLaneId().equals(laneId);
        }
    }
}
