package nablarch.integration.workflow.condition;

/**
 * 指定のパラメータ値が、期待する値以上であることのチェックを行うフロー進行条件クラス。
 * <p/>
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class GeFlowProceedCondition extends NumberFlowProceedConditionSupport {

    /**
     * 期待値以上であることを比較するフロー進行条件クラスを生成する。
     *
     * @param paramKey 比較対象のパラメータを特定するためのキー値
     * @param expectedValue 期待する値
     */
    public GeFlowProceedCondition(String paramKey, String expectedValue) {
        super(paramKey, expectedValue);
    }

    /**
     * 数値の比較を行う。
     *
     * @param paramValue パラメーター値
     * @param expectedValue 期待する値
     * @return 比較結果
     */
    @Override
    protected boolean doComparison(long paramValue, long expectedValue) {
        return paramValue >= expectedValue;
    }
}
