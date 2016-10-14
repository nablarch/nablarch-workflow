package nablarch.integration.workflow.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import nablarch.core.ThreadContext;
import nablarch.core.db.statement.SqlResultSet;

import nablarch.integration.workflow.WorkflowInstance;
import nablarch.integration.workflow.WorkflowManager;
import nablarch.integration.workflow.testhelper.WorkflowDbAccessSupport;
import nablarch.integration.workflow.testhelper.WorkflowTestRule;
import nablarch.integration.workflow.testhelper.entity.EventEntity;
import nablarch.integration.workflow.testhelper.entity.GatewayEntity;
import nablarch.integration.workflow.testhelper.entity.LaneEntity;
import nablarch.integration.workflow.testhelper.entity.SequenceFlowEntity;
import nablarch.integration.workflow.testhelper.entity.TaskEntity;
import nablarch.integration.workflow.testhelper.entity.WorkflowEntity;

/**
 * 少し複雑なワークフロープロセスを使用した機能結合テスト。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class LittleComplexityWorkflowIntegrationTest {

    @ClassRule
    public static WorkflowTestRule workflowTestRule = new WorkflowTestRule(true);

    /** テストで使用するプロセスID */
    private static final String TEST_PROCESS_ID = "P0002";

    /** お客様ID */
    private static final String CLIENT_USER = "0000000001";

    /** システム処理ユーザID */
    private static final String SYSTEM_USER = "S000000001";

    /** 判定者 */
    private static final String JUDGE_USER = "8000000001";

    /** 上位判定者 */
    private static final String UPPER_JUDGE_USER = "8000000002";

    /** 調査担当者 */
    private static final String CHECK_USER_1 = "9000000001";

    private static final String CHECK_USER_2 = "9000000002";

    private static final String CHECK_USER_3 = "9000000003";

    private static final String CHECK_USER_4 = "9000000004";

    /**
     * テストデータの準備
     */
    @BeforeClass
    public static void setUpClass() throws Exception {
        WorkflowDbAccessSupport workflowDao = workflowTestRule.getWorkflowDao();
        workflowDao.cleanupAll();

        WorkflowEntity workflow = new WorkflowEntity(TEST_PROCESS_ID, 2, "交通費申請", "20110101");
        workflowDao.insertWorkflowEntity(workflow);

        LaneEntity lane1 = new LaneEntity(workflow, "l01", "お客様");
        LaneEntity lane2 = new LaneEntity(workflow, "l02", "システム処理");
        LaneEntity lane3 = new LaneEntity(workflow, "l03", "判定者");
        LaneEntity lane4 = new LaneEntity(workflow, "l04", "調査担当者");
        workflowDao.insertLaneEntity(lane1, lane2, lane3, lane4);

        EventEntity e01 = new EventEntity("e01", lane1, "Start", "START");
        EventEntity e02 = new EventEntity("e02", lane4, "TerminateEndEvent", "TERMINATE");
        EventEntity e03 = new EventEntity("e03", lane1, "TerminateEndEvent", "TERMINATE");
        workflowDao.insertEventEntity(e01, e02, e03);

        TaskEntity t01 = new TaskEntity("t01", lane2, "内部自動審査", "NONE", "");
        TaskEntity t02 = new TaskEntity("t02", lane4, "調査", "PARALLEL",
                "nablarch.integration.workflow.condition.OrCompletionCondition(2)");
        TaskEntity t03 = new TaskEntity("t03", lane3, "判定", "NONE", "");
        TaskEntity t04 = new TaskEntity("t04", lane3, "上位判定", "NONE", "");
        TaskEntity t05 = new TaskEntity("t05", lane4, "実行", "SEQUENTIAL",
                "nablarch.integration.workflow.condition.AllCompletionCondition");
        workflowDao.insertTaskEntity(t01, t02, t03, t04, t05);

        GatewayEntity g01 = new GatewayEntity("g01", lane2, "Exclusive Gateway", "EXCLUSIVE");
        GatewayEntity g02 = new GatewayEntity("g02", lane4, "Exclusive Gateway", "EXCLUSIVE");
        GatewayEntity g03 = new GatewayEntity("g03", lane3, "Exclusive Gateway", "EXCLUSIVE");
        GatewayEntity g04 = new GatewayEntity("g04", lane3, "Exclusive Gateway", "EXCLUSIVE");
        GatewayEntity g05 = new GatewayEntity("g05", lane3, "Exclusive Gateway", "EXCLUSIVE");
        workflowDao.insertGatewayEntity(g01, g02, g03, g04, g05);

        SequenceFlowEntity f01 = new SequenceFlowEntity(workflow, "f01", "", "e01", "t01", "");
        SequenceFlowEntity f02 = new SequenceFlowEntity(workflow, "f02", "", "t01", "g01", "");
        SequenceFlowEntity f03 = new SequenceFlowEntity(workflow, "f03", "却下", "g01", "e03",
                "nablarch.integration.workflow.integration.CustomFlowCondition");
        SequenceFlowEntity f04 = new SequenceFlowEntity(workflow, "f04", "審査通過", "g01", "t02",
                "nablarch.integration.workflow.integration.CustomFlowCondition");
        SequenceFlowEntity f05 = new SequenceFlowEntity(workflow, "f05", "", "t02", "g02", "");
        SequenceFlowEntity f06 = new SequenceFlowEntity(workflow, "f06", "却下", "g02", "e03",
                "nablarch.integration.workflow.condition.EqFlowProceedCondition(var, 99)");
        SequenceFlowEntity f07 = new SequenceFlowEntity(workflow, "f07", "調査完了", "g02", "t03",
                "nablarch.integration.workflow.condition.EqFlowProceedCondition(var, 1)");
        SequenceFlowEntity f08 = new SequenceFlowEntity(workflow, "f08", "", "t03", "g03", "");
        SequenceFlowEntity f09 = new SequenceFlowEntity(workflow, "f09", "却下", "g03", "e03",
                "nablarch.integration.workflow.condition.EqFlowProceedCondition(var, 99)");
        SequenceFlowEntity f10 = new SequenceFlowEntity(workflow, "f10", "審査通過", "g03", "g04",
                "nablarch.integration.workflow.condition.EqFlowProceedCondition(var, 1)");
        SequenceFlowEntity f11 = new SequenceFlowEntity(workflow, "f11", "差戻し", "g03", "t02",
                "nablarch.integration.workflow.condition.EqFlowProceedCondition(var, 2)");
        SequenceFlowEntity f12 = new SequenceFlowEntity(workflow, "f12", "xxx円以上", "g04", "t04",
                "nablarch.integration.workflow.condition.GeFlowProceedCondition(amount, 100000)");
        SequenceFlowEntity f13 = new SequenceFlowEntity(workflow, "f13", "xxx円未満", "g04", "t05",
                "nablarch.integration.workflow.condition.LtFlowProceedCondition(amount, 100000)");
        SequenceFlowEntity f14 = new SequenceFlowEntity(workflow, "f14", "", "t04", "g05", "");
        SequenceFlowEntity f15 = new SequenceFlowEntity(workflow, "f15", "承認", "g05", "t05",
                "nablarch.integration.workflow.condition.EqFlowProceedCondition(var, 1)");
        SequenceFlowEntity f16 = new SequenceFlowEntity(workflow, "f16", "却下", "g05", "e03",
                "nablarch.integration.workflow.condition.EqFlowProceedCondition(var, 99)");
        SequenceFlowEntity f17 = new SequenceFlowEntity(workflow, "f17", "", "t05", "e02", "");
        workflowDao.insertSequenceEntity(
                f01, f02, f03, f04, f05, f06, f07, f08, f09, f10, f11, f12, f13, f14, f15, f16, f17);

        workflowTestRule.reloadProcessDefinitions();
    }

    /**
     * テストケースごとインスタンス系テーブルの状態は初期化する。
     */
    @Before
    public void setUp() throws Exception {
        workflowTestRule.getWorkflowDao().cleanup(
                "WF_ACTIVE_GROUP_TASK",
                "WF_ACTIVE_USER_TASK",
                "WF_ACTIVE_FLOW_NODE",
                "WF_INSTANCE_FLOW_NODE",
                "WF_INSTANCE"
        );
    }

    @After
    public void tearDown() throws Exception {
        ThreadContext.clear();
    }

    /**
     * 全てのタスクが実行され完了するパターン
     */
    @Test
    public void testNormalEnd() throws Exception {
        // 開始
        WorkflowInstance workflow = startWorkflow();
        assertStartWorkflow(workflow);

        // 自動審査OK
        workflow = autoJudgeOk(workflow.getInstanceId());
        assertAutoJudgeOk(workflow);

        // 調査OK1人目
        workflow = checkOkOne(workflow.getInstanceId());
        assertCheckOkOne(workflow);

        // 調査OK2人目
        workflow = checkOkTwo(workflow.getInstanceId());
        assertCheckOkTow(workflow);

        // 判定OK→上位判定へ
        workflow = judgeOkToUpperJudge(workflow.getInstanceId());
        assertJudgeOkToUpperJudge(workflow);

        // 上位判定→OK
        workflow = upperJudgeOk(workflow.getInstanceId());
        assertUpperJudgeOk(workflow);

        // 実行1
        workflow = execute(workflow.getInstanceId(), CHECK_USER_4);
        assertExecute(workflow, CHECK_USER_1);

        // 実行2
        workflow = execute(workflow.getInstanceId(), CHECK_USER_1);
        assertExecute(workflow, CHECK_USER_3);

        // 実行3
        workflow = execute(workflow.getInstanceId(), CHECK_USER_3);
        assertExecute(workflow, CHECK_USER_2);

        // 実行4
        workflow = execute(workflow.getInstanceId(), CHECK_USER_2);
        assertCompleteWorkflow(workflow);
    }


    /**
     * ワークフローを開始させる。
     *
     * @return
     */
    private WorkflowInstance startWorkflow() {
        ThreadContext.setUserId(CLIENT_USER);
        WorkflowInstance workflow = WorkflowManager.startInstance(TEST_PROCESS_ID);

        // 次のタスクに人をアサイン
        workflow.assignUserToLane("l03", JUDGE_USER);
        workflow.assignUser("t01", SYSTEM_USER);
        workflowTestRule.commit();
        return workflow;
    }

    /**
     * 自動審査OK
     * @param instanceId
     */
    private WorkflowInstance autoJudgeOk(String instanceId) {
        ThreadContext.setUserId(SYSTEM_USER);

        CustomFlowCondition.TestEntity entity = new CustomFlowCondition.TestEntity(
                9999, CustomFlowCondition.TestEntity.UserType.NORMAL);
        Map<String, CustomFlowCondition.TestEntity> parameter = new HashMap<String, CustomFlowCondition.TestEntity>();
        parameter.put("entity", entity);

        WorkflowInstance workflow = WorkflowManager.findInstance(instanceId);
        workflow.assignUsers("t02", Arrays.asList(
                CHECK_USER_1, CHECK_USER_3, CHECK_USER_4));

        workflow.completeUserTask(parameter);

        workflowTestRule.commit();
        return workflow;
    }

    /**
     * 調査OK1人目
     * @param instanceId
     */
    private WorkflowInstance checkOkOne(String instanceId) {
        ThreadContext.setUserId(CHECK_USER_3);

        Map<String, Integer> parameter = new HashMap<String, Integer>();
        parameter.put("var", 1);

        WorkflowInstance workflow = WorkflowManager.findInstance(instanceId);
        workflow.completeUserTask(parameter);

        workflowTestRule.commit();
        return workflow;
    }

    /**
     * 調査OK2人目
     *
     * @param instanceId@return
     */
    private WorkflowInstance checkOkTwo(String instanceId) {
        ThreadContext.setUserId(CHECK_USER_1);

        WorkflowInstance workflow = WorkflowManager.findInstance(instanceId);
        Map<String, Integer> parameter = new HashMap<String, Integer>();
        parameter.put("var", 1);

        workflow.completeUserTask(parameter);

        workflowTestRule.commit();
        return workflow;
    }

    /**
     * 判定OK→上位判定へ
     *
     * @param instanceId@return
     */
    private WorkflowInstance judgeOkToUpperJudge(String instanceId) {
        ThreadContext.setUserId(JUDGE_USER);

        WorkflowInstance workflow = WorkflowManager.findInstance(instanceId);
        workflow.assignUser("t04", UPPER_JUDGE_USER);

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("var", 1);
        parameters.put("amount", 100000);

        workflow.completeUserTask(parameters);

        workflowTestRule.commit();

        return workflow;
    }

    /**
     * 上位判定を実施→OK
     */
    private WorkflowInstance upperJudgeOk(String instanceId) {
        ThreadContext.setUserId(UPPER_JUDGE_USER);

        WorkflowInstance workflow = WorkflowManager.findInstance(instanceId);

        workflow.assignUsers("t05", Arrays.asList(
                CHECK_USER_4, CHECK_USER_1, CHECK_USER_3, CHECK_USER_2
        ));

        Map<String, Object> parameter = new HashMap<String, Object>();
        parameter.put("var", 1);
        workflow.completeUserTask(parameter);


        workflowTestRule.commit();
        return workflow;
    }

    /**
     * 実行
     */
    private WorkflowInstance execute(String instanceId, String userId) {
        ThreadContext.setUserId(userId);
        WorkflowInstance workflow = WorkflowManager.findInstance(instanceId);
        workflow.completeUserTask();

        workflowTestRule.commit();
        return workflow;
    }

    /**
     * スタート直後のワークフローの状態をアサーとする。
     *
     * @param workflow
     */
    private void assertStartWorkflow(WorkflowInstance workflow) {
        WorkflowDbAccessSupport testDao = workflowTestRule.getWorkflowDao();

        assertThat("最初のタスクがアクティブに", workflow.isActive("t01"), is(true));

        SqlResultSet instanceFlowNode = testDao.findInstanceFlowNode();
        assertThat(instanceFlowNode.size(), is(5));
        assertThat(instanceFlowNode.get(0).getString("flow_node_id"), is("t01"));
        assertThat(instanceFlowNode.get(1).getString("flow_node_id"), is("t02"));
        assertThat(instanceFlowNode.get(2).getString("flow_node_id"), is("t03"));
        assertThat(instanceFlowNode.get(3).getString("flow_node_id"), is("t04"));
        assertThat(instanceFlowNode.get(4).getString("flow_node_id"), is("t05"));

        SqlResultSet activeFlowNode = testDao.findActiveFlowNode();
        assertThat(activeFlowNode.size(), is(1));
        assertThat(activeFlowNode.get(0).getString("flow_node_id"), is("t01"));

        SqlResultSet assignedUser = testDao.findAssignedUser();
        assertThat(assignedUser.size(), is(3));
        assertThat(assignedUser.get(0).getString("flow_node_id"), is("t01"));
        assertThat(assignedUser.get(0).getString("assigned_user_id"), is(SYSTEM_USER));
        assertThat(assignedUser.get(1).getString("flow_node_id"), is("t03"));
        assertThat(assignedUser.get(1).getString("assigned_user_id"), is(JUDGE_USER));
        assertThat(assignedUser.get(2).getString("flow_node_id"), is("t04"));
        assertThat(assignedUser.get(2).getString("assigned_user_id"), is(JUDGE_USER));

        SqlResultSet assignedGroup = testDao.findAssignedGroup();
        assertThat(assignedGroup.size(), is(0));

        SqlResultSet userTask = testDao.findActiveUserTask();
        assertThat(userTask.size(), is(1));
        assertThat(userTask.get(0).getString("assigned_user_id"), is(SYSTEM_USER));

        SqlResultSet groupTask = testDao.findActiveGroupTask();
        assertThat(groupTask.size(), is(0));
    }

    /**
     * 自動審査OK後のインスタンス状態のアサート
     * @param workflow
     */
    private void assertAutoJudgeOk(WorkflowInstance workflow) {
        WorkflowDbAccessSupport testDao = workflowTestRule.getWorkflowDao();

        assertThat("調査タスクがアクティブになっていること", workflow.isActive("t02"), is(true));

        SqlResultSet assignedUser = testDao.findAssignedUser();
        assertThat(assignedUser.size(), is(6));
        assertThat(assignedUser.get(0).getString("flow_node_id"), is("t01"));
        assertThat(assignedUser.get(0).getString("assigned_user_id"), is(SYSTEM_USER));
        assertThat(assignedUser.get(1).getString("flow_node_id"), is("t02"));
        assertThat(assignedUser.get(1).getString("assigned_user_id"), is(CHECK_USER_1));
        assertThat(assignedUser.get(2).getString("flow_node_id"), is("t02"));
        assertThat(assignedUser.get(2).getString("assigned_user_id"), is(CHECK_USER_3));
        assertThat(assignedUser.get(3).getString("flow_node_id"), is("t02"));
        assertThat(assignedUser.get(3).getString("assigned_user_id"), is(CHECK_USER_4));
        assertThat(assignedUser.get(4).getString("flow_node_id"), is("t03"));
        assertThat(assignedUser.get(4).getString("assigned_user_id"), is(JUDGE_USER));
        assertThat(assignedUser.get(5).getString("flow_node_id"), is("t04"));
        assertThat(assignedUser.get(5).getString("assigned_user_id"), is(JUDGE_USER));

        SqlResultSet assignedGroup = testDao.findAssignedGroup();
        assertThat(assignedGroup.size(), is(0));

        SqlResultSet activeFlowNode = testDao.findActiveFlowNode();
        assertThat(activeFlowNode.size(), is(1));
        assertThat(activeFlowNode.get(0).getString("flow_node_id"), is("t02"));

        SqlResultSet userTask = testDao.findActiveUserTask();
        assertThat(userTask.size(), is(3));
        assertThat(userTask.get(0).getString("assigned_user_id"), is(CHECK_USER_1));
        assertThat(userTask.get(1).getString("assigned_user_id"), is(CHECK_USER_3));
        assertThat(userTask.get(2).getString("assigned_user_id"), is(CHECK_USER_4));

        SqlResultSet groupTask = testDao.findActiveGroupTask();
        assertThat(groupTask.size(), is(0));
    }

    /**
     * 1人目の調査OK後の状態アサート
     * @param workflow
     */
    private void assertCheckOkOne(WorkflowInstance workflow) {

        assertThat("1人目の実行後では、アクティブタスクは移動しない", workflow.isActive("t02"), is(true));
        assertThat("進行先はアクティブになっていない", workflow.isActive("t03"), is(false));

        WorkflowDbAccessSupport testDao = workflowTestRule.getWorkflowDao();
        SqlResultSet userTask = testDao.findActiveUserTask();
        assertThat("アクティブユーザ数が減っていること", userTask.size(), is(2));
        assertThat(userTask.get(0).getString("assigned_user_id"), is(CHECK_USER_1));
        assertThat(userTask.get(1).getString("assigned_user_id"), is(CHECK_USER_4));

        SqlResultSet groupTask = testDao.findActiveGroupTask();
        assertThat(groupTask.size(), is(0));
    }
    
    /**
     * 2人目の調査OK後の状態アサート
     * @param workflow
     */
    private void assertCheckOkTow(WorkflowInstance workflow) {
        assertThat("タスクが進行していること", workflow.isActive("t03"), is(true));
        assertThat("進行前タスクは非アクティブになっていること", workflow.isActive("t02"), is(false));

        WorkflowDbAccessSupport testDao = workflowTestRule.getWorkflowDao();
        SqlResultSet activeFlowNode = testDao.findActiveFlowNode();
        assertThat(activeFlowNode.size(), is(1));
        assertThat(activeFlowNode.get(0).getString("flow_node_id"), is("t03"));

        SqlResultSet userTask = testDao.findActiveUserTask();
        assertThat(userTask.size(), is(1));
        assertThat(userTask.get(0).getString("assigned_user_id"), is(JUDGE_USER));

        SqlResultSet groupTask = testDao.findActiveGroupTask();
        assertThat(groupTask.size(), is(0));
    }

    /**
     * 判定OK→上位判定の状態アサート
     */
    private void assertJudgeOkToUpperJudge(WorkflowInstance workflow) {
        assertThat("上位判定がアクティブに", workflow.isActive("t04"), is(true));
        assertThat("判定は非アクティブに", workflow.isActive("t03"), is(false));

        WorkflowDbAccessSupport testDao = workflowTestRule.getWorkflowDao();
        SqlResultSet activeFlowNode = testDao.findActiveFlowNode();
        assertThat(activeFlowNode.size(), is(1));
        assertThat(activeFlowNode.get(0).getString("flow_node_id"), is("t04"));

        // 上位判定者を差し替えたので、その情報もアサートする。
        SqlResultSet assignedUser = testDao.findAssignedUser();
        assertThat(assignedUser.size(), is(6));
        assertThat(assignedUser.get(0).getString("flow_node_id"), is("t01"));
        assertThat(assignedUser.get(0).getString("assigned_user_id"), is(SYSTEM_USER));
        assertThat(assignedUser.get(1).getString("flow_node_id"), is("t02"));
        assertThat(assignedUser.get(1).getString("assigned_user_id"), is(CHECK_USER_1));
        assertThat(assignedUser.get(2).getString("flow_node_id"), is("t02"));
        assertThat(assignedUser.get(2).getString("assigned_user_id"), is(CHECK_USER_3));
        assertThat(assignedUser.get(3).getString("flow_node_id"), is("t02"));
        assertThat(assignedUser.get(3).getString("assigned_user_id"), is(CHECK_USER_4));
        assertThat(assignedUser.get(4).getString("flow_node_id"), is("t03"));
        assertThat(assignedUser.get(4).getString("assigned_user_id"), is(JUDGE_USER));
        assertThat(assignedUser.get(5).getString("flow_node_id"), is("t04"));
        assertThat(assignedUser.get(5).getString("assigned_user_id"), is(UPPER_JUDGE_USER));

        SqlResultSet userTask = testDao.findActiveUserTask();
        assertThat(userTask.size(), is(1));
        assertThat(userTask.get(0).getString("assigned_user_id"), is(UPPER_JUDGE_USER));
    }

    /**
     * 上位判定結果のアサート
     * @param workflow
     */
    private void assertUpperJudgeOk(WorkflowInstance workflow) {
        assertThat("実行がアクティブ", workflow.isActive("t05"), is(true));
        assertThat("上位判定は非アクティブ", workflow.isActive("t04"), is(false));

        WorkflowDbAccessSupport testDao = workflowTestRule.getWorkflowDao();
        SqlResultSet activeFlowNode = testDao.findActiveFlowNode();
        assertThat(activeFlowNode.size(), is(1));
        assertThat(activeFlowNode.get(0).getString("flow_node_id"), is("t05"));

        SqlResultSet userTask = testDao.findActiveUserTask();
        assertThat(userTask.size(), is(1));
        assertThat(userTask.get(0).getString("assigned_user_id"), is(CHECK_USER_4));
        assertThat(userTask.get(0).getBigDecimal("execution_order").intValue(), is(1));

        SqlResultSet assignedUser = testDao.findAssignedUser();
        assertThat(assignedUser.size(), is(10));
        assertThat(assignedUser.get(0).getString("flow_node_id"), is("t01"));
        assertThat(assignedUser.get(1).getString("flow_node_id"), is("t02"));
        assertThat(assignedUser.get(2).getString("flow_node_id"), is("t02"));
        assertThat(assignedUser.get(3).getString("flow_node_id"), is("t02"));
        assertThat(assignedUser.get(4).getString("flow_node_id"), is("t03"));
        assertThat(assignedUser.get(5).getString("flow_node_id"), is("t04"));

        assertThat(assignedUser.get(6).getString("flow_node_id"), is("t05"));
        assertThat(assignedUser.get(6).getString("assigned_user_id"), is(CHECK_USER_1));
        assertThat(assignedUser.get(6).getBigDecimal("execution_order").intValue(), is(2));

        assertThat(assignedUser.get(7).getString("flow_node_id"), is("t05"));
        assertThat(assignedUser.get(7).getString("assigned_user_id"), is(CHECK_USER_2));
        assertThat(assignedUser.get(7).getBigDecimal("execution_order").intValue(), is(4));

        assertThat(assignedUser.get(8).getString("flow_node_id"), is("t05"));
        assertThat(assignedUser.get(8).getString("assigned_user_id"), is(CHECK_USER_3));
        assertThat(assignedUser.get(8).getBigDecimal("execution_order").intValue(), is(3));

        assertThat(assignedUser.get(9).getString("flow_node_id"), is("t05"));
        assertThat(assignedUser.get(9).getString("assigned_user_id"), is(CHECK_USER_4));
        assertThat(assignedUser.get(9).getBigDecimal("execution_order").intValue(), is(1));
    }

    /**
     * 実行後の状態をアサート
     * @param workflow
     */
    private void assertExecute(WorkflowInstance workflow, String nextUserId) {
        assertThat(workflow.isActive("t05"), is(true));

        WorkflowDbAccessSupport testDao = workflowTestRule.getWorkflowDao();
        SqlResultSet activeFlowNode = testDao.findActiveFlowNode();
        assertThat(activeFlowNode.size(), is(1));
        assertThat(activeFlowNode.get(0).getString("flow_node_id"), is("t05"));

        SqlResultSet userTask = testDao.findActiveUserTask();
        assertThat(userTask.size(), is(1));
        assertThat(userTask.get(0).getString("assigned_user_id"), is(nextUserId));
    }

    /**
     * ワークフローインスタンスが終了していることをアサート
     * @param workflow
     */
    private void assertCompleteWorkflow(WorkflowInstance workflow) {
        assertThat(workflow.isCompleted(), is(true));
        assertThat(workflow.isActive("t01"), is(false));
        assertThat(workflow.isActive("t02"), is(false));
        assertThat(workflow.isActive("t03"), is(false));
        assertThat(workflow.isActive("t04"), is(false));
        assertThat(workflow.isActive("t05"), is(false));

        WorkflowDbAccessSupport testDao = workflowTestRule.getWorkflowDao();
        assertThat(testDao.findWorkflowInstance().size(), is(0));
        assertThat(testDao.findInstanceFlowNode().size(), is(0));
        assertThat(testDao.findActiveUserTask().size(), is(0));
        assertThat(testDao.findActiveGroupTask().size(), is(0));
    }
}


