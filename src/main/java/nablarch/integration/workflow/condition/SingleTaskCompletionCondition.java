package nablarch.integration.workflow.condition;

import java.util.Map;

import nablarch.integration.workflow.definition.Task;

/**
 * シングルタスク(非マルチインスタンス)の完了条件クラス。
 * <p/>
 * 本クラスは、完了条件判定結果として常にtrueを返却する。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class SingleTaskCompletionCondition implements CompletionCondition {

    /**
     * {@inheritDoc}
     * <p/>
     * 本実装では、常にtrueを返す
     */
    @Override
    public boolean isCompletedUserTask(Map<String, ?> param, String instanceId, Task task) {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * 本実装では、常にtrueを返す
     */
    @Override
    public boolean isCompletedGroupTask(Map<String, ?> param, String instanceId, Task task) {
        return true;
    }
}
