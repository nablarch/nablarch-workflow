package nablarch.integration.workflow.condition;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nablarch.integration.workflow.definition.SequenceFlow;
import nablarch.integration.workflow.definition.Task;
import nablarch.integration.workflow.testhelper.WorkflowTestRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import nablarch.core.repository.SystemRepository;

import nablarch.integration.workflow.dao.WorkflowInstanceDao;

/**
 * {@link AllCompletionCondition}のテストクラス。
 */
public class AllCompletionConditionTest {

    private static final List<SequenceFlow> SEQUENCE_FLOWS = Collections.emptyList();

    @ClassRule
    public static WorkflowTestRule workflowTestRule = new WorkflowTestRule();

    private final AllCompletionCondition sut = new AllCompletionCondition();

    @Before
    public void setUp() throws Exception {
        workflowTestRule.getWorkflowDao().cleanupAll();
    }

    /**
     * ユーザが割り当てられている場合で、アクティブユーザタスクが存在しない場合は、結果がtrueとなること
     */
    @Test
    public void testActiveUserTaskEmpty() throws Exception {

        // ----- setup -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        workflowInstanceDao.saveAssignedUser("0000000002", "001", Arrays.asList("0000000001", "0000000002",
                "0000000003"));
        workflowInstanceDao.saveActiveUserTask("0000000001", "001", Arrays.asList("1111111111"));
        workflowInstanceDao.saveActiveUserTask("0000000003", "001", Arrays.asList("2222222222"));
        workflowTestRule.commit();

        // ----- execute -----
        boolean actual = sut.isCompletedUserTask(null, "0000000002",
                new Task("001", "", "", "SEQUENTIAL",
                        "nablarch.integration.workflow.condition.AllCompletionCondition",
                        SEQUENCE_FLOWS));

        // ----- assert -----
        assertThat(actual, is(true));
    }


    /**
     * ユーザが割り当てられている場合で、アクティブユーザタスクが存在する場合は、結果がfalseとなること
     */
    @Test
    public void testActiveUserTaskNotEmpty() throws Exception {

        // ----- setup -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        workflowInstanceDao.saveAssignedUser("0000000002", "003", Arrays.asList("0000000001", "1111111111",
                "0000000003"));
        workflowInstanceDao.saveActiveUserTask("0000000002", "003", "1111111111", 2);
        workflowTestRule.commit();

        // ----- execute -----
        boolean actual = sut.isCompletedUserTask(null, "0000000002",
                new Task("003", "", "", "SEQUENTIAL",
                        "nablarch.integration.workflow.condition.AllCompletionCondition",
                        SEQUENCE_FLOWS));

        // ----- assert -----
        assertThat(actual, is(false));
    }

    /**
     * グループが割り当てられている場合で、アクティブグループタスクが存在しない場合は結果がtrueとなること
     *
     * @throws Exception
     */
    @Test
    public void testActiveGroupEmpty() throws Exception {

        // ----- setup -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        workflowInstanceDao.saveAssignedGroup("0000000001", "001", Arrays.asList("0000000001"));
        workflowInstanceDao.saveAssignedSequentialGroup("0000000002", "002", Arrays.asList("0000000001", "0000000003",
                "0000000002"));
        workflowInstanceDao.saveAssignedGroup("0000000003", "001", Arrays.asList("0000000001"));
        workflowTestRule.commit();

        // ----- execute -----
        boolean actual = sut.isCompletedGroupTask(null, "0000000002",
                new Task("002", null, "001", "SEQUENTIAL",
                        "nablarch.integration.workflow.condition.AllCompletionCondition",
                        SEQUENCE_FLOWS));

        // ----- assert -----
        assertThat(actual, is(true));
    }

    /**
     * グループが割り当てられている場合で、アクティブグループタスクが存在している場合結果はfalseとなること
     *
     * @throws Exception
     */
    @Test
    public void testActiveGroupNotEmpty() throws Exception {

        // ----- setup -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        workflowInstanceDao.saveAssignedGroup("0000000001", "001", Arrays.asList("0000000001"));
        workflowInstanceDao.saveAssignedSequentialGroup("0000000002", "002", Arrays.asList("0000000001", "0000000003",
                "0000000002"));
        workflowInstanceDao.saveAssignedGroup("0000000003", "001", Arrays.asList("0000000001"));

        workflowInstanceDao.saveActiveGroupTask("0000000002", "002", "0000000003", 2);
        workflowTestRule.commit();

        // ----- execute -----
        boolean actual = sut.isCompletedGroupTask(null, "0000000002",
                new Task("002", null, "001", "SEQUENTIAL",
                        "nablarch.integration.workflow.condition.AllCompletionCondition",
                        SEQUENCE_FLOWS));

        // ----- assert -----
        assertThat(actual, is(false));
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
