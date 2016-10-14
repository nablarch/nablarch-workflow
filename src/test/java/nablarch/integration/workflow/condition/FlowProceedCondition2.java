package nablarch.integration.workflow.condition;

import java.util.Map;

import nablarch.integration.workflow.definition.SequenceFlow;

public class FlowProceedCondition2 implements FlowProceedCondition {

    public final String arg;

    public FlowProceedCondition2(String arg) {
        this.arg = arg;
    }

    /**
     * このシーケンスフローで遷移可能か判定する。
     *
     *
     * @param instanceId
     * @param param パラメータ
     * @param sequenceFlow シーケンス
     * @return 遷移可能な場合はtrue
     */
    @Override
    public boolean isMatch(String instanceId, Map<String, ?> param, SequenceFlow sequenceFlow) {
        return false;
    }
}
