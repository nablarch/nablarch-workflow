package nablarch.integration.workflow.condition;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nablarch.integration.workflow.dao.WorkflowInstanceDao;
import nablarch.integration.workflow.definition.SequenceFlow;
import nablarch.integration.workflow.definition.Task;
import nablarch.integration.workflow.testhelper.WorkflowTestRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import nablarch.core.repository.SystemRepository;

/**
 * {@link OrCompletionCondition}のテスト
 */
public class OrCompletionConditionTest {

    private static final List<SequenceFlow> SEQUENCE_FLOW_LIST = Collections.<SequenceFlow>emptyList();

    @ClassRule
    public static WorkflowTestRule workflowTestRule = new WorkflowTestRule();

    @Before
    public void setUp() throws Exception {
        workflowTestRule.getWorkflowDao().cleanupAll();
    }

    /**
     * 実行済みユーザ数(アサインユーザ数 - アクティブユーザ数)が閾値と同じ場合
     * 結果はtrueとなること。
     */
    @Test
    public void testUserEq() throws Exception {

        // ----- setup -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        workflowInstanceDao.saveAssignedUser("0000000001", "010", Arrays.asList(
                "1111111111", "2222222222", "3333333333"
        ));
        workflowInstanceDao.saveAssignedUser("0000000001", "011", Arrays.asList(
                "4444444444", "5555555555", "6666666666"
        ));
        workflowInstanceDao.saveAssignedUser("0000000001", "999", Arrays.asList(
                "7777777777", "2222222222", "1111111111"
        ));
        workflowInstanceDao.saveActiveUserTask("0000000001", "999", Arrays.asList(
                "2222222222"
        ));
        workflowTestRule.commit();

        // ----- execute -----
        Task task = new Task("999", "dummy", "", "PARALLEL",
                "nablarch.integration.workflow.condition.OrCompletionCondition(2)",
                SEQUENCE_FLOW_LIST);
        boolean actual = new OrCompletionCondition("2").isCompletedUserTask(null, "0000000001", task);

        // ----- assert -----
        assertThat("閾値と同数なので結果はtrue", actual, is(true));
    }

    /**
     * 実行済みユーザ数が閾値を超えた場合、結果はtrueとなること
     */
    @Test
    public void testUserGt() throws Exception{

        // ----- setup -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        workflowInstanceDao.saveAssignedUser("0000000001", "010", Arrays.asList(
                "1111111111", "2222222222", "3333333333"
        ));
        workflowInstanceDao.saveAssignedUser("0000000001", "011", Arrays.asList(
                "4444444444", "5555555555", "6666666666"
        ));
        workflowInstanceDao.saveAssignedUser("0000000001", "999", Arrays.asList(
                "7777777777", "2222222222", "1111111111"
        ));
        workflowInstanceDao.saveActiveUserTask("0000000001", "999", Arrays.asList(
                "2222222222"
        ));
        workflowTestRule.commit();

        // ----- execute -----
        Task task = new Task("999", "dummy", "", "PARALLEL",
                "nablarch.integration.workflow.condition.OrCompletionCondition(1)",
                SEQUENCE_FLOW_LIST);
        boolean actual = new OrCompletionCondition("1").isCompletedUserTask(null, "0000000001", task);

        // ----- assert -----
        assertThat("閾値を超えているので終了判定はtrue", actual, is(true));
    }

    /**
     * 実行済みユーザ数が閾値より小さい場合、結果はfalseとなること
     */
    @Test
    public void testUserLt() throws Exception{

        // ----- setup -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        workflowInstanceDao.saveAssignedUser("0000000001", "010", Arrays.asList(
                "1111111111", "2222222222", "3333333333"
        ));
        workflowInstanceDao.saveAssignedUser("0000000001", "011", Arrays.asList(
                "4444444444", "5555555555", "6666666666"
        ));
        workflowInstanceDao.saveAssignedUser("0000000001", "999", Arrays.asList(
                "7777777777", "2222222222", "1111111111"
        ));
        workflowInstanceDao.saveActiveUserTask("0000000001", "999", Arrays.asList(
                "2222222222"
        ));
        workflowTestRule.commit();

        // ----- execute -----
        Task task = new Task("999", "dummy", "", "PARALLEL",
                "nablarch.integration.workflow.condition.OrCompletionCondition(3)",
                SEQUENCE_FLOW_LIST);
        boolean actual = new OrCompletionCondition("3").isCompletedUserTask(null, "0000000001", task);

        // ----- assert -----
        assertThat("閾値を超えていないので結果はfalse", actual, is(false));
    }

    /**
     * 全てのユーザが実行済みの場合、閾値にかかわらずtrueとなること
     */
    @Test
    public void testUserAll() throws Exception{

        // ----- setup -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        workflowInstanceDao.saveAssignedUser("0000000001", "010", Arrays.asList(
                "1111111111", "2222222222", "3333333333"
        ));
        workflowInstanceDao.saveAssignedUser("0000000001", "011", Arrays.asList(
                "4444444444", "5555555555", "6666666666"
        ));
        workflowInstanceDao.saveAssignedUser("0000000001", "999", Arrays.asList(
                "7777777777", "2222222222", "1111111111"
        ));
        workflowInstanceDao.saveActiveUserTask("0000000000", "999", Arrays.asList(
                "2222222222"
        ));
        workflowInstanceDao.saveActiveUserTask("0000000002", "999", Arrays.asList(
                "2222222222"
        ));
        workflowTestRule.commit();

        // ----- execute -----
        Task task = new Task("999", "dummy", "", "PARALLEL",
                "nablarch.integration.workflow.condition.OrCompletionCondition(9999)",
                SEQUENCE_FLOW_LIST);
        boolean actual = new OrCompletionCondition("9999").isCompletedUserTask(null, "0000000001", task);

        // ----- assert -----
        assertThat("全てのユーザが実行済みなのでtrue", actual, is(true));
    }

    /**
     * 実行済みグループ数(アサイングループ数 - アクティブグループ数)が閾値と同じ場合
     * 結果はtrueとなること。
     */
    @Test
    public void testGroupEq() throws Exception {

        // ----- setup -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        workflowInstanceDao.saveAssignedGroup("0000000001", "010", Arrays.asList(
                "1111111111", "2222222222", "3333333333"
        ));
        workflowInstanceDao.saveAssignedGroup("0000000001", "011", Arrays.asList(
                "4444444444", "5555555555", "6666666666"
        ));
        workflowInstanceDao.saveAssignedGroup("0000000001", "999", Arrays.asList(
                "7777777777", "2222222222", "1111111111"
        ));
        workflowInstanceDao.saveActiveGroupTask("0000000001", "999", Arrays.asList("2222222222"));
        workflowTestRule.commit();

        // ----- execute -----
        Task task = new Task("999", "dummy", "001", "PARALLEL",
                "nablarch.integration.workflow.condition.OrCompletionCondition(2)",
                SEQUENCE_FLOW_LIST);
        boolean actual = new OrCompletionCondition("2").isCompletedGroupTask(null, "0000000001", task);

        // ----- assert -----
        assertThat("閾値と同数なので結果はtrue", actual, is(true));
    }

    /**
     * 実行済みグループ数が閾値を超えた場合、結果はtrueとなること
     */
    @Test
    public void testGroupGt() throws Exception{

        // ----- setup -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        workflowInstanceDao.saveAssignedGroup("0000000001", "010", Arrays.asList(
                "1111111111", "2222222222", "3333333333"
        ));
        workflowInstanceDao.saveAssignedGroup("0000000001", "011", Arrays.asList(
                "4444444444", "5555555555", "6666666666"
        ));
        workflowInstanceDao.saveAssignedGroup("0000000001", "999", Arrays.asList(
                "7777777777", "2222222222", "1111111111"
        ));

        workflowInstanceDao.saveActiveGroupTask("0000000001", "999", Arrays.asList("2222222222"));
        workflowTestRule.commit();

        // ----- execute -----
        Task task = new Task("999", "dummy", "001", "PARALLEL",
                "nablarch.integration.workflow.condition.OrCompletionCondition(1)",
                SEQUENCE_FLOW_LIST);
        boolean actual = new OrCompletionCondition("1").isCompletedGroupTask(null, "0000000001", task);

        // ----- assert -----
        assertThat("閾値を超えているので終了判定はtrue", actual, is(true));
    }

    /**
     * 実行済みグループ数が閾値より小さい場合、結果はfalseとなること
     */
    @Test
    public void testGroupLt() throws Exception{

        // ----- setup -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        workflowInstanceDao.saveAssignedGroup("0000000001", "010", Arrays.asList(
                "1111111111", "2222222222", "3333333333"
        ));
        workflowInstanceDao.saveAssignedGroup("0000000001", "011", Arrays.asList(
                "4444444444", "5555555555", "6666666666"
        ));
        workflowInstanceDao.saveAssignedGroup("0000000001", "999", Arrays.asList(
                "7777777777", "2222222222", "1111111111"
        ));
        workflowInstanceDao.saveActiveGroupTask("0000000001", "999", Arrays.asList("2222222222"));
        workflowTestRule.commit();

        // ----- execute -----
        Task task = new Task("999", "dummy", "", "PARALLEL",
                "nablarch.integration.workflow.condition.OrCompletionCondition(3)",
                SEQUENCE_FLOW_LIST);
        boolean actual = new OrCompletionCondition("3").isCompletedGroupTask(null, "0000000001", task);

        // ----- assert -----
        assertThat("閾値を超えていないので結果はfalse", actual, is(false));
    }

    /**
     * 全てのグループが実行済みの場合、閾値にかかわらずtrueとなること
     */
    @Test
    public void testGroupAll() throws Exception{

        // ----- setup -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        workflowInstanceDao.saveAssignedGroup("0000000001", "010", Arrays.asList(
                "1111111111", "2222222222", "3333333333"
        ));
        workflowInstanceDao.saveAssignedGroup("0000000001", "011", Arrays.asList(
                "4444444444", "5555555555", "6666666666"
        ));
        workflowInstanceDao.saveAssignedGroup("0000000001", "999", Arrays.asList(
                "7777777777", "2222222222", "1111111111"
        ));
        workflowInstanceDao.saveActiveGroupTask("0000000000", "999", Arrays.asList(
                "2222222222"
        ));
        workflowInstanceDao.saveActiveGroupTask("0000000002", "999", Arrays.asList(
                "2222222222"
        ));
        workflowTestRule.commit();

        // ----- execute -----
        Task task = new Task("999", "dummy", "", "PARALLEL",
                "nablarch.integration.workflow.condition.OrCompletionCondition(9999)",
                SEQUENCE_FLOW_LIST);
        boolean actual = new OrCompletionCondition("9999").isCompletedGroupTask(null, "0000000001", task);

        // ----- assert -----
        assertThat("全てのユーザが実行済みなのでtrue", actual, is(true));
    }

    /**
     * テスト対象のインスタンスを取得する。
     *
     * @return テスト対象オブジェクト
     */
    private WorkflowInstanceDao getWorkflowInstanceDao() {
        return SystemRepository.get("workflowInstanceDao");
    }
}
