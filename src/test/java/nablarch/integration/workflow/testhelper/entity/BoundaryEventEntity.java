package nablarch.integration.workflow.testhelper.entity;

/**
 * バウンダリーイベントエンティティ。
 * <p/>
 * 本クラスは、イミュータブルオブジェクトのためフィールドを公開している。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class BoundaryEventEntity extends FlowNodeEntity {

    /** アクティビティエンティティ */
    public final TaskEntity taskEntity;

    /** イベントエンティティ */
    public final BoundaryEventTriggerEntity boundaryEventTriggerEntity;

    /**
     * バウンダリイベントエンティティを生成する。
     *
     * @param taskEntity アクティビティエンティティ
     * @param boundaryEventTriggerEntity イベントエンティティ
     */
    public BoundaryEventEntity(
            String flowNodeId,
            String flowNodeName,
            LaneEntity laneEntity,
            TaskEntity taskEntity,
            BoundaryEventTriggerEntity boundaryEventTriggerEntity) {
        super(flowNodeId, laneEntity, flowNodeName);
        this.taskEntity = taskEntity;
        this.boundaryEventTriggerEntity = boundaryEventTriggerEntity;
    }
}
