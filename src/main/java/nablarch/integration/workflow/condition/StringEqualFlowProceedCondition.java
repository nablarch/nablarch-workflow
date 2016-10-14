package nablarch.integration.workflow.condition;

import java.util.Map;

import nablarch.integration.workflow.definition.SequenceFlow;

/**
 * 文字列が一致するかチェックを行うフロー進行条件クラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class StringEqualFlowProceedCondition implements FlowProceedCondition {

    /** パラメータ値を特定するためのキー値 */
    private final String paramKey;

    /** 期待する値 */
    private final String expectedValue;

    /**
     * コンストラクタ。
     *
     * @param paramKey 比較対象のパラメータを特定するためのキー値
     * @param expectedValue 期待する値
     */
    public StringEqualFlowProceedCondition(String paramKey, String expectedValue) {
        this.paramKey = paramKey;
        this.expectedValue = expectedValue;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * 期待する値と、コンストラクタで指定されたキー値のパラメータ値が一致する場合は、
     * 本シーケンスフローで遷移可能と判断する。
     */
    @Override
    public boolean isMatch(String instanceId, Map<String, ?> param, SequenceFlow sequenceFlow) {
        return (param != null) && expectedValue.equals(param.get(paramKey));
    }
}
