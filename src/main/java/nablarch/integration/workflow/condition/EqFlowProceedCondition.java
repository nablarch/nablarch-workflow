package nablarch.integration.workflow.condition;

/**
 * 指定のパラメータ値が、期待する値を一致するかチェックを行うフロー進行条件クラス。
 * <p/>
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class EqFlowProceedCondition extends NumberFlowProceedConditionSupport {

    /**
     * 期待値と一致することを判定するフロー進行条件クラスを生成する。
     *
     * @param paramKey 比較対象のパラメータを特定するためのキー値
     * @param expectedValue 期待する値
     */
    public EqFlowProceedCondition(String paramKey, String expectedValue) {
        super(paramKey, expectedValue);
    }

    /**
     * パラメータ値と期待する値が一致していることを比較する。
     *
     * @param paramValue パラメーター値
     * @param expectedValue 期待する値
     * @return パラメーター値と期待する値が一致している場合はtrue
     */
    @Override
    protected boolean doComparison(long paramValue, long expectedValue) {
        return paramValue == expectedValue;
    }
}

