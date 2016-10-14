package nablarch.integration.workflow.definition;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * {@link FlowNode}のテスト。
 */
public class FlowNodeTest {

    /**
     * 遷移先ノードが取得できること
     */
    @Test
    public void testGetNextFlowNode() throws Exception {

        List<SequenceFlow> sequenceFlows = new ArrayList<SequenceFlow>();
        sequenceFlows.add(new SequenceFlow("s01", "seq", "f01", "f02", null));

        FlowNode sut = new FlowNode("001", "name", "lane", sequenceFlows) {
        };
        assertThat(sut.getNextFlowNodeId("0001", null), is("f02"));
    }

    /**
     * 自ノードを遷移元とするシーケンスフローが存在しない場合例外が発生すること。
     */
    @Test
    public void testGetNextFlowNodeNull() throws Exception {
        FlowNode sut = new FlowNode("001", "name", "lane", null) {
        };
        try {
            sut.getNextFlowNodeId("0001", null);
            fail("とおらない");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is(
                    "there are multiple or empty sequence flow. single must be sequence flow. instanceId = [0001], flowNodeId = [001]"));
        }
    }

    /**
     * 自ノードを遷移元とするシーケンスフローが空の場合例外が発生すること
     */
    @Test
    public void testGetNextFlowNodeEmpty() throws Exception {
        FlowNode sut = new FlowNode("002", "name", "lane", Collections.<SequenceFlow>emptyList()) {

        };
        try {
            sut.getNextFlowNodeId("0003", null);
            fail("とおらない");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is(
                    "there are multiple or empty sequence flow. single must be sequence flow. instanceId = [0003], flowNodeId = [002]"));
        }
    }

    /**
     * 自ノードを遷移元とするシーケンスフローが複数存在する場合、例外が発生すること
     */
    @Test
    public void testGetNextFlowNodeMulti() throws Exception {
        List<SequenceFlow> sequenceFlows = new ArrayList<SequenceFlow>();
        sequenceFlows.add(new SequenceFlow("s01", "seq", "f01", "f02", null));
        sequenceFlows.add(new SequenceFlow("s02", "seq", "f01", "f03", null));

        FlowNode sut = new FlowNode("f01", "name", "lane", sequenceFlows) {
        };
        try {
            sut.getNextFlowNodeId("0003", null);
            fail("とおらない");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is(
                    "there are multiple or empty sequence flow. single must be sequence flow. instanceId = [0003], flowNodeId = [f01]"));
        }
    }
}

