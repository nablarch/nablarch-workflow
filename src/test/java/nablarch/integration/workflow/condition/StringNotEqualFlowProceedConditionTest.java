package nablarch.integration.workflow.condition;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import nablarch.integration.workflow.definition.SequenceFlow;

import org.junit.Before;
import org.junit.Test;

/**
 * {@link StringNotEqualFlowProceedCondition}のテストクラス。
 */
public class StringNotEqualFlowProceedConditionTest {
    
    /** テストで使用するダミーのシーケンスフロー */
    private SequenceFlow dummyFlow;

    @Before
    public void setUp() throws Exception {
        dummyFlow = new SequenceFlow("000", "フロー", "001", "002", null);
    }

    /**
     * パラメーターの値が期待値と一致しない場合
     */
    @Test
    public void testNotEq() throws Exception {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("var", "a");

        StringNotEqualFlowProceedCondition condition = new StringNotEqualFlowProceedCondition("var", "b");
        assertThat(condition.isMatch(null, param, dummyFlow), is(true));
    }

    /**
     * パラメーターの値が期待値と一致する場合
     *
     * @throws Exception
     */
    @Test
    public void testEq() throws Exception {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("var", "value");

        StringNotEqualFlowProceedCondition condition = new StringNotEqualFlowProceedCondition("var", "value");
        assertThat(condition.isMatch(null, param, dummyFlow), is(false));
    }

    /**
     * パラメーターに指定のキーの値が存在しない場合
     */
    @Test
    public void testParamNotFound() throws Exception {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("var1", "a");
        param.put("var3", "b");

        StringNotEqualFlowProceedCondition condition = new StringNotEqualFlowProceedCondition("var2", "b");
        assertThat(condition.isMatch(null, param, dummyFlow), is(false));
    }

    /**
     * パラメーターがnullの場合
     */
    @Test
    public void testParamIsNull() throws Exception {
        StringNotEqualFlowProceedCondition condition = new StringNotEqualFlowProceedCondition("var2", "b");
        assertThat(condition.isMatch(null, null, dummyFlow), is(false));
    }
}

