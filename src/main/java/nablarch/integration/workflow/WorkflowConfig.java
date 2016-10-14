package nablarch.integration.workflow;

import nablarch.core.repository.SystemRepository;

import nablarch.integration.workflow.dao.WorkflowInstanceDao;
import nablarch.integration.workflow.definition.WorkflowDefinitionHolder;

/**
 * ワークフローの設定情報クラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class WorkflowConfig {

    /** ワークフロー設定のキー値 */
    private static final String CONFIG_KEY = "workflowConfig";
    /** ワークフロー定義情報を保持するクラス */
    private WorkflowDefinitionHolder workflowDefinitionHolder;

    /** ワークフロー進行状況データベースに関するクラス */
    private WorkflowInstanceDao workflowInstanceDao;

    /** ワークフローインスタンスのファクトリクラス */
    private WorkflowInstanceFactory workflowInstanceFactory;

    /**
     * ワークフロー関連の設定情報を取得する。
     *
     * @return ワークフロー関連の設定情報
     */
    public static WorkflowConfig get() {
        return SystemRepository.get(CONFIG_KEY);
    }

    /**
     * ワークフロー定義情報を保持するクラスを設定する。
     *
     * @return ワークフロー定義情報を保持するクラスを取得する。
     */
    public WorkflowDefinitionHolder getWorkflowDefinitionHolder() {
        return workflowDefinitionHolder;
    }

    /**
     * ワークフロー定義情報を保持するクラスを設定する。
     *
     * @param workflowDefinitionHolder ワークフロー定義情報を保持するクラスを設定する。
     */
    public void setWorkflowDefinitionHolder(WorkflowDefinitionHolder workflowDefinitionHolder) {
        this.workflowDefinitionHolder = workflowDefinitionHolder;
    }

    /**
     * ワークフロー進行状況のデータベースアクセスクラスを取得する。
     *
     * @return プロセス進行状況DAO
     */
    public WorkflowInstanceDao getWorkflowInstanceDao() {
        return workflowInstanceDao;
    }

    /**
     * ワークフローの状態を管理するためのデータベースアクセスオブジェクトを設定する。
     *
     * @param workflowInstanceDao ワークフローの状態を管理するためのデータベースアクセスオブジェクト
     */
    public void setWorkflowInstanceDao(WorkflowInstanceDao workflowInstanceDao) {
        this.workflowInstanceDao = workflowInstanceDao;
    }

    /**
     * ワークフローインスタンスのファクトリクラスを取得する。
     *
     * @return ワークフローインスタンスのファクトリクラス
     */
    public WorkflowInstanceFactory getWorkflowInstanceFactory() {
        return workflowInstanceFactory;
    }

    /**
     * ワークフローインスタンスのファクトリクラスを設定する。
     *
     * @param workflowInstanceFactory ワークフローインスタンスのファクトリクラス
     */
    public void setWorkflowInstanceFactory(WorkflowInstanceFactory workflowInstanceFactory) {
        this.workflowInstanceFactory = workflowInstanceFactory;
    }
}
