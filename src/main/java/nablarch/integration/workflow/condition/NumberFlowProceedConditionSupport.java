package nablarch.integration.workflow.condition;

import java.util.Map;

import nablarch.integration.workflow.definition.SequenceFlow;

/**
 * 数値比較を行うフロー進行条件判定クラスのサポートクラス。
 * <p/>
 * 本クラスで許容する値の範囲は、{@link Long#MIN_VALUE}から{@link Long#MAX_VALUE}までである。<br/>
 * <p/>
 * 比較対象のパラメータのオブジェクトが、数値型の場合には{@link Number#longValue()}を使用して、
 * 強制的にlongに変換し比較を行う。<br/>
 * 値が文字列型({@link String})の場合には、{@link Long#valueOf(String)}を使用して、
 * longに変換し比較を行う。longへの変換に失敗した場合はfalseを返す。<br/>
 * 上記に該当しない方の場合には、値は期待する値とは一致しないとし{@link FlowProceedCondition#isMatch(String, java.util.Map, SequenceFlow)}はfalseを返す。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public abstract class NumberFlowProceedConditionSupport implements FlowProceedCondition {

    /** パラメーターのキー値 */
    private final String paramKey;

    /** 期待する値 */
    private final long expectedValue;

    /**
     * 数値比較を行うフロー進行条件判定を生成する。
     *
     * @param paramKey 比較対象のパラメータを特定するためのキー値
     * @param expectedValue 期待する値
     */
    protected NumberFlowProceedConditionSupport(String paramKey, String expectedValue) {
        this.paramKey = paramKey;
        this.expectedValue = toLong(expectedValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMatch(String instanceId, Map<String, ?> param, SequenceFlow sequenceFlow) {
        if (param == null) {
            return false;
        }
        Object value = param.get(paramKey);

        long paramValue;
        if (value instanceof Number) {
            paramValue = ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                paramValue = toLong((String) value);
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            return false;
        }
        return doComparison(paramValue, expectedValue);
    }

    /**
     * 数値の比較を行う。
     *
     * @param paramValue パラメーター値
     * @param expectedValue 期待する値
     * @return 比較結果
     */
    protected abstract boolean doComparison(long paramValue, long expectedValue);

    /**
     * 引数の文字列をlongに変換する。
     * <p/>
     * longへの変換に失敗した場合は、{@link java.lang.NumberFormatException}をそ送出する。
     *
     * @param value 文字列
     * @return 変換結果
     */
    private static long toLong(String value) {
        return Long.valueOf(value);
    }
}
