package nablarch.integration.workflow.definition;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import nablarch.integration.workflow.WorkflowConfig;
import nablarch.integration.workflow.dao.WorkflowInstanceDao;

/**
 * フローノード定義を表すクラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public abstract class FlowNode {

    /** フローノードID */
    private final String flowNodeId;

    /** フローノード名 */
    private final String flowNodeName;

    /** レーンID */
    private final String laneId;

    /** シーケンスフロー(自身をソースとするシーケンスフローのリスト) */
    private final List<SequenceFlow> sequenceFlows;

    /**
     * フローノード定義を表すクラス。
     *
     * @param flowNodeId フローノードID
     * @param flowNodeName フローノード名
     * @param laneId レーンID
     * @param sequenceFlows 自身をソースとするシーケンスフローのリスト
     */
    public FlowNode(
            String flowNodeId, String flowNodeName, String laneId, List<SequenceFlow> sequenceFlows) {
        this.flowNodeId = flowNodeId;
        this.flowNodeName = flowNodeName;
        this.laneId = laneId;
        this.sequenceFlows = ((sequenceFlows == null) ? null : Collections.unmodifiableList(sequenceFlows));
    }

    /**
     * フローノードIDを取得する。
     *
     * @return フローノードID
     */
    public String getFlowNodeId() {
        return flowNodeId;
    }

    /**
     * フローノード名を取得する。
     *
     * @return フローノード名
     */
    public String getFlowNodeName() {
        return flowNodeName;
    }

    /**
     * レーンIDを取得する。
     *
     * @return レーンID
     */
    public String getLaneId() {
        return laneId;
    }

    /**
     * 自身をソースとするシーケンスフローのリスト。
     *
     * @return 自身をソースとするシーケンスフロー
     */
    public List<SequenceFlow> getSequenceFlows() {
        return sequenceFlows;
    }

    /**
     * 遷移先のフローノードを取得する。
     * <p/>
     * 自身を遷移元とするシーケンスフローが1つしか存在しない場合は、
     * そのシーケンスフローの遷移先を遷移先のフローノードとして返却する。
     * <p/>
     * シーケンスフローが複数存在している場合には、本メソッドでは{@link java.lang.IllegalStateException}を送出する。
     * 複数の遷移先をサポートする必要がある場合には、本クラスの具象クラス側にて実装を行う必要がある。
     *
     * @param instanceId インスタンスID
     * @param parameter パラメータ
     * @return 遷移先のフローノードID
     */
    public String getNextFlowNodeId(String instanceId, Map<String, ?> parameter) {
        if ((sequenceFlows == null) || (sequenceFlows.size() != 1)) {
            throw new IllegalStateException(String.format(
                    "there are multiple or empty sequence flow. single must be sequence flow. instanceId = [%s], flowNodeId = [%s]",
                    instanceId, flowNodeId));
        }
        return sequenceFlows.get(0).getTargetFlowNodeId();
    }

    /**
     * フローノードのアクティブ化処理を行う。
     *
     * @param instanceId アクティブ化処理を行う対象のワークフローインスタンスID
     * @param parameter アクティブ化時に使用するパラメータ
     */
    public void activate(String instanceId, Map<String, ?> parameter) {
    }

    /**
     * ユーザタスクとして、フローノード上での処理を行う。フローノードでの処理が完了し、ワークフローを次のノードに進めてよい場合は {@code true} を返却する。
     *
     * @param instanceId 対象のワークフローインスタンスID
     * @param parameter ワークフローの進行時に使用するパラメータ
     * @param executor このノードでの処理を実行しているユーザ
     * @return 次のフローノードにワークフローを進行させて良い場合は {@code true}
     */
    public boolean processNodeByUser(String instanceId, Map<String, ?> parameter, String executor) {
        return true;
    }

    /**
     * グループタスクとして、フローノード上での処理を行う。フローノードでの処理が完了し、ワークフローを次のノードに進めてよい場合は {@code true} を返却する。
     *
     * @param instanceId 対象のワークフローインスタンスID
     * @param parameter ワークフローの進行時に使用するパラメータ
     * @param executor このノードでの処理を実行しているグループ
     * @return 次のフローノードにワークフローを進行させて良い場合は {@code true}
     */
    public boolean processNodeByGroup(String instanceId, Map<String, ?> parameter, String executor) {
        return true;
    }

    /**
     * ワークフローインスタンス系テーブルへのアクセスクラスを取得する。
     *
     * @return ワークフローインスタンスDAO
     */
    protected WorkflowInstanceDao getWorkflowInstanceDao() {
        return WorkflowConfig.get().getWorkflowInstanceDao();
    }
}

