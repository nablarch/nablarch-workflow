package nablarch.integration.workflow.testhelper.entity;

/**
 * イベントノードエンティティ。
 * <p/>
 * 本クラスは、イミュータブルオブジェクトのためフィールドを公開している。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class EventEntity extends FlowNodeEntity {

    /** イベントタイプ */
    public final String eventType;

    /**
     * イベントノードエンティティを生成する。
     *
     * @param flowNodeId フローノードID
     * @param laneEntity レーンエンティティ
     * @param name 名前
     * @param eventType イベントタイプ
     */
    public EventEntity(String flowNodeId, LaneEntity laneEntity, String name, String eventType) {
        super(flowNodeId, laneEntity, name);
        this.eventType = eventType;
    }
}

