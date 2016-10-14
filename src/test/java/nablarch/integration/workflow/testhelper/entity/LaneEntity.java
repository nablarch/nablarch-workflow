package nablarch.integration.workflow.testhelper.entity;

/**
 * ワークフローレーン定義エンティティ
 * <p/>
 * 本クラスは、イミュータブルオブジェクトのためフィールドを公開している。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class LaneEntity {

    /** プロセス定義 */
    public final WorkflowEntity workflowEntity;

    /** レーンID */
    public final String laneId;

    /** 名前 */
    public final String name;

    /**
     * レーンエンティティを生成する。
     *  @param workflowEntity プロセス定義
     * @param laneId レーンID
     * @param name 名前
     */
    public LaneEntity(WorkflowEntity workflowEntity, String laneId, String name) {
        this.workflowEntity = workflowEntity;
        this.laneId = laneId;
        this.name = name;
    }
}
