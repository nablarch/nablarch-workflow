package nablarch.integration.workflow.condition;

import java.util.Map;

import nablarch.integration.workflow.WorkflowConfig;
import nablarch.integration.workflow.dao.WorkflowInstanceDao;
import nablarch.integration.workflow.definition.Task;

/**
 * 全ての担当者またはグループがタスクが実行済みであることを検証する終了判定クラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class AllCompletionCondition implements CompletionCondition {

    /**
     * {@inheritDoc}
     * <p/>
     *
     * 全てのアクティブユーザタスクが処理済み（レコードが存在しない）状態の場合、タスク終了としtrueを返す。
     */
    @Override
    public boolean isCompletedUserTask(Map<String, ?> param, String instanceId, Task task) {
        WorkflowInstanceDao workflowInstanceDao = WorkflowConfig.get().getWorkflowInstanceDao();
        return workflowInstanceDao.getActiveUserTaskCount(instanceId) == 0;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * 全てのアクティブグループタスクが処理済み（レコードが存在しない）状態の場合、タスク終了としtrueを返す。
     */
    @Override
    public boolean isCompletedGroupTask(Map<String, ?> param, String instanceId, Task task) {
        WorkflowInstanceDao workflowInstanceDao = WorkflowConfig.get().getWorkflowInstanceDao();
        return workflowInstanceDao.getActiveGroupTaskCount(instanceId) == 0;
    }
}
