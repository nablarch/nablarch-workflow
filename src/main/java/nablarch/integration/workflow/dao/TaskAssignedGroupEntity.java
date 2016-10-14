package nablarch.integration.workflow.dao;

/**
 * 担当グループ情報エンティティ。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class TaskAssignedGroupEntity {

    /** インスタンスID */
    private final String instanceId;

    /** フローノードID */
    private final String flowNodeId;

    /** 担当グループ ID */
    private final String assignedGroupId;

    /** 実行順 */
    private final int executionOrder;

    /**
     * 担当グループエンティティを生成する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param assignedGroupId 担当グループID
     * @param executionOrder 実行順
     */
    public TaskAssignedGroupEntity(String instanceId, String flowNodeId, String assignedGroupId, int executionOrder) {
        this.instanceId = instanceId;
        this.flowNodeId = flowNodeId;
        this.assignedGroupId = assignedGroupId;
        this.executionOrder = executionOrder;
    }

    /**
     * インスタンスIDを取得する。
     *
     * @return インスタンスID
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * フローノードIDを取得する。
     *
     * @return フローノードID
     */
    public String getFlowNodeId() {
        return flowNodeId;
    }

    /**
     * 担当グループIDを取得する。
     *
     * @return 担当グループID
     */
    public String getAssignedGroupId() {
        return assignedGroupId;
    }

    /**
     * 実行順を取得する。
     *
     * @return 実行順
     */
    public int getExecutionOrder() {
        return executionOrder;
    }
}

