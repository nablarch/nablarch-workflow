package nablarch.integration.workflow.definition;

/**
 * レーン定義情報を表すクラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class Lane {

    /** レーンID */
    private final String laneId;

    /** レーン名称 */
    private final String laneName;

    /**
     * レーン定義情報を生成する。
     *
     * @param laneId レーンID
     * @param laneName レーン名称
     */
    public Lane(String laneId, String laneName) {
        this.laneId = laneId;
        this.laneName = laneName;
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
     * レーン名称を取得する。
     *
     * @return レーン名称
     */
    public String getLaneName() {
        return laneName;
    }
}
