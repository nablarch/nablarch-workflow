package nablarch.integration.workflow.condition;

import java.util.Map;

import nablarch.integration.workflow.definition.Task;

/**
 * マルチインスタンスタスクの終了判定を行うインタフェース。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public interface CompletionCondition {

    /**
     * ユーザタスクの終了判定を行う。
     *
     * @param param パラメータ
     * @param instanceId インスタンスID
     * @param task タスク
     * @return 終了条件と一致した場合はtrue
     */
    boolean isCompletedUserTask(Map<String, ?> param, String instanceId, Task task);

    /**
     * グループタスクの終了判定を行う。
     *
     * @param param パラメータ
     * @param instanceId インスタンスID
     * @param task タスク
     * @return 終了条件と一致した場合はtrue
     */
    boolean isCompletedGroupTask(Map<String, ?> param, String instanceId, Task task);
}
