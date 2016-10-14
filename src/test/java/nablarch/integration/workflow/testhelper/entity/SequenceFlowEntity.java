package nablarch.integration.workflow.testhelper.entity;

/**
 * シーケンスフロー定義エンティティ。
 * <p/>
 * 本クラスは、イミュータブルオブジェクトのため、フィールドをpublicで公開している。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class SequenceFlowEntity {

    /** プロセスエンティティ */
    public final WorkflowEntity workflowEntity;

    /** シーケンスID */
    public final String sequenceId;

    /** シーケンス名*/
    public final String name;

    /** 遷移元ノードID */
    public final String sourceFlowNodeId;

    /** 遷移先ノードID */
    public final String targetFlowNodeId;

    /** フローコンディション */
    public final String flowCondition;

    /**
     * シーケンスフロー定義エンティティを生成する。
     * @param workflowEntity
     * @param sequenceId シーケンスID
     * @param name シーケンス名
     * @param sourceFlowNodeId 遷移元ノードID
     * @param targetFlowNodeId 遷移先ノードID
     * @param flowCondition フローコンディション
     */
    public SequenceFlowEntity(
            WorkflowEntity workflowEntity,
            String sequenceId,
            String name,
            String sourceFlowNodeId,
            String targetFlowNodeId,
            String flowCondition) {
        this.workflowEntity = workflowEntity;
        this.sequenceId = sequenceId;
        this.name = name;
        this.sourceFlowNodeId = sourceFlowNodeId;
        this.targetFlowNodeId = targetFlowNodeId;
        this.flowCondition = flowCondition;
    }
}

