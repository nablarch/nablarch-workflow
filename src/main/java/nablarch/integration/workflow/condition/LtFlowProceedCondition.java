package nablarch.integration.workflow.condition;

/**
 * 指定のパラメータ値が期待する値より小さいことをチェックするフロー進行条件クラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class LtFlowProceedCondition extends NumberFlowProceedConditionSupport {

    /**
     * 期待値未満であることを判定するフロー進行条件判定を生成する。
     *
     * @param paramKey 比較対象のパラメータを特定するためのキー値
     * @param expectedValue 期待する値
     */
    public LtFlowProceedCondition(String paramKey, String expectedValue) {
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
        return paramValue < expectedValue;
    }
}
