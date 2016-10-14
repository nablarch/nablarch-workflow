package nablarch.integration.workflow;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static nablarch.integration.workflow.WorkflowTestSupport.assertActiveFlowNode;
import static nablarch.integration.workflow.WorkflowTestSupport.assertCurrentTasks;
import static nablarch.integration.workflow.WorkflowTestSupport.assertWorkflowInstance;
import static nablarch.integration.workflow.WorkflowTestSupport.prepareWorkflowWithDb;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import nablarch.integration.workflow.condition.StringEqualFlowProceedCondition;
import nablarch.integration.workflow.testhelper.WorkflowDbAccessSupport;
import nablarch.integration.workflow.testhelper.WorkflowTestRule;
import nablarch.integration.workflow.testhelper.entity.EventEntity;
import nablarch.integration.workflow.testhelper.entity.GatewayEntity;
import nablarch.integration.workflow.testhelper.entity.LaneEntity;
import nablarch.integration.workflow.testhelper.entity.SequenceFlowEntity;
import nablarch.integration.workflow.testhelper.entity.TaskEntity;
import nablarch.integration.workflow.testhelper.entity.WorkflowEntity;

/**
 * {@link BasicWorkflowInstanceFactory} のテストクラス。
 *
 * @author Ryo Tanaka
 * @since 1.4.2
 */
public class BasicWorkflowInstanceFactoryTest {

    @ClassRule
    public static WorkflowTestRule rule = new WorkflowTestRule(true);

    private static WorkflowDbAccessSupport db;

    private static final String WORKFLOW_ID = "WF001";
    private static final String TERMINATE_EVENT = "e02";
    private static final String TASK = "t01";

    private static final String TO_GATEWAY_WORKFLOW_ID = "WF002";
    private static final String TO_GATEWAY_ACTIVATE_TASK = "t90";

    private static final List<String> NOT_ASSIGNED = Collections.emptyList();

    @BeforeClass
    public static void before() throws Exception {
        db = rule.getWorkflowDao();
        // メソッドごとに用意すると、テストがだいぶ遅くなる。。。
        prepareWorkflowDefinition();
    }

    private final WorkflowInstanceFactory sut = new BasicWorkflowInstanceFactory();

    /**
     * パラメータなしでワークフローを開始する場合の {@link WorkflowManager#startInstance(String)} のテスト。
     */
    @Test
    public void testStart_WithoutParameter() throws Exception {
        WorkflowInstance workflow = sut.start(WORKFLOW_ID);
        rule.commit();

        assertThat("適用日を過ぎていて、バージョンの値が最大のワークフロー定義が取得されていること。", workflow.getVersion(), is(2L));
        assertWorkflowInstance("正しくワークフローが開始され、ワークフローインスタンスが生成されていること。", workflow);
        assertActiveFlowNode("進行先フローノードを正しく取得できていること。", workflow, TASK);
        assertCurrentTasks("ユーザがアサインされていないタスクが正しくアクティブ化されていること。", workflow, NOT_ASSIGNED, NOT_ASSIGNED);
    }

    /**
     * パラメータを使用してワークフローを開始する場合の {@link WorkflowManager#startInstance(String)} のテスト。
     */
    @Test
    public void testStart_WithParameter() throws Exception {
        WorkflowInstance workflow = sut.start(TO_GATEWAY_WORKFLOW_ID, Collections.singletonMap("toTask", "parallel"));
        rule.commit();

        assertWorkflowInstance("正しくワークフローが開始され、ワークフローインスタンスが生成されていること。", workflow);
        assertActiveFlowNode("ゲートウェイにパラメータが渡され、進行先フローノードを正しく取得できていること。", workflow, TO_GATEWAY_ACTIVATE_TASK);
        assertCurrentTasks("ユーザがアサインされていない並列タスクが正しくアクティブ化されていること。", workflow, NOT_ASSIGNED, NOT_ASSIGNED);
    }

    /**
     * {@link WorkflowManager#startInstance(String, Map)} で存在しないワークフローIDが指定された場合には、例外が発生すること。
     */
    @Test
    public void testStart_WorkflowDefinitionNotFound() throws Exception {
        try {
            sut.start("存在しないworkflowId", Collections.<String, Object>emptyMap());
            fail("存在しないワークフローIDでワークフローを開始しようとした場合には、例外が発生しなくてはいけない。");
        } catch (IllegalArgumentException actual) {
            assertThat(actual.getMessage(), containsString("Workflow definition was not found."));
            assertThat(actual.getMessage(), containsString("workflow id = [存在しないworkflowId]"));
        }
    }

    /**
     * 存在するワークフローインスタンスのインスタンスIDが渡された場合、データベースからワークフローインスタンス情報を取得して、
     * 正しく {@link BasicWorkflowInstance} オブジェクトの状態が復元されること。
     */
    @Test
    public void testFind_Found() throws Exception {
        String id = prepareWorkflowWithDb(WORKFLOW_ID, TASK).getInstanceId();
        rule.commit();

        WorkflowInstance workflow = sut.find(id);

        assertActiveFlowNode("アクティブフローノードが正しくDBから復元されていること。", workflow, TASK);
        assertThat("ワークフロー定義が正しくDBから復元されていること：ワークフローID", workflow.getWorkflowId(), is(WORKFLOW_ID));
        assertThat("ワークフロー定義が正しくDBから復元されていること：バージョン", workflow.getVersion(), is(2L));
    }

    /**
     * 存在しないワークフローインスタンスのインスタンスIDが渡された場合、完了状態のワークフローインスタンスが返却されること。
     */
    @Test
    public void testFind_NotFound() throws Exception {
        String instanceId = "completed instance id";
        WorkflowInstance workflow = sut.find(instanceId);

        assertThat("指定したインスタンスIDが設定されていること。", workflow.getInstanceId(), is(instanceId));
        assertThat("ワークフローが完了状態になっていること。", workflow.isCompleted(), is(true));
        assertThat("アクティブフローノードは常にfalseを返却すること。", workflow.isActive("someId"), is(false));
    }

    // ----- support methods -----
    private static void prepareWorkflowDefinition() {
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
}
