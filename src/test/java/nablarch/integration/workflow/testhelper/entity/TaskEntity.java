package nablarch.integration.workflow.testhelper.entity;

/**
 * アクティビティエンティティ。
 * <p/>
 * 本クラスは、イミュータブルオブジェクトのためフィールドを公開している。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class TaskEntity extends FlowNodeEntity {

    /** マルチインスタンスタイプ */
    public final String multiInstanceType;

    /** 終了条件 */
    public final String completionCondition;

    /**
     * アクティビティエンティティを生成する。
     *  @param flowNodeId フローノードID
     * @param laneEntity レーンエンティティ
     * @param name 名前
     * @param multiInstanceType マルチインスタンスタイプ
     * @param completionCondition
     */
    public TaskEntity(String flowNodeId, LaneEntity laneEntity, String name, String multiInstanceType,
            String completionCondition) {
        super(flowNodeId, laneEntity, name);
        this.multiInstanceType = multiInstanceType;
        this.completionCondition = completionCondition;
    }
}
