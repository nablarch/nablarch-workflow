package nablarch.integration.workflow.definition;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.List;

import nablarch.integration.workflow.util.WorkflowUtil;

/**
 * ワークフローの定義情報を表すクラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class WorkflowDefinition {

    /** ワークフローID */
    private final String workflowId;

    /** バージョン番号 */
    private final int version;

    /** ワークフロー名 */
    private final String workflowName;

    /** 適用日 */
    private final String effectiveDate;

    /** 開始イベントを識別するID */
    private Event startEvent;

    /** シーケンスフロー定義情報 */
    private List<SequenceFlow> sequenceFlows;

    /** レーン定義情報 */
    private List<Lane> lanes;

    /** イベント定義情報 */
    private List<Event> events;

    /** タスク定義情報 */
    private List<Task> tasks;

    /** ゲートウェイ定義情報 */
    private List<Gateway> gateways;

    /** 中間イベント定義情報 */
    private List<BoundaryEvent> boundaryEvents;

    /**
     * ワークフロー定義情報を生成する。
     *
     * @param workflowId ワークフローID
     * @param version バージョン
     * @param workflowName ワークフロー名
     * @param effectiveDate 有効日
     */
    public WorkflowDefinition(
            String workflowId, int version, String workflowName, String effectiveDate) {
        this.workflowId = workflowId;
        this.version = version;
        this.workflowName = workflowName;
        this.effectiveDate = effectiveDate;
    }

    /**
     * 開始イベントを取得する。
     *
     * @return 開始イベント定義
     */
    private synchronized Event findStartEvent() {
        List<Event> filtered = WorkflowUtil.filterList(events,
                new WorkflowUtil.ListFilter<Event>() {
                    @Override
                    public boolean isMatch(Event other) {
                        return other.getEventType() == Event.EventType.START;
                    }
                });

        if (filtered.isEmpty()) {
            throw new IllegalStateException(
                    String.format("\"Start Event\" is not defined. \"Start Event\" must be a single definition."
                            + " workflow = [%s(%s)], version = [%d]", workflowId, workflowName, version));
        }
        if (filtered.size() >= 2) {
            throw new IllegalStateException(
                    String.format("\"Start Event\" is multiply defined. \"Start Event\" must be a single definition."
                            + " workflow = [%s(%s)], version = [%d]", workflowId, workflowName, version));
        }
        return filtered.get(0);
    }

    /**
     * ワークフローIDを取得する。
     *
     * @return ワークフローID
     */
    public String getWorkflowId() {
        return workflowId;
    }

    /**
     * バージョン番号を取得する。
     *
     * @return バージョン番号
     */
    public int getVersion() {
        return version;
    }

    /**
     * ワークフロー名を取得する。
     *
     * @return ワークフロー名
     */
    public String getWorkflowName() {
        return workflowName;
    }

    /**
     * 適用日を取得する。
     *
     * @return 適用日
     */
    public String getEffectiveDate() {
        return effectiveDate;
    }

    /**
     * タスクIDに対応したタスク定義を返却する。
     *
     * @param taskId タスクを識別するフローノードID
     * @return タスク定義
     * @throws IllegalArgumentException タスクIDに対応するタスク定義が存在しない場合
     */
    public Task findTask(String taskId) throws IllegalArgumentException {
        List<Task> definitions = WorkflowUtil.filterList(tasks, new FlowNodeFilter<Task>(taskId));
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException(String.format("task definition was not found. workflow id = [%s], version = [%s], task id = [%s]",
                    workflowId, version, taskId));
        }
        return definitions.get(0);
    }

    /**
     * シーケンスフロー定義を設定する。
     *
     * @param sequenceFlows シーケンスフロー定義
     */
    public void setSequenceFlows(List<SequenceFlow> sequenceFlows) {
        this.sequenceFlows = unmodifiableList(sequenceFlows);
    }

    /**
     * シーケンスフロー定義を返却する。
     *
     * @return シーケンスフロー定義
     */
    public List<SequenceFlow> getSequenceFlows() {
        return sequenceFlows;
    }

    /**
     * イベント定義を設定する。
     *
     * @param events イベント情報
     */
    public void setEvents(List<Event> events) {
        this.events = unmodifiableList(events);
    }

    /**
     * イベント定義を取得する。
     *
     * @return イベント定義
     */
    public List<Event> getEvents() {
        return events;
    }

    /**
     * タスク定義を設定する。
     *
     * @param tasks タスク定義情報
     */
    public void setTasks(List<Task> tasks) {
        this.tasks = unmodifiableList(tasks);
    }

    /**
     * タスク定義を返す。
     *
     * @return タスク定義
     */
    public List<Task> getTasks() {
        return unmodifiableList(tasks);
    }

    /**
     * ゲートウェイ定義を設定する。
     *
     * @param gateways ゲートウェイ定義情報
     */
    public void setGateways(List<Gateway> gateways) {
        this.gateways = unmodifiableList(gateways);
    }

    /**
     * ゲートウェイ定義を取得する
     *
     * @return ゲートウェイ定義
     */
    public List<Gateway> getGateways() {
        return gateways;
    }

    /**
     * 境界イベント定義を設定する。
     *
     * @param boundaryEvents 境界イベント定義
     */
    public void setBoundaryEvents(List<BoundaryEvent> boundaryEvents) {
        this.boundaryEvents = unmodifiableList(boundaryEvents);
    }

    /**
     * 境界イベント定義を取得する。
     *
     * @return 境界イベント定義
     */
    public List<BoundaryEvent> getBoundaryEvents() {
        return boundaryEvents;
    }

    /**
     * レーン定義を設定する。
     *
     * @param lanes レーン定義
     */
    public void setLanes(List<Lane> lanes) {
        this.lanes = unmodifiableList(lanes);
    }

    /**
     * レーン定義を返却する。
     *
     * @return レーン定義
     */
    public List<Lane> getLanes() {
        return lanes;
    }

    /**
     * このプロセスの開始イベントを取得する。
     *
     * @return 開始イベント
     */
    public Event getStartEvent() {
        if (startEvent == null) {
            startEvent = findStartEvent();
        }
        return startEvent;
    }

    /**
     * 指定されたフローノードIDの定義情報を検索する。
     *
     * @param flowNodeId 取得対象のフローノードID
     * @return フローノード定義情報
     */
    public FlowNode findFlowNode(String flowNodeId) {
        List<FlowNode> result = new ArrayList<FlowNode>();
        FlowNodeFilter<FlowNode> filter = new FlowNodeFilter<FlowNode>(flowNodeId);
        result.addAll(WorkflowUtil.filterList(tasks, filter));
        result.addAll(WorkflowUtil.filterList(gateways, filter));
        result.addAll(WorkflowUtil.filterList(events, filter));
        result.addAll(WorkflowUtil.filterList(boundaryEvents, filter));
        if (result.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("flow node definitions was not found. workflow id = [%s], version = [%s], flow node id = [%s]",
                            workflowId, version, flowNodeId));
        }
        return result.get(0);
    }

    /**
     * 指定されたトリガーIDに紐づく中間イベント一覧を取得する。
     *
     * @param triggerId トリガーID
     * @return トリガーIDに紐づく中間イベント一覧
     */
    public List<BoundaryEvent> getBoundaryEvent(final String triggerId) {
        return WorkflowUtil.filterList(boundaryEvents,
                new WorkflowUtil.ListFilter<BoundaryEvent>() {
                    @Override
                    public boolean isMatch(BoundaryEvent other) {
                        return other.getBoundaryEventTriggerId().equals(triggerId);
                    }
                });
    }

    /**
     * フローノード定義情報を検索するためのフィルター条件クラス。
     *
     * @author hisaaki sioiri
     * @since 1.4.2
     */
    private static final class FlowNodeFilter<T extends FlowNode> implements WorkflowUtil.ListFilter<T> {

        /**
         * 検索条件のフローノードID
         */
        private final String flowNodeId;

        /**
         * コンストラクタ。
         *
         * @param flowNodeId フローノードID
         */
        private FlowNodeFilter(String flowNodeId) {
            this.flowNodeId = flowNodeId;
        }

        @Override
        public boolean isMatch(T other) {
            return flowNodeId.equals(other.getFlowNodeId());
        }
    }
}

