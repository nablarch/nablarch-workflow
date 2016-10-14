package nablarch.integration.workflow;

import java.util.Map;

/**
 * {@link WorkflowInstance} を生成するファクトリクラス。
 *
 * @author Ryo Tanaka
 * @since 1.4.2
 */
public interface WorkflowInstanceFactory {

    /**
     * 指定されたワークフローIDのワークフローを開始する。
     *
     * @param workflowId 新規に開始するワークフローのワークフローID
     * @return 開始されたワークフローのインスタンスをあらわす {@link WorkflowInstance}
     * @throws IllegalArgumentException 指定されたワークフローIDに対応するワークフロー定義が存在しない場合。
     */
    WorkflowInstance start(String workflowId) throws IllegalArgumentException;

    /**
     * 指定されたワークフローIDのワークフローを開始する。
     * <p/>
     * 開始されたワークフローインスタンスでは、開始イベントから進行して、最初に存在するタスクがアクティブフローノードとなっている。
     * 開始イベントから、次のタスクまで進行させる際には、 {@code parameter} が各フローノードでの処理に使用される。
     *
     * @param workflowId 新規に開始するワークフローのワークフローID
     * @param parameter 開始イベントから、次のタスクまでワークフローを進行させる際に、各フローノードで使用するパラメータ
     * @return 開始されたワークフローのインスタンスをあらわす {@link WorkflowInstance}
     * @throws IllegalArgumentException 指定されたワークフローIDに対応するワークフロー定義が存在しない場合。
     */
    WorkflowInstance start(String workflowId, Map<String, ?> parameter) throws IllegalArgumentException;

    /**
     * すでに開始されているワークフローのインスタンスを取得する。
     *
     * @param instanceId 取得するワークフローインスタンスのインスタンスID
     * @return 取得されたワークフローインスタンス
     */
    WorkflowInstance find(String instanceId);
}
