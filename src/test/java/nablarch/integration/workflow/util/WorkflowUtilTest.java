package nablarch.integration.workflow.util;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nablarch.integration.workflow.condition.FlowProceedCondition;
import nablarch.integration.workflow.condition.FlowProceedCondition2;
import nablarch.integration.workflow.condition.FlowProceedCondition1;
import nablarch.integration.workflow.condition.FlowProceedCondition3;
import nablarch.integration.workflow.util.WorkflowUtil.ListFilter;

import org.junit.Test;

/**
 * {@link WorkflowUtil}のテストクラス。
 */
public class WorkflowUtilTest {


    /**
     * 指定の条件でフィルタされたリストが取得できること。
     */
    @Test
    public void testListFilter() {
        List<Integer> list = Arrays.asList(0, 49, 50, 51, 100);
        List<Integer> actual = WorkflowUtil.filterList(list, new ListFilter<Integer>() {
            @Override
            public boolean isMatch(Integer other) {
                return other > 50;
            }
        });

        List<Integer> expected = Arrays.asList(51, 100);

        assertThat(actual, is(expected));
    }

    /**
     * 空のListをフィルタした場合、サイズが空のリストが返却されること。
     */
    @Test
    public void testEmptyListFilter() {
        List<Object> actual = WorkflowUtil.filterList(new ArrayList<Object>(), new ListFilter<Object>() {
            @Override
            public boolean isMatch(Object other) {
                return other != null;
            }
        });
        assertThat(actual.size(), is(0));
    }

    /**
     * 条件にマッチする値がない場合、空のリストが返却されること。
     */
    @Test
    public void testNoMatchListFilter() {
        List<String> actual = WorkflowUtil.filterList(Arrays.asList("1", "2", "3"), new ListFilter<String>() {
            @Override
            public boolean isMatch(String other) {
                return other == null;
            }
        });
        assertThat(actual.size(), is(0));
    }

    /**
     * 条件にマッチする最初の要素が取得できること
     */
    @Test
    public void testFind() {
        int actual = WorkflowUtil.find(Arrays.asList(1, 2, 3), new ListFilter<Integer>() {
            @Override
            public boolean isMatch(Integer other) {
                return other > 1;
            }
        });
        assertThat(actual, is(2));
    }

    /**
     * 条件にマッチする要素が存在しない場合は、結果nullがかえされること
     */
    @Test
    public void testFindNotFound() {
        Integer actual = WorkflowUtil.find(Arrays.asList(1, 2, 3), new ListFilter<Integer>() {
            @Override
            public boolean isMatch(Integer other) {
                return other > 3;
            }
        });
        assertThat(actual, is(nullValue()));
    }

    /**
     * 要素が存在している場合trueが返却されること
     */
    @Test
    public void testContains() {
        boolean actual = WorkflowUtil.contains(Arrays.asList(1, 2, 3), new ListFilter<Integer>() {
            @Override
            public boolean isMatch(Integer other) {
                return other == 1;
            }
        });
        assertThat(actual, is(true));
    }

    /**
     * 要素が存在していない場合,falseが返却されること。
     *
     * @throws Exception
     */
    @Test
    public void testNotContains() throws Exception {
        boolean actual = WorkflowUtil.contains(Arrays.asList(1, 2, 3), new ListFilter<Integer>() {
            @Override
            public boolean isMatch(Integer other) {
                return other < 1;
            }
        });
        assertThat(actual, is(false));
    }


    /**
     * 指定したクラスのインスタンスが生成できること。
     */
    @Test
    public void testCreateInstanceNoArgumentConstructor() throws Exception {
        FlowProceedCondition instance = WorkflowUtil.createInstance(
                "nablarch.integration.workflow.condition.FlowProceedCondition1");
        assertThat(instance, instanceOf(FlowProceedCondition1.class));
    }

    /**
     * コンストラクタに引数が1つある場合でも、インスタンスが生成できること。
     */
    @Test
    public void testCreateInstanceOneArgumentConstructor() throws Exception {
        FlowProceedCondition instance = WorkflowUtil.createInstance(
                "nablarch.integration.workflow.condition.FlowProceedCondition2(12345)");
        assertThat(instance, instanceOf(FlowProceedCondition2.class));
        assertThat(((FlowProceedCondition2) instance).arg, is("12345"));
    }

    /**
     * コンストラクタに引数が複数ある場合でも、インスタンス生成できること
     */
    @Test
    public void testCreateInstanceMultiArgumentConstructor() throws Exception {
        FlowProceedCondition instance = WorkflowUtil.createInstance(
                "nablarch.integration.workflow.condition.FlowProceedCondition3(12345, 　 )");
        assertThat(instance, instanceOf(FlowProceedCondition3.class));
        assertThat(((FlowProceedCondition3) instance).arg1, is("12345"));
        assertThat(((FlowProceedCondition3) instance).arg2, is("　"));
    }

    /**
     * クラス名形式として不正な形式を指定した場合例外が発生すること
     */
    @Test
    public void testCreateInstanceInvalidClassname() {
        try {
            WorkflowUtil.createInstance("main()");
            fail("通らない");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("invalid class name pattern."));
        }
    }

    /**
     * 存在しないクラス名を指定した場合例外が発生すること
     */
    @Test
    public void testCreateInstanceClassNotFound() {
        try {
            WorkflowUtil.createInstance("notfoundclass");
            fail("通らない");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("failed to create instance."));
        }
    }

    /**
     * クラス名形式として不正な形式を指定した場合例外が発生すること
     */
    @Test
    public void testCreateInstanceInvalidParameter() {
        try {
            WorkflowUtil.createInstance("FlowProceedCondition3(12345, hogefuga, )");
            fail("通らない");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("failed to create instance."));
        }
    }

    /**
     * コンストラクタが見つけられない場合エラーとなること
     */
    @Test
    public void testCreateInstanceConstructorNotFound() {
        try {
            WorkflowUtil.createInstance(
                    "FlowProceedCondition4(12345, hogefuga )");
            fail("通らない");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("failed to create instance."));
        }
    }
}


