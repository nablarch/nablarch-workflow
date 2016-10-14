package nablarch.integration.workflow.definition;

import java.util.List;
import java.util.Map;

/**
 * ゲートウェイ定義を表すクラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class Gateway extends FlowNode {

    /**
     * ゲートウェイタイプ
     *
     * @author hisaaki sioiri
     * @since 1.4.2
     */
    public enum GatewayType {
        /** exclusive gateway */
        EXCLUSIVE
    }

    /** ゲートウェイタイプ */
    private final GatewayType gatewayType;

    /**
     * ゲートウェイ定義を生成する。
     *
     * @param flowNodeId フローノードID
     * @param flowNodeName フローノード名
     * @param laneId レーンID
     * @param gatewayType ゲートウェイタイプ
     * @param sequenceFlows 自身を遷移元とするシーケンスフローのリスト
     */
    public Gateway(
            String flowNodeId,
            String flowNodeName,
            String laneId,
            String gatewayType,
            List<SequenceFlow> sequenceFlows) {
        super(flowNodeId, flowNodeName, laneId, sequenceFlows);
        this.gatewayType = GatewayType.valueOf(gatewayType);
    }

    /**
     * ゲートウェイタイプを取得する。
     *
     * @return ゲートウェイタイプ
     */
    public GatewayType getGatewayType() {
        return gatewayType;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * 自身を遷移元とするシーケンスフローリストから、条件にマッチするシーケンスフローを特定し、
     * その遷移先フローノードIDを次のフローノードIDとする。
     *
     * 全てのシーケンスフローをチェックした結果、遷移先が見つからなかった場合には{@link IllegalStateException}を送出する。
     */
    @Override
    public String getNextFlowNodeId(String instanceId, Map<String, ?> parameter) {
        for (SequenceFlow sequenceFlow : getSequenceFlows()) {
            if (sequenceFlow.canProceed(instanceId, parameter)) {
                return sequenceFlow.getTargetFlowNodeId();
            }
        }
        throw new IllegalStateException(String.format(
                "The sequence flow to proceed was not found. instance id = [%s], gateway id = [%s]",
                instanceId, getFlowNodeId()));
    }
}

