package nablarch.integration.workflow.testhelper.entity;

/**
 * イベントノードエンティティ。
 * <p/>
 * 本クラスは、イミュータブルオブジェクトのためフィールドを公開している。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class GatewayEntity extends FlowNodeEntity {

    /** ゲートウェイタイプ */
    public final String gatewayType;

    /**
     * Gatewayエンティティを生成する。
     * @param flowNodeId フローノードID
     * @param laneEntity レーンエンティティ
     * @param name 名前
     * @param gatewayType ゲートウェイタイプ
     */
    public GatewayEntity(
            String flowNodeId, LaneEntity laneEntity, String name, String gatewayType) {
        super(flowNodeId, laneEntity, name);
        this.gatewayType = gatewayType;
    }
}
