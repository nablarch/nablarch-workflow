package nablarch.integration.workflow.definition;

import java.util.List;

/**
 * 境界イベント定義を表すクラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class BoundaryEvent extends FlowNode {

    /** 境界イベントトリガーID */
    private final String boundaryEventTriggerId;

    /** 境界イベントトリガー名 */
    private final String boundaryEventTriggerName;

    /** 接続先タスクID */
    private final String attachedTaskId;

    /**
     * 境界イベント定義を生成する。
     *
     * @param flowNodeId フローノードID
     * @param flowNodeName フローノード名
     * @param laneId レーンID
     * @param boundaryEventTriggerId 境界イベントトリガーID
     * @param boundaryEventTriggerName 境界イベントトリガー名
     * @param attachedTaskId 接続先タスクID
     * @param sequenceFlows 自身を遷移元とするシーケンスフロー定義
     */
    public BoundaryEvent(
            String flowNodeId,
            String flowNodeName,
            String laneId,
            String boundaryEventTriggerId,
            String boundaryEventTriggerName,
            String attachedTaskId,
            List<SequenceFlow> sequenceFlows) {
        super(flowNodeId, flowNodeName, laneId, sequenceFlows);
        this.boundaryEventTriggerId = boundaryEventTriggerId;
        this.boundaryEventTriggerName = boundaryEventTriggerName;
        this.attachedTaskId = attachedTaskId;
    }

    /**
     * 境界イベントトリガーIDを取得する。
     *
     * @return 境界イベントトリガーID
     */
    public String getBoundaryEventTriggerId() {
        return boundaryEventTriggerId;
    }

    /**
     * 境界イベントトリガー名を取得する。
     *
     * @return 境界イベントトリガー名
     */
    public String getBoundaryEventTriggerName() {
        return boundaryEventTriggerName;
    }

    /**
     * 接続先タスクIDを取得する。
     *
     * @return 接続先タスクID
     */
    public String getAttachedTaskId() {
        return attachedTaskId;
    }
}

