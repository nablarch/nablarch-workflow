package nablarch.integration.workflow.dao;

/**
 * アクティブユーザタスクエンティティ。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class ActiveUserTaskEntity extends TaskAssignedUserEntity {

    /**
     * アクティブユーザエンティティを生成する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param userId ユーザID
     * @param executionOrder 実行順
     */
    public ActiveUserTaskEntity(String instanceId, String flowNodeId, String userId, int executionOrder) {
        super(instanceId, flowNodeId, userId, executionOrder);
    }
}
