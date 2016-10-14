package nablarch.integration.workflow.condition;

import java.util.Map;

import nablarch.integration.workflow.WorkflowConfig;
import nablarch.integration.workflow.dao.WorkflowInstanceDao;
import nablarch.integration.workflow.definition.Task;

/**
 * 一定数の担当者(ユーザまたはグループ)がタスク実行済みの場合に、タスク完了と判断する終了条件クラス。
 * <p/>
 * 一定数の指定は、本クラスのインスタンス生成時に指定する。
 * 一定数に満たない場合でも、全てのユーザ(グループ)が処理済みとなった場合には、
 * これ以上処理するユーザ(グループ)がいないため、タスク完了と判断する。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class OrCompletionCondition implements CompletionCondition {

    /** 閾値 */
    private final int threshold;

    /**
     * コンストラクタ。
     *
     * @param threshold 閾値
     */
    public OrCompletionCondition(String threshold) {
        this.threshold = Integer.valueOf(threshold);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * コンストラクタで指定された一定数のユーザが処理済みの場合、
     * タスク完了としtrueを返却する。
     */
    @Override
    public boolean isCompletedUserTask(Map<String, ?> param, String instanceId, Task task) {
        WorkflowInstanceDao workflowInstanceDao = WorkflowConfig.get().getWorkflowInstanceDao();

        int assignedUserCount = workflowInstanceDao.getTaskAssignedUserCount(instanceId, task.getFlowNodeId());
        int activeUserTaskCount = workflowInstanceDao.getActiveUserTaskCount(instanceId);
        return (activeUserTaskCount == 0) || ((assignedUserCount - activeUserTaskCount) >= threshold);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * コンストラクタで指定された一定数のグループが処理済みの場合、
     * タスク完了としtrueを返却する。
     */
    @Override
    public boolean isCompletedGroupTask(Map<String, ?> param, String instanceId, Task task) {
        WorkflowInstanceDao workflowInstanceDao = WorkflowConfig.get().getWorkflowInstanceDao();

        int assignedGroupCount = workflowInstanceDao.getTaskAssignedGroupCount(instanceId, task.getFlowNodeId());
        int activeGroupTaskCount = workflowInstanceDao.getActiveGroupTaskCount(instanceId);
        return (activeGroupTaskCount == 0) || ((assignedGroupCount - activeGroupTaskCount) >= threshold);
    }
}

