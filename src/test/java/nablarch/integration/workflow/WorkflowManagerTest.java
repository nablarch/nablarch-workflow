package nablarch.integration.workflow;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static nablarch.integration.workflow.WorkflowTestSupport.assertActiveFlowNode;
import static nablarch.integration.workflow.WorkflowTestSupport.assertWorkflowInstance;
import static nablarch.integration.workflow.WorkflowTestSupport.prepareWorkflowWithDb;

import java.util.Collections;
import java.util.Map;

import nablarch.integration.workflow.condition.StringEqualFlowProceedCondition;
import nablarch.integration.workflow.testhelper.WorkflowTestRule;
import nablarch.integration.workflow.testhelper.entity.GatewayEntity;
import nablarch.integration.workflow.testhelper.entity.LaneEntity;
import nablarch.integration.workflow.testhelper.entity.WorkflowEntity;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import nablarch.integration.workflow.testhelper.WorkflowDbAccessSupport;
import nablarch.integration.workflow.testhelper.entity.EventEntity;
import nablarch.integration.workflow.testhelper.entity.SequenceFlowEntity;
import nablarch.integration.workflow.testhelper.entity.TaskEntity;

/**
 * {@link WorkflowManager} のテストクラス。
 */
public class WorkflowManagerTest {

    @ClassRule
    public static final WorkflowTestRule rule = new WorkflowTestRule(false);

    private static final String WORKFLOW_ID = "WF003";
    private static final String TERMINATE_EVENT = "e02";
    private static final String TASK = "t01";

    private static final String TO_GATEWAY_WORKFLOW_ID = "WF002";
    private static final String TO_GATEWAY_ACTIVATE_TASK = "t90";

    @BeforeClass
    public static void before() throws Exception {
        WorkflowDbAccessSupport db = rule.getWorkflowDao();
        db.cleanupAll();
        String toParTaskCondition = StringEqualFlowProceedCondition.class.getName() + "(toTask, parallel)";
        String toTerminateCondition = StringEqualFlowProceedCondition.class.getName() + "(toTerminate, true)";
        // スタートイベント後にタスクがあるパターン
        {
            // バージョンが正しく選択されることをテストするために、複数バージョンを用意しておく。
            db.insertWorkflowEntity(new WorkflowEntity(WORKFLOW_ID, 1L, "汎用のワークフロー定義", "19700101"));
            db.insertWorkflowEntity(new WorkflowEntity(WORKFLOW_ID, 3L, "汎用のワークフロー定義", "99991231"));

            WorkflowEntity workflow = new WorkflowEntity(WORKFLOW_ID, 2L, "汎用のワークフロー定義", "19700101");
            db.insertWorkflowEntity(workflow);
            LaneEntity lane = new LaneEntity(workflow, "l01", "Lane");
            db.insertLaneEntity(lane);
            db.insertEventEntity(new EventEntity("e01", lane, "StartEvent", "START"));
            db.insertTaskEntity(new TaskEntity(TASK, lane, "Task", "NONE", null));
            db.insertSequenceEntity(new SequenceFlowEntity(workflow, "000000001", "StartEvent -> Task", "e01", TASK, null));
        }

        // スタートイベント後にゲートウェイがあるパターン
        {
            WorkflowEntity workflow = new WorkflowEntity(TO_GATEWAY_WORKFLOW_ID, 1L, "スタートイベント後にゲートウェイがあるワークフロー定義", "19700101");
            db.insertWorkflowEntity(workflow);
            LaneEntity lane = new LaneEntity(workflow, "l01", "Lane");
            db.insertLaneEntity(lane);
            db.insertEventEntity(new EventEntity("e01", lane, "StartEvent", "START"));
            db.insertGatewayEntity(new GatewayEntity("g01", lane, "Gateway", "EXCLUSIVE"));
            db.insertEventEntity(new EventEntity(TERMINATE_EVENT, lane, "TerminateEvent", "TERMINATE"));
            db.insertTaskEntity(new TaskEntity(TO_GATEWAY_ACTIVATE_TASK, lane, "Task", "PARALLEL", null));
            db.insertSequenceEntity(
                    new SequenceFlowEntity(workflow, "000000001", "StartEvent -> Gateway", "e01", "g01", null),
                    new SequenceFlowEntity(workflow, "000000002", "Gateway -> TerminateEvent", "g01", TERMINATE_EVENT, toTerminateCondition),
                    new SequenceFlowEntity(workflow, "000000003", "Gateway -> Task", "g01", TO_GATEWAY_ACTIVATE_TASK, toParTaskCondition)
            );
        }
        rule.reloadProcessDefinitions();
    }

