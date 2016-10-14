package nablarch.integration.workflow;

import java.util.Collections;
import java.util.Map;

import nablarch.integration.workflow.dao.WorkflowInstanceEntity;
import nablarch.integration.workflow.definition.FlowNode;
import nablarch.integration.workflow.dao.ActiveFlowNodeEntity;
import nablarch.integration.workflow.dao.WorkflowInstanceDao;
import nablarch.integration.workflow.definition.WorkflowDefinition;
import nablarch.integration.workflow.definition.WorkflowDefinitionHolder;

/**
 * {@link BasicWorkflowInstance} を生成するファクトリクラス。
 *
 * @author Ryo Tanaka
 * @since 1.4.2
 */
public class BasicWorkflowInstanceFactory implements WorkflowInstanceFactory {

    /**
     * {@inheritDoc}
     * <p/>
     * ワークフローの進行に利用するパラメータには、空のMapが利用される。
     */
    @Override
    public WorkflowInstance start(String workflowId) {
        return start(workflowId, Collections.<String, Object>emptyMap());
    }

    @Override
    public WorkflowInstance start(String workflowId, Map<String, ?> parameter) {
        WorkflowDefinition definition = getWorkflowDefinitionHolder().getWorkflowDefinition(workflowId);

        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        String instanceId = dao.createWorkflowInstance(definition.getWorkflowId(), definition.getVersion(), definition.getTasks());

        BasicWorkflowInstance started = new BasicWorkflowInstance(instanceId, definition, definition.getStartEvent());
        started.proceedToNextNode(parameter);

        return started;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * ワークフローがすでに完了している場合など、指定されたインスタンスIDを持つワークフローインスタンスが存在しない場合には、
     * ワークフローインスタンスは既に完了しているものとして判断し、完了状態をあらわすワークフローインスタンスを
     * 返却する。このインスタンスは、 {@link WorkflowInstance#isCompleted()} に対して常に {@code true} を返却し、
     * {@link WorkflowInstance#isActive(String)} は、常に {@code false} を返却する。
     * また、このインスタンスに対してタスクの進行や担当ユーザ/グループの割り当てを行うことはできない。（実行時例外が送出される。）
     */
    @Override
    public WorkflowInstance find(String instanceId) {
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        WorkflowInstanceEntity found = dao.findInstance(instanceId);

        if (found == null) {
            return new WorkflowInstance.CompletedWorkflowInstance(instanceId);
        }

        WorkflowDefinition definition = getWorkflowDefinitionHolder().getWorkflowDefinition(found.getWorkflowId(), found.getVersion());

        ActiveFlowNodeEntity active = dao.findActiveFlowNode(instanceId);
        FlowNode activeNode = definition.findFlowNode(active.getFlowNodeId());

        return new BasicWorkflowInstance(instanceId, definition, activeNode);
    }

    /**
     * ワークフローインスタンス系テーブルへのアクセスクラスを取得する。
     *
     * @return ワークフローインスタンスDAO
     */
    private static WorkflowInstanceDao getWorkflowInstanceDao() {
        return WorkflowConfig.get().getWorkflowInstanceDao();
    }

    /**
     * ワークフロー定義情報を保持するクラスを取得する。
     *
     * @return ワークフロー定義情報を保持するクラス
     */
    private static WorkflowDefinitionHolder getWorkflowDefinitionHolder() {
        return WorkflowConfig.get().getWorkflowDefinitionHolder();
    }
}
