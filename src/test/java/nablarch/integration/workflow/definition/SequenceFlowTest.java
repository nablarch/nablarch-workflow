package nablarch.integration.workflow.definition;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * {@link SequenceFlow}のテストクラス。
 */
@RunWith(Parameterized.class)
public class SequenceFlowTest {

    /** メッセージ */
    private final String message;

    /** テスト対象 */
    private final SequenceFlow sut;

    /** キー値 */
    private final String key;

    /** 値 */
    private final int value;

    /** 期待値 */
    private final boolean expected;

    @Parameterized.Parameters
    public static Collection parameters() {
        Object[][] parameters = {
                {
                        "遷移条件にマッチするので結果はtrue",
                        new SequenceFlow("id", "name", "src", "dst", "nablarch.integration.workflow.condition.EqFlowProceedCondition(var, 10)"),
                        "var",
                        10,
                        true
                },
                {
                        "遷移条件にマッチしないので結果はfalse",
                        new SequenceFlow("id", "name", "src", "dst", "nablarch.integration.workflow.condition.EqFlowProceedCondition(var, 10)"),
                        "var",
                        11,
                        false
                },
                {
                        "異なるコンディションで条件にマッチするので結果はtrue",
                        new SequenceFlow("id", "name", "src", "dst", "nablarch.integration.workflow.condition.GeFlowProceedCondition(var, 999999999)"),
                        "var",
                        999999999,
                        true
                },
                {
                        "遷移条件が設定されていないので結果はtrue",
                        new SequenceFlow("id", "name", "src", "dst", null),
                        "var",
                        11,
                        true
                }
        };
        return Arrays.asList(parameters);
    }


    public SequenceFlowTest(String message, SequenceFlow sut, String key, int value, boolean expected) {
        this.message = message;
        this.sut = sut;
        this.key = key;
        this.value = value;
        this.expected = expected;
    }

    @Test
    public void testCanProceed() throws Exception {
        Map<String, Integer> parameter = new HashMap<String, Integer>();
        parameter.put(key, value);
        assertThat(message, sut.canProceed("", parameter), is(expected));
    }
}

