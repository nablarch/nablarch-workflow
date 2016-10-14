package nablarch.integration.workflow.condition;

import java.util.Map;

import nablarch.integration.workflow.definition.SequenceFlow;

/**
 * 文字列が一致しないことをチェックするフロー進行条件クラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class StringNotEqualFlowProceedCondition implements FlowProceedCondition {

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
    public StringNotEqualFlowProceedCondition(String paramKey, String expectedValue) {
        this.paramKey = paramKey;
        this.expectedValue = expectedValue;
    }


    /**
     * {@inheritDoc}
     *
     * 期待する値と、コンストラクタで指定されたキー値のパラメータ値が一致しない場合は、
     * 本シーケンスフローで遷移可能と判断する。
     */
    @Override
    public boolean isMatch(String instanceId, Map<String, ?> param, SequenceFlow sequenceFlow) {
        if (param == null) {
            return false;
        }
        if (!param.containsKey(paramKey)) {
            return false;
        }
        return !expectedValue.equals(param.get(paramKey));
    }
}
