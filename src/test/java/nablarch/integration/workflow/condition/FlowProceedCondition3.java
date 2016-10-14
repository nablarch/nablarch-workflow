package nablarch.integration.workflow.condition;

import java.util.Map;

import nablarch.integration.workflow.definition.SequenceFlow;

public class FlowProceedCondition3 implements FlowProceedCondition {

    public final String arg1;
    public final String arg2;

    public FlowProceedCondition3(String arg1, String arg2) {
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
