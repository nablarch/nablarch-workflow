package nablarch.integration.workflow.dao;

/**
 * ワークフローインスタンスエンティティ。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class WorkflowInstanceEntity {

    /** インスタンスID */
    private final String instanceId;

    /** ワークフローID */
    private final String workflowId;

    /** バージョン */
    private final long version;

    /**
     * ワークフローインスタンスエンティティを生成する。
     *
     * @param instanceId インスタンスID
     * @param workflowId ワークフローID
     * @param version バージョン
     */
    public WorkflowInstanceEntity(String instanceId, String workflowId, long version) {
        this.instanceId = instanceId;
        this.workflowId = workflowId;
        this.version = version;
    }

    /**
     * インスタンスIDを取得する。
     *
     * @return インスタンスID
     */
    public String getInstanceId() {
        return instanceId;
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
     * バージョンを取得する。
     *
     * @return バージョン
     */
    public long getVersion() {
        return version;
    }
}
