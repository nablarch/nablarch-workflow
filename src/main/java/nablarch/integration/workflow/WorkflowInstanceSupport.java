package nablarch.integration.workflow;

import java.util.Collections;
import java.util.Map;

import nablarch.core.ThreadContext;

/**
 * {@link WorkflowInstance} 実装クラスのサポートクラス。
 * <p/>
 * 本クラスでは、オーバーロードされたメソッドの委譲関係を実装している。
 *
 * @author Ryo Tanaka
 * @since 1.4.2
 */
public abstract class WorkflowInstanceSupport implements WorkflowInstance {

    /**
     * {@inheritDoc}
     * <p/>
     * タスクを完了させるユーザには、{@link ThreadContext#getUserId()} が利用され、ワークフローを進行させる際のパラメータには、空のMapが利用される。
     *
     * @throws IllegalStateException {@link ThreadContext#getUserId()} に対してアクティブユーザタスクが見つからない場合、またはワークフローが既に完了している場合。
     */
    @Override
    public void completeUserTask() throws IllegalStateException {
        completeUserTask(Collections.<String, Object>emptyMap());
    }

    /**
     * {@inheritDoc}
     * <p/>
     * ワークフローを進行させる際のパラメータには、空のMapが利用される。
     */
    @Override
    public void completeUserTask(String assigned) throws IllegalStateException {
        completeUserTask(Collections.<String, Object>emptyMap(), assigned);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * タスクを完了させるユーザには、{@link ThreadContext#getUserId()} が利用される。
     *
     * @throws IllegalStateException {@link ThreadContext#getUserId()} に対してアクティブユーザタスクが見つからない場合、またはワークフローが既に完了している場合。
     */
    @Override
    public void completeUserTask(Map<String, ?> parameter) throws IllegalStateException {
        completeUserTask(parameter, ThreadContext.getUserId());
    }

    /**
     * {@inheritDoc}
     * <p/>
     * ワークフローを進行させる際のパラメータには、空のMapが利用される。
     */
    @Override
    public void completeGroupTask(String assigned) throws IllegalStateException {
        completeGroupTask(Collections.<String, Object>emptyMap(), assigned);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * ワークフローを進行させる際のパラメータには、空のMapが利用される。
     */
    @Override
    public void triggerEvent(String eventTriggerId) throws IllegalStateException {
        triggerEvent(eventTriggerId, Collections.<String, Object>emptyMap());
    }

    @Override
    public void assignUser(String taskId, String user) throws IllegalStateException, IllegalArgumentException {
        assignUsers(taskId, Collections.singletonList(user));
    }

    @Override
    public void assignGroup(String taskId, String group) throws IllegalStateException, IllegalArgumentException {
        assignGroups(taskId, Collections.singletonList(group));
    }

    @Override
    public void assignUserToLane(String laneId, String user) throws IllegalStateException, IllegalArgumentException {
        assignUsersToLane(laneId, Collections.singletonList(user));
    }

    @Override
    public void assignGroupToLane(String laneId, String group) throws IllegalStateException, IllegalArgumentException {
        assignGroupsToLane(laneId, Collections.singletonList(group));
    }
}