    /**
     * {@link WorkflowManager#startInstance(String)} のテスト
     *
     * {@link WorkflowInstanceFactory} に委譲しているので、最低限のテストのみ行う。
     */
    @Test
    public void testStartInstance_WithoutParameter() throws Exception {
        WorkflowInstance workflow = WorkflowManager.startInstance(WORKFLOW_ID);
        rule.commit();

        assertWorkflowInstance("正しくワークフローが開始され、ワークフローインスタンスが生成されていること。", workflow);
        assertActiveFlowNode("進行先フローノードを正しく取得できていること。", workflow, TASK);
    }

    /**
     * {@link WorkflowManager#startInstance(String, Map)} のテスト
     *
     * {@link WorkflowInstanceFactory} に委譲しているので、最低限のテストのみ行う。
     */
    @Test
    public void testStartInstance_WithParameter() throws Exception {
        WorkflowInstance workflow = WorkflowManager.startInstance(TO_GATEWAY_WORKFLOW_ID, Collections.singletonMap("toTask", "parallel"));
        rule.commit();

        assertWorkflowInstance("正しくワークフローが開始され、ワークフローインスタンスが生成されていること。", workflow);
        assertActiveFlowNode("ゲートウェイにパラメータが渡され、進行先フローノードを正しく取得できていること。", workflow, TO_GATEWAY_ACTIVATE_TASK);
    }

    /**
     * {@link WorkflowManager#findInstance(String)} のテスト
     *
     * {@link WorkflowInstanceFactory} に委譲しているので、最低限のテストのみ行う。
     */
    @Test
    public void testFindInstance() throws Exception {
        String id = prepareWorkflowWithDb(WORKFLOW_ID, TASK).getInstanceId();
        rule.commit();

        WorkflowInstance workflow = WorkflowManager.findInstance(id);

        assertThat("ワークフロー定義が正しくDBから復元されていること：ワークフローID", workflow.getWorkflowId(), is(WORKFLOW_ID));
        assertThat("ワークフロー定義が正しくDBから復元されていること：バージョン", workflow.getVersion(), is(2L));
    }

    /**
     * {@link WorkflowManager#getCurrentVersion(String)} のテスト。
     */
    @Test
    public void testGetCurrentVersion() throws Exception {
        assertThat("定義されているバージョンが一つのワークフローに対して、正しいバージョンを取得できること。",
                WorkflowManager.getCurrentVersion(TO_GATEWAY_WORKFLOW_ID), is(1));
        assertThat("定義されているバージョンが複数あるワークフローに対して、正しいバージョンを取得できること。",
                WorkflowManager.getCurrentVersion(WORKFLOW_ID), is(2));
    }

    /**
     * 指定されたワークフローIDのワークフロー定義が存在しない場合の {@link WorkflowManager#getCurrentVersion(String)} のテスト。
     */
    @Test
    public void testGetCurrentVersion_NotExists() throws Exception {
        String notExistingWorkflowId = "NOT_EXISTING_WORKFLOW_ID";
        try {
            WorkflowManager.getCurrentVersion(notExistingWorkflowId);
            fail("指定されたワークフローIDのワークフロー定義が存在しない場合には、例外が発生しなくてはいけない。");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("Workflow definition was not found."));
            assertThat(e.getMessage(), containsString("workflow id = [" + notExistingWorkflowId + "]"));
        }
    }
}
