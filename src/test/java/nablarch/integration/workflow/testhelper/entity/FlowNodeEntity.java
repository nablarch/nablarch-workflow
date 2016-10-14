package nablarch.integration.workflow.testhelper.entity;

/**
 * フローノードエンティティ。
 * <p/>
 * 本クラスは、イミュータブルオブジェクトのためフィールドを公開している。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public abstract class FlowNodeEntity {

    /** フローノードID */
    public final String flowNodeId;

    /** レーンID */
    public final LaneEntity laneEntity;

    /** 名前 */
    public final String name;

    /**
     * フローノードエンティティを生成する。
     *
     * @param flowNodeId フローノードID
     * @param laneEntity レーンID
     * @param name 名前
     */
    public FlowNodeEntity(String flowNodeId, LaneEntity laneEntity, String name) {
        this.flowNodeId = flowNodeId;
        this.laneEntity = laneEntity;
        this.name = name;
    }
}
