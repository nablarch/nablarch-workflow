package nablarch.integration.workflow.dao;

/**
 * アクティブフローノードエンティティ。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class ActiveFlowNodeEntity {

    /** インスタンスID */
    private final String instanceId;

    /** フローノードID */
    private final String flowNodeId;

    /**
     * アクティブフローノードエンティティを生成する。
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     */
    public ActiveFlowNodeEntity(String instanceId, String flowNodeId) {
        this.instanceId = instanceId;
        this.flowNodeId = flowNodeId;
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
}
