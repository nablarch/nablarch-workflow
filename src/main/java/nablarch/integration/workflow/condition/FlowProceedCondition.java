package nablarch.integration.workflow.condition;

import java.util.Map;

import nablarch.integration.workflow.definition.SequenceFlow;

/**
 * シーケンスフローの遷移判定を行うインタフェース。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public interface FlowProceedCondition {

    /**
     * シーケンスフローに従ってワークフローが進行可能か判定する。
     *
     * @param instanceId インスタンスID
     * @param param パラメータ
     * @param sequenceFlow 評価対象のシーケンスフロー
     * @return 遷移可能な場合はtrue
     */
    boolean isMatch(String instanceId, Map<String, ?> param, SequenceFlow sequenceFlow);
}
