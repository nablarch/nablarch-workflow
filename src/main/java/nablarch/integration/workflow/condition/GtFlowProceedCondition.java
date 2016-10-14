package nablarch.integration.workflow.condition;

/**
 * 指定のパラメータ値が期待する値より大きいことをチェックするフロー進行条件クラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class GtFlowProceedCondition extends NumberFlowProceedConditionSupport {

    /**
     * 数値比較を行うフロー進行条件判定を生成する。
     *
     * @param paramKey 比較対象のパラメータを特定するためのキー値
     * @param expectedValue 期待する値
     */
    public GtFlowProceedCondition(String paramKey, String expectedValue) {
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
        return paramValue > expectedValue;
    }
}
