package nablarch.integration.workflow.testhelper.entity;

/**
 * プロセス定義エンティティ。
 * <p/>
 * 本クラスは、イミュータブルオブジェクトのため、フィールドをpublicで公開している。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class WorkflowEntity {

    /** バージョン */
    public final long version;

    /** プロセスID */
    public final String processId;

    /** プロセス名 */
    public final String processName;

    /** 有効日 */
    public String effectiveDate;

    /**
     * プロセス定義エンティティを生成する。
     *
     * @param processId プロセスID
     * @param version バージョン
     * @param processName プロセス名
     * @param effectiveDate 有効日(yyyyMMdd)
     */
    public WorkflowEntity(
            String processId,
            long version,
            String processName,
            String effectiveDate) {
        this.processId = processId;
        this.version = version;
        this.processName = processName;
        this.effectiveDate = effectiveDate;
    }
}
