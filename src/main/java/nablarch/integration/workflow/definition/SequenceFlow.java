package nablarch.integration.workflow.definition;

import java.util.Map;

import nablarch.integration.workflow.condition.FlowProceedCondition;
import nablarch.integration.workflow.util.WorkflowUtil;

/**
 * シーケンスフローの定義を表すクラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class SequenceFlow {

    /** シーケンスフローID */
    private final String sequenceFlowId;

    /** シーケンスフロー名 */
    private final String sequenceFlowName;

    /** 遷移元シーケンスフローID */
    private final String sourceFlowNodeId;

    /** 遷移先シーケンスフローID */
    private final String targetFlowNodeId;

    /** フローコンディション */
    private final FlowProceedCondition flowProceedCondition;

    /**
     * シーケンスフロー定義を生成する。
     *
     * @param sequenceFlowId シーケンスフローID
     * @param sequenceFlowName シーケンスフロー名
     * @param sourceFlowNodeId 遷移元シーケンスフローID
     * @param targetFlowNodeId 遷移先シーケンスフローID
     * @param flowCondition フローコンディションの型名
     */
    public SequenceFlow(
            String sequenceFlowId,
            String sequenceFlowName,
            String sourceFlowNodeId,
            String targetFlowNodeId,
            String flowCondition) {
        this.sequenceFlowId = sequenceFlowId;
        this.sequenceFlowName = sequenceFlowName;
        this.sourceFlowNodeId = sourceFlowNodeId;
        this.targetFlowNodeId = targetFlowNodeId;
        flowProceedCondition = WorkflowUtil.createInstance(flowCondition);
    }

    /**
     * シーケンスフローIDを取得する。
     *
     * @return シーケンスフローID
     */
    public String getSequenceFlowId() {
        return sequenceFlowId;
    }

    /**
     * シーケンスフロー名を取得する。
     *
     * @return シーケンスフロー名
     */
    public String getSequenceFlowName() {
        return sequenceFlowName;
    }

    /**
     * 遷移元フローノードIDを取得する。
     *
     * @return 遷移元フローノードID
     */
    public String getSourceFlowNodeId() {
        return sourceFlowNodeId;
    }

    /**
     * 遷移先フローノードIDを取得する。
     *
     * @return 遷移先フローノードID
     */
    public String getTargetFlowNodeId() {
        return targetFlowNodeId;
    }

    /**
     * このシーケンスフローで遷移可能か否かを判定する。
     *
     * 遷移可能かの判断は、{@link FlowProceedCondition}により判断する。
     * 遷移条件({@link FlowProceedCondition})が指定されていない場合には、常に遷移可能(true)を返す。
     *
     * @see FlowProceedCondition
     * @param instanceId インスタンスID
     * @param parameter パラメータ
     * @return 進行可能の場合はtrue
     */
    public boolean canProceed(String instanceId, Map<String, ?> parameter) {
        if (flowProceedCondition == null) {
            return true;
        }
        return flowProceedCondition.isMatch(instanceId, parameter, this);
    }
}

