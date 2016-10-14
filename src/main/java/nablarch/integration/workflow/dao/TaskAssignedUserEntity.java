package nablarch.integration.workflow.dao;

/**
 * 担当ユーザ情報エンティティ。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class TaskAssignedUserEntity {

    /** インスタスID */
    private final String instanceId;

    /** フローノードID */
    private final String flowNodeId;

    /** 担当ユーザID */
    private final String userId;

    /** 実行順 */
    private final int executionOrder;

    /**
     * 担当ユーザ情報エンティティを生成する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param userId ユーザID
     * @param executionOrder 実行順
     */
    public TaskAssignedUserEntity(String instanceId, String flowNodeId, String userId, int executionOrder) {
        this.instanceId = instanceId;
        this.flowNodeId = flowNodeId;
        this.userId = userId;
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
     * ユーザIDを取得する。
     *
     * @return ユーザID
     */
    public String getUserId() {
        return userId;
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
