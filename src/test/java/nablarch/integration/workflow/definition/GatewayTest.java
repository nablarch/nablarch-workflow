package nablarch.integration.workflow.definition;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * {@link Gateway}のテスト
 */
public class GatewayTest {

    /**
     * 遷移先フローノードIDが取得できること
     */
    @Test
    public void testGetNextFlowNode() throws Exception {

        SequenceFlow flow1 = new SequenceFlow("001", "1", "001", "002",
                "nablarch.integration.workflow.condition.EqFlowProceedCondition(var, 100)");
        SequenceFlow flow2 = new SequenceFlow("002", "1", "001", "003",
                "nablarch.integration.workflow.condition.NeFlowProceedCondition(var, 100)");
        List<SequenceFlow> sequenceFlows = new ArrayList<SequenceFlow>();
        sequenceFlows.add(flow1);
        sequenceFlows.add(flow2);

        Gateway sut = new Gateway("001", "1", "001", "EXCLUSIVE", sequenceFlows);
        Map<String, Integer> parameter = new HashMap<String, Integer>();
        parameter.put("var", 101);
        assertThat(sut.getNextFlowNodeId("11", parameter), is("003"));
    }

    /**
     * 遷移先が存在しない場合、例外が送出されること。
     */
    @Test
    public void testGetNextFlowNodeNotFound() throws Exception {
        SequenceFlow flow1 = new SequenceFlow("001", "1", "001", "002",
                "nablarch.integration.workflow.condition.EqFlowProceedCondition(var, 100)");
        SequenceFlow flow2 = new SequenceFlow("002", "1", "001", "003",
                "nablarch.integration.workflow.condition.GeFlowProceedCondition(var, 100)");
        List<SequenceFlow> sequenceFlows = new ArrayList<SequenceFlow>();
        sequenceFlows.add(flow1);
        sequenceFlows.add(flow2);

        Gateway sut = new Gateway("G01", "1", "001", "EXCLUSIVE", sequenceFlows);
        Map<String, Integer> parameter = new HashMap<String, Integer>();
        parameter.put("var", 99);
        try {
            sut.getNextFlowNodeId("11", parameter);
            fail("ここはとおらない");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("The sequence flow to proceed was not found. instance id = [11], gateway id = [G01]"));
        }
    }
}
