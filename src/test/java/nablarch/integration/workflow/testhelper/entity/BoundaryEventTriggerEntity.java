package nablarch.integration.workflow.testhelper.entity;

/**
 * イベントエンティティ。
 * <p/>
 * 本クラスは、イミュータブルオブジェクトのためフィールドを公開している。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class BoundaryEventTriggerEntity {

    /** プロセス定義 */
    public final WorkflowEntity workflowEntity;

    /** イベントトリガーID */
    public final String eventTriggerId;

    /** イベントトリガー名 */
    public final String eventTriggerName;

    /**
     * イベントエンティティを生成する。
     *
     * @param workflowEntity プロセスエンティティ
     * @param eventTriggerId イベントトリガーID
     * @param eventTriggerName イベントトリガー名
     */
    public BoundaryEventTriggerEntity(WorkflowEntity workflowEntity, String eventTriggerId, String eventTriggerName) {
        this.workflowEntity = workflowEntity;
        this.eventTriggerId = eventTriggerId;
        this.eventTriggerName = eventTriggerName;
    }
}
