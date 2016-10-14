package nablarch.integration.workflow;

import java.util.Map;

/**
 * ワークフローの管理を行うクラス。
 *
 * @author Ryo Tanaka
 * @since 1.4.2
 */
public final class WorkflowManager {

    /**
     * 指定されたワークフローIDのワークフローを開始する。
     *
     * @param workflowId 新規に開始するワークフローのワークフローID
     * @return 開始されたワークフローのインスタンスをあらわす {@link WorkflowInstance}
     * @throws IllegalArgumentException 指定されたワークフローIDに対応するワークフロー定義が存在しない場合。
     */
    public static WorkflowInstance startInstance(String workflowId) throws IllegalArgumentException {
        return getWorkflowInstanceFactory().start(workflowId);
    }

    /**
     * 指定されたワークフローIDのワークフローを開始する。
     *
     * @param workflowId 新規に開始するワークフローのワークフローID
     * @param parameter 開始イベントから、次のタスクまでワークフローを進行させる際に、各フローノードで使用するパラメータ
     * @return 開始されたワークフローのインスタンスをあらわす {@link WorkflowInstance}
     * @throws IllegalArgumentException 指定されたワークフローIDに対応するワークフロー定義が存在しない場合。
     */
    public static WorkflowInstance startInstance(String workflowId, Map<String, ?> parameter) throws IllegalArgumentException {
        return getWorkflowInstanceFactory().start(workflowId, parameter);
    }

    /**
     * すでに開始されているワークフローのインスタンスを取得する。
     *
     * @param instanceId 取得するワークフローインスタンスのインスタンスID
     * @return 取得されたワークフローインスタンス
     */
    public static WorkflowInstance findInstance(String instanceId) {
        return getWorkflowInstanceFactory().find(instanceId);
    }

    /**
     * 指定されたワークフローIDのワークフロー定義で、現在有効なバージョンを取得する。
     *
     * @param workflowId ワークフローID
     * @return 現在有効なバージョン
     * @throws IllegalArgumentException 指定されたワークフローIDが存在しない場合。
     */
    public static int getCurrentVersion(String workflowId) throws IllegalArgumentException {
        return WorkflowConfig.get().getWorkflowDefinitionHolder().getWorkflowDefinition(workflowId).getVersion();
    }

    /**
     * ワークフローインスタンスのファクトリクラスを取得する。
     *
     * @return ワークフローインスタンスのファクトリクラス
     */
    private static WorkflowInstanceFactory getWorkflowInstanceFactory() {
        return WorkflowConfig.get().getWorkflowInstanceFactory();
    }

    /**
     * 隠蔽コンストラクタ
     */
    private WorkflowManager() {
    }
}
