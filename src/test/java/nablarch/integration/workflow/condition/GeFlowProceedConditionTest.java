package nablarch.integration.workflow.condition;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import nablarch.integration.workflow.definition.SequenceFlow;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * {@link GeFlowProceedCondition}のテスト。
 */
@RunWith(Parameterized.class)
public class GeFlowProceedConditionTest {

    private final String msg;

    private final Map<String, Object> param;

    private final String paramKey;

    private final String value;

    private final boolean expected;


    @Parameterized.Parameters
    public static Collection parameters() {
        Map<String, Object> defaultParam = new HashMap<String, Object>();
        defaultParam.put("intVal", 99999);
        defaultParam.put("longVal", Long.MAX_VALUE - 1);
        defaultParam.put("bigDecimalVal", new BigDecimal("100"));
        defaultParam.put("strVal", "12345");
        defaultParam.put("invalidNum", "a");
        defaultParam.put("nullVal", null);
        defaultParam.put("otherType", new ArrayList<String>());
        Object[][] objects = {
                {"intで期待する値より小さい場合", defaultParam, "intVal", "100000", false},
                {"intで期待する値と同じ値の場合", defaultParam, "intVal", "99999", true},
                {"intで期待する値より大きい場合", defaultParam, "intVal", "99998", true},
                {"longで期待する値より小さい場合", defaultParam, "longVal", String.valueOf(Long.MAX_VALUE), false},
                {"longで期待する値と同じ値の場合", defaultParam, "longVal", String.valueOf(Long.MAX_VALUE - 1), true},
                {"longで期待する値以上の場合", defaultParam, "longVal", String.valueOf(Long.MAX_VALUE - 2), true},
                {"BigDecimalで期待する値より小さい場合", defaultParam, "bigDecimalVal", "101", false},
                {"BigDecimalで期待する値と同じ値の場合", defaultParam, "bigDecimalVal", "100", true},
                {"BigDecimalで期待する値より大きい場合", defaultParam, "bigDecimalVal", "99", true},
                {"Stringで期待する値より小さい場合", defaultParam, "strVal", "12346", false},
                {"Stringで期待する値と同じ値の場合", defaultParam, "strVal", "12345", true},
                {"Stringで期待する値より大き場合", defaultParam, "strVal", "12344", true},
                {"Stringで数字ではない場合", defaultParam, "invalidNum", "1234", false},
                {"パラメータ自体がnullの場合", null, "key", "11111", false},
                {"パラメータがnullの場合は必ず一致しない", defaultParam, "nullVal", "11111", false},
                {"パラメータの型が許容する型意外の場合", defaultParam, "otherType", "11111", false},
        };
        return Arrays.asList(
                objects
        );
    }

    public GeFlowProceedConditionTest(String msg, Map<String, Object> param, String paramKey, String value,
            boolean expected) {
        this.msg = msg;
        this.param = param;
        this.paramKey = paramKey;
        this.value = value;
        this.expected = expected;
    }

    @Test
    public void testIsMatch() throws Exception {
        SequenceFlow dummyFlow = new SequenceFlow("000", "フロー", "001", "002", null);
        GeFlowProceedCondition sut = new GeFlowProceedCondition(paramKey, value);
        assertThat(msg, sut.isMatch(null, param, dummyFlow), is(expected));
    }
}
