package nablarch.integration.workflow.definition;

import java.util.List;
import java.util.Map;

/**
 * ワークフローのイベントノード情報を表すクラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class Event extends FlowNode {

    /**
     * イベントタイプを表す列挙型。
     *
     * @author hisaaki sioiri
     * @since 1.4.2
     */
    public enum EventType {
        /** 開始イベント */
        START,
        /** 中断イベント */
        TERMINATE
    }

    /** イベントタイプ */
    private final EventType eventType;

    /**
     * イベントノード情報を生成する。
     *
     * @param flowNodeId フローノードID
     * @param flowNodeName フローノード名
     * @param laneId レーンID
     * @param eventType イベントタイプ
     * @param sequenceFlows 自身をソースとするシーケンスフロー
     */
    public Event(
            String flowNodeId,
            String flowNodeName,
            String laneId,
            String eventType,
            List<SequenceFlow> sequenceFlows) {
        super(flowNodeId, flowNodeName, laneId, sequenceFlows);
        this.eventType = EventType.valueOf(eventType);
    }

    /**
     * イベントノタイプを取得する。
     *
     * @return イベントタイプ
     */
    public EventType getEventType() {
        return eventType;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * イベントタイプが停止({@link EventType#TERMINATE})の場合には、
     * 後続フローノードは存在しないためnullを返す。
     * それ以外の場合には、{@link FlowNode#getNextFlowNodeId(String, java.util.Map)}に処理を移譲する。
     */
    @Override
    public String getNextFlowNodeId(String instanceId, Map<String, ?> parameter) {
        if (eventType == EventType.TERMINATE) {
            return null;
        }
        return super.getNextFlowNodeId(instanceId, parameter);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * イベントタイプが停止({@link EventType#TERMINATE})の場合には、ワークフローインスタンス情報をすべて削除する。
     *
     * @param instanceId アクティブ化処理を行う対象のワークフローインスタンスID
     * @param parameter アクティブ化時に使用するパラメータ
     */
    @Override
    public void activate(String instanceId, Map<String, ?> parameter) {
        super.activate(instanceId, parameter);

        if (getEventType() == EventType.TERMINATE) {
            getWorkflowInstanceDao().deleteInstance(instanceId);
        }
    }
}

