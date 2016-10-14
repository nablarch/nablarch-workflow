package nablarch.integration.workflow.dao;

/**
 * アクティブグループタスクエンティティ。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class ActiveGroupTaskEntity extends TaskAssignedGroupEntity {

    /**
     * アクティブグループタスクエンティティを生成する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param assignedGroupId 担当グループID
     * @param executionOrder 実行順
     */
    public ActiveGroupTaskEntity(String instanceId, String flowNodeId, String assignedGroupId, int executionOrder) {
        super(instanceId, flowNodeId, assignedGroupId, executionOrder);
    }
}
