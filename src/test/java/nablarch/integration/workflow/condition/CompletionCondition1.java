package nablarch.integration.workflow.condition;

import java.util.Map;

import nablarch.integration.workflow.definition.Task;

public class CompletionCondition1 implements CompletionCondition {

    /**
     * ユーザタスクの終了判定を行う。
     *
     * @param param パラメータ
     * @param instanceId インスタンスID
     * @param task タスク
     * @return 終了条件と一致した場合はtrue
     */
    @Override
    public boolean isCompletedUserTask(Map<String, ?> param, String instanceId, Task task) {
        return false;
    }

    /**
     * グループタスクの終了判定を行う。
     *
     * @param param パラメータ
     * @param instanceId インスタンスID
     * @param task タスク
     * @return 終了条件と一致した場合はtrue
     */
    @Override
    public boolean isCompletedGroupTask(Map<String, ?> param, String instanceId, Task task) {
        return false;
    }
}
