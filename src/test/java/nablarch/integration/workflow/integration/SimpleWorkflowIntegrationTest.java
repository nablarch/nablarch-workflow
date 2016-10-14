package nablarch.integration.workflow.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import nablarch.integration.workflow.WorkflowInstance;
import nablarch.integration.workflow.testhelper.WorkflowTestRule;
import nablarch.integration.workflow.testhelper.entity.BoundaryEventEntity;
import nablarch.integration.workflow.testhelper.entity.BoundaryEventTriggerEntity;
import nablarch.integration.workflow.testhelper.entity.GatewayEntity;
import nablarch.integration.workflow.testhelper.entity.LaneEntity;
import nablarch.integration.workflow.testhelper.entity.WorkflowEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import nablarch.core.ThreadContext;
import nablarch.core.db.statement.SqlResultSet;

import nablarch.integration.workflow.WorkflowManager;
import nablarch.integration.workflow.testhelper.WorkflowDbAccessSupport;
import nablarch.integration.workflow.testhelper.entity.EventEntity;
import nablarch.integration.workflow.testhelper.entity.SequenceFlowEntity;
import nablarch.integration.workflow.testhelper.entity.TaskEntity;

/**
 * シンプルなワークフローを用いた機能結合テスト。
 * <p/>
 * 本テストで使用するワークフロー定義は、本ソースコードファイルの配置ディレクトリ内の
 * simple_process.bpmnを参照すること。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class SimpleWorkflowIntegrationTest {

    @ClassRule
    public static WorkflowTestRule workflowTestRule = new WorkflowTestRule(true);

    /** テストで使用するプロセスID */
    private static final String TEST_PROCESS_ID = "PRO01";

    /** 申請者ユーザID */
    private static final String APPLICATION_USER = "0000000001";

    /** 庶務グループ */
    private static final String GENERAL_SECTION = "G000000001";

    /** 庶務グループのユーザ */
    private static final String GENERAL_SECTION_USER = "8000000001";

    /** 承認者 */
    private static final String APPROVAL_USER = "9000000001";

    /**
     * テストデータの準備
     */
    @BeforeClass
    public static void setUpClass() throws Exception {
        WorkflowDbAccessSupport workflowDbAccessSupport = workflowTestRule.getWorkflowDao();
        workflowDbAccessSupport.cleanupAll();

        WorkflowEntity workflow = new WorkflowEntity(TEST_PROCESS_ID, 2, "交通費申請", "20110101");

        LaneEntity lane1 = new LaneEntity(workflow, "l01", "申請者");
        LaneEntity lane2 = new LaneEntity(workflow, "l02", "庶務");
        LaneEntity lane3 = new LaneEntity(workflow, "l03", "承認者");

        EventEntity start = new EventEntity("f01", lane2, "開始", "START");
        EventEntity terminate1 = new EventEntity("f06", lane2, "TerminateEndEvent", "TERMINATE");
        EventEntity terminate2 = new EventEntity("f09", lane3, "TerminateEndEvent", "TERMINATE");

        TaskEntity task1 = new TaskEntity("f03", lane1, "再申請", "NONE", null);
        TaskEntity task2 = new TaskEntity("f02", lane2, "確認", "NONE", null);
        TaskEntity task3 = new TaskEntity("f07", lane3, "承認", "NONE", null);

        GatewayEntity gateway1 = new GatewayEntity("f05", lane2, "Exclusive Gateway", "EXCLUSIVE");
        GatewayEntity gateway2 = new GatewayEntity("f08", lane3, "Exclusive Gateway", "EXCLUSIVE");

        BoundaryEventTriggerEntity trigger = new BoundaryEventTriggerEntity(workflow, "t01", "t01");

        BoundaryEventEntity boundaryEvent = new BoundaryEventEntity("f04", "Message", lane2, task2, trigger);

        workflowDbAccessSupport.insertWorkflowEntity(workflow);
        workflowDbAccessSupport.insertLaneEntity(lane1, lane2, lane3);
        workflowDbAccessSupport.insertEventEntity(start, terminate1, terminate2);
        workflowDbAccessSupport.insertTaskEntity(task1, task2, task3);
        workflowDbAccessSupport.insertGatewayEntity(gateway1, gateway2);
        workflowDbAccessSupport.insertBoundaryEventTriggerEntity(trigger);
        workflowDbAccessSupport.insertBoundaryEventEntity(boundaryEvent);
        workflowDbAccessSupport.insertSequenceEntity(
                new SequenceFlowEntity(workflow, "f01", null, "f01", "f02", null),
                new SequenceFlowEntity(workflow, "f02", null, "f03", "f02", null),
                new SequenceFlowEntity(workflow, "f03", "引き戻し", "f04", "f03", null),
                new SequenceFlowEntity(workflow, "f04", null, "f02", "f05", null),
                new SequenceFlowEntity(workflow, "f05", "再申請", "f05", "f03",
                        "nablarch.integration.workflow.condition.EqFlowProceedCondition(var, 1)"),
                new SequenceFlowEntity(workflow, "f06", "差し戻し", "f08", "f02",
                        "nablarch.integration.workflow.condition.EqFlowProceedCondition(var, 2)"),
                new SequenceFlowEntity(workflow, "f07", "確認OK", "f05", "f07",
                        "nablarch.integration.workflow.condition.EqFlowProceedCondition(var, 3)"),
                new SequenceFlowEntity(workflow, "f08", null, "f05", "f06",
                        "nablarch.integration.workflow.condition.EqFlowProceedCondition(var, 2)"),
                new SequenceFlowEntity(workflow, "f09", null, "f07", "f08", null),
                new SequenceFlowEntity(workflow, "f10", null, "f08", "f09",
                        "nablarch.integration.workflow.condition.EqFlowProceedCondition(var, 1)")
        );
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
     * 申請→確認→承認のシンプルな流れのテスト。
     */
    @Test
    public void testConfirmAndApproval() throws Exception {

        // プロセス開始
        WorkflowInstance workflow = startWorkflowProcess();
        assertStartInstance(workflow);

        // 確認行為の実施
        workflow = confirmWorkflowInstance(workflow.getInstanceId());
        assertCompleteConfirm(workflow);

        // 承認行為の実施
        workflow = approvalWorkflowInstance(workflow.getInstanceId());
        assertCompleteApproval(workflow);
    }

    /**
     * 申請→確認(差し戻し)→再申請→確認→承認の流れのテスト。
     */
    @Test
    public void  testConfirmNg() {

        // プロセス開始
        WorkflowInstance workflow = startWorkflowProcess();
        assertStartInstance(workflow);

        // 差し戻し処理
        workflow = confirmNgWorkflowInstance(workflow.getInstanceId());
        assertConfirmNg(workflow);

        // 再申請処理
        workflow = reapplicationWorkflowInstance(workflow.getInstanceId());
        // インスタンスの状態はプロセス開始毎同じ状態になっていること
        assertStartInstance(workflow);

        // 確認の実施
        workflow = confirmWorkflowInstance(workflow.getInstanceId());
        assertCompleteConfirm(workflow);

        // 承認
        workflow = approvalWorkflowInstance(workflow.getInstanceId());
        assertCompleteApproval(workflow);
    }

    /**
     * 申請→確認（却下）→終了の流れのテスト
     */
    @Test
    public void testConfirmReject() {

        // プロセス開始
        WorkflowInstance workflow = startWorkflowProcess();
        assertStartInstance(workflow);

        // 却下
        workflow = rejectWorkflowInstance(workflow.getInstanceId());
        assertRejectInstance(workflow);
    }

    /**
     * 申請→確認→承認（差し戻し）→再度確認→承認の流れのテスト
     * @throws Exception
     */
    @Test
    public void testApprovalNg() throws Exception {

        // プロセス開始
        WorkflowInstance workflow = startWorkflowProcess();
        assertStartInstance(workflow);

        // 確認
        workflow = confirmWorkflowInstance(workflow.getInstanceId());
        assertCompleteConfirm(workflow);

        // 承認（NG）
        workflow = approvalNgWorkFlowInstance(workflow.getInstanceId());
        // プロセス開始時の状態と同じ状態になる。
        assertStartInstance(workflow);

        // 再確認
        workflow = confirmWorkflowInstance(workflow.getInstanceId());
        assertCompleteConfirm(workflow);

        // 承認
        workflow = approvalWorkflowInstance(workflow.getInstanceId());
        assertCompleteApproval(workflow);
    }

    /**
     * 確認待ち状態の申請をキャンセルして再申請
     * @throws Exception
     */
    @Test
    public void testCancel() throws Exception {
        WorkflowInstance workflow = startWorkflowProcess();
        assertStartInstance(workflow);

        workflow = cancelInstance(workflow.getInstanceId());
        assertConfirmNg(workflow);

        workflow = reapplicationWorkflowInstance(workflow.getInstanceId());
        assertStartInstance(workflow);

        workflow = confirmWorkflowInstance(workflow.getInstanceId());
        assertCompleteConfirm(workflow);

        workflow = approvalWorkflowInstance(workflow.getInstanceId());
        assertCompleteApproval(workflow);
    }

    /**
     * ワークフローを開始する。
     * @return ワークフローインスタンス
     */
    private WorkflowInstance startWorkflowProcess() {
        ThreadContext.setUserId(APPLICATION_USER);
        WorkflowInstance workflow = WorkflowManager.startInstance(TEST_PROCESS_ID);
        assertThat("確認ユーザタスクがアクティブになっていること", workflow.isActive("f02"), is(true));

        workflow.assignGroup("f02", GENERAL_SECTION);
        workflow.assignUser("f03", APPLICATION_USER);
        workflow.assignUser("f07", APPROVAL_USER);
        workflowTestRule.commit();
        return workflow;
    }

    /**
     * 申請を確認する。
     */
    private WorkflowInstance confirmWorkflowInstance(String instanceId) {
        ThreadContext.setUserId(GENERAL_SECTION_USER);

        HashMap<String, Object> parameter = new HashMap<String, Object>();
        parameter.put("var", 3);

        WorkflowInstance workflow = WorkflowManager.findInstance(instanceId);
        workflow.completeGroupTask(parameter, GENERAL_SECTION);

        workflowTestRule.commit();
        return workflow;
    }

    /**
     * 申請をNG（差し戻し）する。
     */
    private WorkflowInstance confirmNgWorkflowInstance(String instanceId) {
        ThreadContext.setUserId(GENERAL_SECTION_USER);

        Map<String, Object> parameter = new HashMap<String, Object>();
        parameter.put("var", "1");

        WorkflowInstance workflow = WorkflowManager.findInstance(instanceId);
        workflow.completeGroupTask(parameter, GENERAL_SECTION);

        workflowTestRule.commit();
        return workflow;
    }

    /**
     * 却下する。
     */
    private WorkflowInstance rejectWorkflowInstance(String instanceId) {
        ThreadContext.setUserId(GENERAL_SECTION_USER);

        Map<String, Object> parameter = new HashMap<String, Object>();
        parameter.put("var", "2");

        WorkflowInstance workflow = WorkflowManager.findInstance(instanceId);
        workflow.completeGroupTask(parameter, GENERAL_SECTION);

        workflowTestRule.commit();
        return workflow;
    }

    /**
     * 再申請処理を行う。
     */
    private WorkflowInstance reapplicationWorkflowInstance(String instanceId) {
        ThreadContext.setUserId(APPLICATION_USER);

        WorkflowInstance workflow = WorkflowManager.findInstance(instanceId);
        workflow.completeUserTask();

        workflowTestRule.commit();
        return workflow;
    }

    /**
     * 承認行為をする。
     */
    private WorkflowInstance approvalWorkflowInstance(String instanceId) {
        ThreadContext.setUserId(APPROVAL_USER);

        Map<String, Object> parameter = new HashMap<String, Object>();
        parameter.put("var", 1);

        WorkflowInstance workflow = WorkflowManager.findInstance(instanceId);
        workflow.completeUserTask(parameter);

        workflowTestRule.commit();
        return workflow;
    }

    /**
     * 承認時の差し戻しを行う。
     */
    private WorkflowInstance approvalNgWorkFlowInstance(String instanceId) {
        ThreadContext.setUserId(APPROVAL_USER);

        Map<String, Integer> parameter = new HashMap<String, Integer>();
        parameter.put("var", 2);

        WorkflowInstance workflow = WorkflowManager.findInstance(instanceId);
        workflow.completeUserTask(parameter);

        workflowTestRule.commit();

        return workflow;
    }


    /**
     * プロセス開始直後のインスタンス関連テーブルの状態をアサート
     */
    private void assertStartInstance(WorkflowInstance workflow) {
        WorkflowDbAccessSupport testDao = workflowTestRule.getWorkflowDao();

        // インスタンスの状態をアサート
        SqlResultSet instance = testDao.findWorkflowInstance();
        assertThat(instance.size(), is(1));
        assertThat(instance.get(0).getString("instance_id"), is(workflow.getInstanceId()));
        assertThat(instance.get(0).getString("workflow_id"), is(TEST_PROCESS_ID));
        assertThat(instance.get(0).getBigDecimal("def_version").intValue(), is(2));

        SqlResultSet instanceFlowNode = testDao.findInstanceFlowNode();
        assertThat(instanceFlowNode.size(), is(3));
        assertThat(instanceFlowNode.get(0).getString("flow_node_id"), is("f02"));
        assertThat(instanceFlowNode.get(1).getString("flow_node_id"), is("f03"));
        assertThat(instanceFlowNode.get(2).getString("flow_node_id"), is("f07"));

        SqlResultSet activeFlowNode = testDao.findActiveFlowNode();
        assertThat(activeFlowNode.size(), is(1));
        assertThat(activeFlowNode.get(0).getString("flow_node_id"), is("f02"));

        SqlResultSet assignedUser = testDao.findAssignedUser();
        assertThat(assignedUser.size(), is(2));
        assertThat(assignedUser.get(0).getString("flow_node_id"), is("f03"));
        assertThat(assignedUser.get(0).getString("assigned_user_id"), is(APPLICATION_USER));
        assertThat(assignedUser.get(1).getString("flow_node_id"), is("f07"));
        assertThat(assignedUser.get(1).getString("assigned_user_id"), is(APPROVAL_USER));

        SqlResultSet assignedGroup = testDao.findAssignedGroup();
        assertThat(assignedGroup.size(), is(1));
        assertThat(assignedGroup.get(0).getString("flow_node_id"), is("f02"));
        assertThat(assignedGroup.get(0).getString("assigned_group_id"), is(GENERAL_SECTION));

        SqlResultSet activeUserTask = testDao.findActiveUserTask();
        assertThat("このタスクは、グループが担当なのでユーザは空であること", activeUserTask.size(), is(0));

        SqlResultSet groupTask = testDao.findActiveGroupTask();
        assertThat(groupTask.size(), is(1));
        assertThat(groupTask.get(0).getString("flow_node_id"), is("f02"));
        assertThat(groupTask.get(0).getString("assigned_group_id"), is(GENERAL_SECTION));
    }

    /**
     * 確認完了後のインスタンスの状態をアサート
     */
    private void assertCompleteConfirm(WorkflowInstance workflow) {
        WorkflowDbAccessSupport testDao = workflowTestRule.getWorkflowDao();

        assertThat("確認タスクは非アクティブに", workflow.isActive("f02"), is(false));
        assertThat("承認タスクはアクティブに", workflow.isActive("f07"), is(true));

        SqlResultSet activeFlowNode = testDao.findActiveFlowNode();
        assertThat(activeFlowNode.size(), is(1));
        assertThat(activeFlowNode.get(0).getString("flow_node_id"), is("f07"));

        SqlResultSet userTask = testDao.findActiveUserTask();
        assertThat(userTask.size(), is(1));
        assertThat(userTask.get(0).getString("flow_node_id"), is("f07"));
        assertThat(userTask.get(0).getString("assigned_user_id"), is(APPROVAL_USER));

        SqlResultSet groupTask = testDao.findActiveGroupTask();
        assertThat(groupTask.size(), is(0));
    }

    /**
     * 確認NG（差し戻し）後のインスタンスの状態をアサート
     */
    private void assertConfirmNg(WorkflowInstance workflow) {
        WorkflowDbAccessSupport testDao = workflowTestRule.getWorkflowDao();

        assertThat("確認タスクは非アクティブに", workflow.isActive("f02"), is(false));
        assertThat("再申請タスクがアクティブであること", workflow.isActive("f03"), is(true));

        SqlResultSet activeFlowNode = testDao.findActiveFlowNode();
        assertThat(activeFlowNode.size(), is(1));
        assertThat(activeFlowNode.get(0).getString("flow_node_id"), is("f03"));

        SqlResultSet activeUserTask = testDao.findActiveUserTask();
        assertThat(activeUserTask.size(), is(1));
        assertThat(activeUserTask.get(0).getString("flow_node_id"), is("f03"));
        assertThat(activeUserTask.get(0).getString("assigned_user_id"), is(APPLICATION_USER));

        SqlResultSet activeGroupTask = testDao.findActiveGroupTask();
        assertThat(activeGroupTask.size(), is(0));
    }

    /**
     * 承認完了後のインスタンスの状態をアサート
     */
    private void assertCompleteApproval(WorkflowInstance workflow) {
        WorkflowDbAccessSupport testDao = workflowTestRule.getWorkflowDao();

        assertThat("承認後はワークフローが完了状態であること", workflow.isCompleted(), is(true));
        assertThat("再申請タスクは非アクティブであること", workflow.isActive("f02"), is(false));
        assertThat("確認タスクは非アクティブであること", workflow.isActive("f03"), is(false));
        assertThat("承認タスクは非アクティブであること", workflow.isActive("f07"), is(false));

        assertThat("インスタンスのデータが削除されていること", testDao.findWorkflowInstance().size(), is(0));
        assertThat("タスク一覧が削除されていること", testDao.findInstanceFlowNode().size(), is(0));
        assertThat("担当者情報が削除されていること", testDao.findAssignedUser().size(), is(0));
        assertThat("担当グループ情報が削除されていること", testDao.findAssignedGroup().size(), is(0));
        assertThat("アクティブノード情報が削除されていること", testDao.findActiveFlowNode().size(), is(0));
        assertThat("アクティブ担当者情報が削除されていること", testDao.findActiveUserTask().size(), is(0));
        assertThat("アクティブ担当グループ情報が削除されていること", testDao.findActiveGroupTask().size(), is(0));
    }

    /**
     * 却下後のインスタンスの状態をアサート
     */
    private void assertRejectInstance(WorkflowInstance workflow) {
        // 承認完了後と同じ状態になっていること
        assertCompleteApproval(workflow);
    }

    /**
     * 申請をキャンセルする。
     */
    private WorkflowInstance cancelInstance(String instanceId) {
        WorkflowInstance workflow = WorkflowManager.findInstance(instanceId);
        workflow.triggerEvent("t01");

        workflowTestRule.commit();

        return workflow;
    }

}

