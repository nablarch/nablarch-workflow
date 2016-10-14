package nablarch.integration.workflow.condition;

import java.util.Map;

import nablarch.integration.workflow.definition.SequenceFlow;

public class FlowProceedCondition4 implements FlowProceedCondition {

    public final String arg1;
    public final int arg2;

    public FlowProceedCondition4(String arg1, int arg2) {
        this.arg1 = arg1;
        this.arg2 = arg2;
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
