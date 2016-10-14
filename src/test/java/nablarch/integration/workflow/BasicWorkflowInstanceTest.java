package nablarch.integration.workflow;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nablarch.integration.workflow.condition.StringEqualFlowProceedCondition;
import nablarch.integration.workflow.definition.Event;
import nablarch.integration.workflow.testhelper.WorkflowDbAccessSupport;
import nablarch.integration.workflow.testhelper.WorkflowTestRule;
import nablarch.integration.workflow.testhelper.entity.BoundaryEventEntity;
import nablarch.integration.workflow.testhelper.entity.BoundaryEventTriggerEntity;
import nablarch.integration.workflow.testhelper.entity.EventEntity;
import nablarch.integration.workflow.testhelper.entity.GatewayEntity;
import nablarch.integration.workflow.testhelper.entity.LaneEntity;
import nablarch.integration.workflow.testhelper.entity.SequenceFlowEntity;
import nablarch.integration.workflow.testhelper.entity.TaskEntity;
import nablarch.integration.workflow.testhelper.entity.WorkflowEntity;
import nablarch.integration.workflow.util.WorkflowUtil;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import nablarch.core.ThreadContext;
import nablarch.core.db.statement.SqlRow;

import nablarch.integration.workflow.condition.AllCompletionCondition;
import nablarch.integration.workflow.condition.OrCompletionCondition;

/**
 * {@link BasicWorkflowInstance} で、ワークフローをワークフロー定義通りに進行させることができることのテスト。
 * <p/>
 * ワークフローが進行するときに
 * <ul>
 * <li>アクティブフローノードのフローノード処理が実行されること</li>
 * <li>アクティブフローノードのタスクが完了した場合：進行先フローノードが選択されること、進行先フローノードのアクティベート処理が実行されること</li>
 * <li>アクティブフローノードのタスクが完了しない場合：アクティブフローノードは更新されないこと</li>
 * </ul>
 * のテストを行う。
 *
 * @author Ryo Tanaka
 * @since 1.4.2
 */
public class BasicWorkflowInstanceTest {

    @ClassRule
    public static final WorkflowTestRule rule = new WorkflowTestRule(true);

    private static WorkflowDbAccessSupport db;

    @BeforeClass
    public static void before() throws Exception {
        db = rule.getWorkflowDao();
        // メソッドごとに用意すると、テストがだいぶ遅くなる。。。
        prepareWorkflowDefinition();
    }

    @After
    public void after() throws Exception {
        ThreadContext.clear();
    }

    private static final String WORKFLOW_ID = "WF001";
    private static final String TERMINATE_EVENT = "e02";
    private static final String TASK = "t01";
    private static final String OTHER_TASK = "t03";
    private static final String SEQ_TASK = "t04";
    private static final String SEQ_TASK_2 = "t05";
    private static final String OR_1_COMPLETE_PAR_TASK = "t06";
    private static final String OR_2_COMPLETE_PAR_TASK = "t07";
    private static final String PAR_TASK = "t08";
    private static final String EXECUTING_USER = "execUserId";
    private static final String OTHER_USER = "otherUsrId";
    private static final String EVENT_TRIGGER_ID = "t01";
    private static final String TASK_EVENT_TRIGGER_ID = "t02";

    private static final String TO_GATEWAY_WORKFLOW_ID = "WF002";
    private static final String TO_GATEWAY_ACTIVATE_TASK = "t90";

    private static final List<String> NOT_ASSIGNED = Collections.emptyList();

    // ----- support methods -----
    private static void prepareWorkflowDefinition() {
        db.cleanupAll();
        String toGatewayCondition = StringEqualFlowProceedCondition.class.getName() + "(toGateway, true)";
        String toTaskCondition = StringEqualFlowProceedCondition.class.getName() + "(toTask, none)";
        String toOtherTaskCondition = StringEqualFlowProceedCondition.class.getName() + "(toTask, other)";
        String toSeqTaskCondition = StringEqualFlowProceedCondition.class.getName() + "(toTask, sequential)";
        String toSeqTask2Condition = StringEqualFlowProceedCondition.class.getName() + "(toTask, sequential2)";
        String toParTaskCondition = StringEqualFlowProceedCondition.class.getName() + "(toTask, parallel)";
        String toTerminateCondition = StringEqualFlowProceedCondition.class.getName() + "(toTerminate, true)";
        // スタートイベント後にタスクがあるパターン
        {
            WorkflowEntity workflow = new WorkflowEntity(WORKFLOW_ID, 2L, "汎用のワークフロー定義", "19700101");
            db.insertWorkflowEntity(workflow);
            LaneEntity lane = new LaneEntity(workflow, "l01", "Lane");
            LaneEntity lane2 = new LaneEntity(workflow, "l02", "Lane");
            LaneEntity lane3 = new LaneEntity(workflow, "l03", "Lane");
            db.insertLaneEntity(
                    lane,
                    lane2,
                    lane3
            );
            db.insertEventEntity(
                    new EventEntity("e01", lane, "StartEvent", "START"),
                    new EventEntity(TERMINATE_EVENT, lane, "TerminateEvent", "TERMINATE")
            );
            TaskEntity task = new TaskEntity(TASK, lane2, "Task", "NONE", null);
            TaskEntity seqTask = new TaskEntity(SEQ_TASK, lane2, "SeqTask", "SEQUENTIAL", AllCompletionCondition.class.getName());
            TaskEntity parTask = new TaskEntity(PAR_TASK, lane2, "ParTask(all)", "PARALLEL", AllCompletionCondition.class.getName());
            db.insertTaskEntity(
                    task,
                    seqTask,
                    parTask,
                    new TaskEntity(OTHER_TASK, lane, "Task", "NONE", null),
                    new TaskEntity(SEQ_TASK_2, lane, "SeqTask2", "SEQUENTIAL", AllCompletionCondition.class.getName()),
                    new TaskEntity(OR_1_COMPLETE_PAR_TASK, lane, "ParTask(1)", "PARALLEL", OrCompletionCondition.class.getName() + "(1)"),
                    new TaskEntity(OR_2_COMPLETE_PAR_TASK, lane, "ParTask(2)", "PARALLEL", OrCompletionCondition.class.getName() + "(2)")
            );
            BoundaryEventTriggerEntity eventTrigger = new BoundaryEventTriggerEntity(workflow, EVENT_TRIGGER_ID, "EventTrigger");
            BoundaryEventTriggerEntity taskEventTrigger = new BoundaryEventTriggerEntity(workflow, TASK_EVENT_TRIGGER_ID, "EventTrigger");
            db.insertBoundaryEventTriggerEntity(
                    eventTrigger,
                    taskEventTrigger
            );
            db.insertBoundaryEventEntity(
                    new BoundaryEventEntity("b01", "BoundaryEvent1", lane, task, taskEventTrigger),
                    new BoundaryEventEntity("b02", "BoundaryEvent2", lane, seqTask, eventTrigger),
                    new BoundaryEventEntity("b03", "BoundaryEvent3", lane, parTask, eventTrigger)
            );
            db.insertGatewayEntity(
                    new GatewayEntity("g01", lane, "Gateway", "EXCLUSIVE"),
                    new GatewayEntity("g02", lane, "Gateway", "EXCLUSIVE")
            );
            db.insertSequenceEntity(
                    new SequenceFlowEntity(workflow, "000000001", "StartEvent -> Task", "e01", TASK, null),
                    new SequenceFlowEntity(workflow, "000000002", "Task -> Gateway1", TASK, "g01", null),
                    new SequenceFlowEntity(workflow, "000000003", "Gateway1 -> Gateway2", "g01", "g02", toGatewayCondition),
                    new SequenceFlowEntity(workflow, "000000004", "Gateway2 -> Task", "g02", TASK, toTaskCondition),
                    new SequenceFlowEntity(workflow, "000000005", "Gateway2 -> OtherTask", "g02", OTHER_TASK, toOtherTaskCondition),
                    new SequenceFlowEntity(workflow, "000000006", "Gateway2 -> SeqTask", "g02", SEQ_TASK, toSeqTaskCondition),
                    new SequenceFlowEntity(workflow, "000000007", "Gateway2 -> SeqTask2", "g02", SEQ_TASK_2, toSeqTask2Condition),
                    new SequenceFlowEntity(workflow, "000000008", "Gateway2 -> ParTask(all)", "g02", PAR_TASK, toParTaskCondition),
                    new SequenceFlowEntity(workflow, "000000009", "Gateway2 -> Terminate", "g02", TERMINATE_EVENT, toTerminateCondition),
                    new SequenceFlowEntity(workflow, "000000010", "SeqTask -> Gateway2", SEQ_TASK, "g02", null),
                    new SequenceFlowEntity(workflow, "000000011", "ParTask(1) -> Gateway2", OR_1_COMPLETE_PAR_TASK, "g02", null),
                    new SequenceFlowEntity(workflow, "000000012", "ParTask(all) -> Gateway2", PAR_TASK, "g02", null),
                    new SequenceFlowEntity(workflow, "000000013", "BoundaryEvent1 -> OtherTask", "b01", OTHER_TASK, null),
                    new SequenceFlowEntity(workflow, "000000014", "BoundaryEvent2 -> Gateway2", "b02", "g02", null),
                    new SequenceFlowEntity(workflow, "000000015", "BoundaryEvent3 -> Gateway2", "b03", "g02", null),
                    new SequenceFlowEntity(workflow, "000000016", "OtherTask -> TASK", OTHER_TASK, TASK, null)
            );
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

    // ----- tests

    /**
     * 実行中のユーザを指定せずに {@link BasicWorkflowInstance#completeUserTask()} を呼び出す場合のテスト。<br>
     * 対象のユーザのアクティブユーザタスクが存在しない場合は、例外が発生すること。
     */
    @Test
    public void testCompleteUserTask_WithoutExecutor_WithoutParameter() throws Exception {
        ThreadContext.setUserId(EXECUTING_USER);
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, TASK);

        try {
            workflow.completeUserTask();
            fail("スレッドコンテキストに格納されたユーザのユーザタスクが存在しない場合は、例外が発生しなくてはいけない。");
        } catch (IllegalStateException actual) {
            assertThat("実行ユーザとして、スレッドコンテキストの値が使用されること。", actual.getMessage(), containsString("user = [" + EXECUTING_USER + "]"));
        }
    }

    /**
     * 実行中のユーザを指定して {@link BasicWorkflowInstance#completeUserTask(String)} を呼び出す場合のテスト。
     */
    @Test
    public void testCompleteUserTask_WithExecutor_WithoutParameter() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, OTHER_TASK);
        workflow.assignUser(OTHER_TASK, OTHER_USER);

        workflow.completeUserTask(OTHER_USER);
        assertThat("ワークフローの進行に利用するパラメータとして空のマップが利用され、タスクが完了していること。", workflow.isActive(OTHER_TASK), is(false));
    }

    /**
     * 実行中のグループを指定して {@link BasicWorkflowInstance#completeGroupTask(String)} を呼び出す場合のテスト。
     */
    @Test
    public void testCompleteGroupTask_WithExecutor_WithoutParameter() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, OTHER_TASK);
        workflow.assignGroup(OTHER_TASK, OTHER_USER);

        workflow.completeGroupTask(OTHER_USER);
        assertThat("ワークフローの進行に利用するパラメータとして空のマップが利用され、タスクが完了していること。", workflow.isActive(OTHER_TASK), is(false));
    }

    /**
     * 実行中のユーザを指定して {@link BasicWorkflowInstance#completeUserTask(String)} を呼び出す場合のテスト。
     */
    @Test
    public void testCompleteUserTask_WithExecutor_WithoutParameter_NotExist() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, TASK);
        workflow.assignUser(TASK, EXECUTING_USER);

        String otherUserId = "otherUsrId";
        try {
            workflow.completeUserTask(otherUserId);
            fail("指定されたユーザのユーザタスクが存在しない場合は、例外が発生しなくてはいけない。");
        } catch (IllegalStateException actual) {
            assertThat("引数で渡された実行ユーザが使用されること。", actual.getMessage(), containsString("user = [" + otherUserId + "]"));
        }
    }

    /**
     * 実行中のグループを指定して {@link BasicWorkflowInstance#completeGroupTask(String)} を呼び出す場合のテスト。
     */
    @Test
    public void testCompleteGroupTask_WithExecutor_WithoutParameter_NotExist() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, TASK);
        workflow.assignGroup(TASK, EXECUTING_USER);

        String otherUserId = "otherUsrId";
        try {
            workflow.completeGroupTask(otherUserId);
            fail("指定されたグループのユーザタスクが存在しない場合は、例外が発生しなくてはいけない。");
        } catch (IllegalStateException actual) {
            assertThat("引数で渡された実行グループが使用されること。", actual.getMessage(), containsString("group = [" + otherUserId + "]"));
        }
    }

    /**
     * パラメータを指定して {@link BasicWorkflowInstance#completeUserTask(Map)} を呼び出す場合のテスト。
     */
    @Test
    public void testCompleteUserTask_WithoutExecutor_WithParameter() throws Exception {
        ThreadContext.setUserId(EXECUTING_USER);
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, PAR_TASK);
        workflow.assignUser(PAR_TASK, EXECUTING_USER);

        workflow.completeUserTask(Collections.singletonMap("toTask", "none"));
        rule.commit();

        assertThat("実行ユーザとしてスレッドコンテキストの値が使用され、タスクが完了していること。", workflow.isActive(PAR_TASK), is(false));
        WorkflowTestSupport.assertActiveFlowNode("引数のパラメータを使用して進行先フローノードが取得されていること。", workflow, TASK);
    }

    /**
     * 既に完了しているワークフローに対して {@link BasicWorkflowInstance#completeUserTask()} を呼び出す場合のテスト。
     */
    @Test
    public void testCompleteUserTask_AlreadyCompletedWorkflow() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithoutDb("dummy_____", WORKFLOW_ID, TERMINATE_EVENT);

        try {
            workflow.completeUserTask();
            fail("すでに完了しているワークフローに対して、タスクを完了させようとした場合、例外が発生しなくてはいけない。");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("Workflow is already completed."));
            assertThat(e.getMessage(), containsString("instance id = [dummy_____]"));
            assertThat(e.getMessage(), containsString("active flow node id = [" + TERMINATE_EVENT + "]"));
        }
    }

    /**
     * 既に完了しているワークフローに対して {@link BasicWorkflowInstance#completeGroupTask(String)} を呼び出す場合のテスト。
     */
    @Test
    public void testCompleteGroupTask_AlreadyCompletedWorkflow() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithoutDb("dummy_____", WORKFLOW_ID, TERMINATE_EVENT);

        try {
            workflow.completeGroupTask("dummyGroup");
            fail("すでに完了しているワークフローに対して、タスクを完了させようとした場合、例外が発生しなくてはいけない。");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("Workflow is already completed."));
            assertThat(e.getMessage(), containsString("instance id = [dummy_____]"));
            assertThat(e.getMessage(), containsString("active flow node id = [" + TERMINATE_EVENT + "]"));
        }
    }

    /**
     * 実行ユーザ、パラメータを指定して {@link BasicWorkflowInstance#completeUserTask(Map, String)} を呼び出す場合のテスト。<br />
     * タスクの完了後に、次のタスクがアクティブになる場合のテスト。
     * <p/>
     * <ul>
     * <li>現在アクティブなタスクのフローノード処理が呼ばれること。</li>
     * <li>フローノード処理の結果、タスクが完了し、パラメータを使用して進行先ノードが正しく取得されること。（次のタスクまで進行すること。）</li>
     * <li>進行先ノードのアクティベート処理が呼ばれること。</li>
     * </ul>
     */
    @Test
    public void testCompleteUserTask_WithExecutor_WithParameter_CompleteToTask() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, OR_1_COMPLETE_PAR_TASK);
        workflow.assignUsers(OR_1_COMPLETE_PAR_TASK, Arrays.asList(OTHER_USER, "g000000004", "g000000005"));
        workflow.assignUsers(SEQ_TASK, Arrays.asList("g000000001", "g000000002", "g000000003"));
        rule.commit();

        Map<String, String> param = new HashMap<String, String>();
        param.put("toGateway", "true");
        param.put("toTask", "sequential");
        workflow.completeUserTask(param, OTHER_USER);
        rule.commit();

        List<SqlRow> tasks = WorkflowUtil.filterList(db.findActiveGroupTask(), new WorkflowTestSupport.WorkflowInstanceFlowNodeFilter(workflow, TASK));
        assertThat("アクティブフローノードのフローノード処理が実行されていること。", tasks, is(empty()));
        WorkflowTestSupport.assertActiveFlowNode("ゲートウェイにパラメータが渡され、進行先フローノードを正しく取得できていること。", workflow, SEQ_TASK);
        WorkflowTestSupport.assertCurrentTasks("次のタスクまで進行し、進行先フローノードのアクティブ化処理が正しく完了していること。",
                workflow, Collections.singletonList("g000000001"), NOT_ASSIGNED);
    }

    /**
     * 実行グループ、パラメータを指定して {@link BasicWorkflowInstance#completeGroupTask(Map, String)} を呼び出す場合のテスト。
     * タスクの完了後に、次の停止イベントがアクティブになる場合のテスト。
     * <p/>
     * <ul>
     * <li>現在アクティブなタスクのフローノード処理が呼ばれること。</li>
     * <li>フローノード処理の結果、タスクが完了し、パラメータを使用して進行先ノードが正しく取得されること。（次の停止イベントまで進行すること。）</li>
     * <li>進行先ノードのアクティベート処理が呼ばれること。</li>
     * </ul>
     */
    @Test
    public void testCompleteGroupTask_WithExecutor_WithParameter_CompleteToTerminateEvent() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, OR_1_COMPLETE_PAR_TASK);
        workflow.assignGroups(OR_1_COMPLETE_PAR_TASK, Arrays.asList(OTHER_USER, "g000000004", "g000000005"));
        workflow.assignGroups(SEQ_TASK, Arrays.asList("g000000001", "g000000002", "g000000003"));
        rule.commit();

        Map<String, String> param = new HashMap<String, String>();
        param.put("toGateway", "true");
        param.put("toTerminate", "true");
        workflow.completeGroupTask(param, OTHER_USER);
        rule.commit();

        assertThat("進行先フローノードが正しく取得されていること。", workflow.isActive(TERMINATE_EVENT), is(true));
        assertThat("次の停止イベントまで進行し、進行先フローノードのアクティブ化処理が正しく完了していること。",
                WorkflowUtil.contains(db.findWorkflowInstance(), new WorkflowTestSupport.WorkflowInstanceFilter(workflow)), is(false));
    }

    /**
     * 実行ユーザ、パラメータを指定して {@link BasicWorkflowInstance#completeUserTask(Map, String)} を呼び出す場合のテスト。
     * <p/>
     * <ul>
     * <li>現在アクティブなタスクのフローノード処理が呼ばれること。</li>
     * <li>フローノード処理の結果、タスクが完了せず、アクティブフローノードが更新されないこと。</li>
     * </ul>
     */
    @Test
    public void testCompleteUserTask_WithExecutor_WithParameter_NotComplete() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, PAR_TASK);
        workflow.assignUsers(PAR_TASK, Arrays.asList(OTHER_USER, "u000000004", "u000000005"));
        workflow.assignUsers(SEQ_TASK, Arrays.asList("u000000001", "u000000002", "u000000003"));
        rule.commit();

        Map<String, String> param = new HashMap<String, String>();
        param.put("toGateway", "true");
        param.put("toTask", "sequential");
        workflow.completeUserTask(param, OTHER_USER);
        rule.commit();

        WorkflowTestSupport.assertCurrentTasks("アクティブフローノードのフローノード処理が正しく完了していること。",
                workflow, Arrays.asList("u000000004", "u000000005"), NOT_ASSIGNED);
        WorkflowTestSupport.assertActiveFlowNode("タスクが完了しなかったため、アクティブフローノードは更新されていないこと。", workflow, PAR_TASK);
    }

    /**
     * 実行グループ、パラメータを指定して {@link BasicWorkflowInstance#completeGroupTask(Map, String)} を呼び出す場合のテスト。
     * <p/>
     * <ul>
     * <li>現在アクティブなタスクのフローノード処理が呼ばれること。</li>
     * <li>フローノード処理の結果、タスクが完了せず、アクティブフローノードが更新されないこと。</li>
     * </ul>
     */
    @Test
    public void testCompleteGroupTask_WithExecutor_WithParameter_NotComplete() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, PAR_TASK);
        workflow.assignGroups(PAR_TASK, Arrays.asList(OTHER_USER, "u000000004", "u000000005"));
        workflow.assignGroups(SEQ_TASK, Arrays.asList("u000000001", "u000000002", "u000000003"));
        rule.commit();

        Map<String, String> param = new HashMap<String, String>();
        param.put("toGateway", "true");
        param.put("toTask", "sequential");
        workflow.completeGroupTask(param, OTHER_USER);
        rule.commit();

        WorkflowTestSupport.assertCurrentTasks("アクティブフローノードのフローノード処理が正しく完了していること。",
                workflow, NOT_ASSIGNED, Arrays.asList("u000000004", "u000000005"));
        WorkflowTestSupport.assertActiveFlowNode("タスクが完了しなかったため、アクティブフローノードは更新されていないこと。", workflow, PAR_TASK);
    }

    /**
     * パラメータを指定せずに {@link BasicWorkflowInstance#triggerEvent(String)} を呼び出す場合のテスト。（タスクまで進行する。）
     */
    @Test
    public void testTriggerEvent_WithoutParameter() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, TASK);
        workflow.assignUser(TASK, OTHER_USER);
        workflow.assignUser(OTHER_TASK, EXECUTING_USER);

        workflow.triggerEvent(TASK_EVENT_TRIGGER_ID);
        rule.commit();

        WorkflowTestSupport.assertActiveFlowNode("中断境界イベントで現在のタスクが中断され、次のタスクまで正しく進行すること。", workflow, OTHER_TASK);
        WorkflowTestSupport.assertCurrentTasks("進行先フローノードのアクティブ化処理が実行されること。", workflow, Arrays.asList(EXECUTING_USER), NOT_ASSIGNED);
    }

    /**
     * パラメータを指定して {@link BasicWorkflowInstance#triggerEvent(String, Map)} を呼び出す場合のテスト。(停止イベントまで進行する)
     */
    @Test
    public void testTriggerEvent_FromParallelTask_MultipleAssigned_ToParallelTask_MultipleAssigned() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, PAR_TASK);
        workflow.assignGroups(PAR_TASK, Arrays.asList("0000000001", "0000000002", "0000000003"));
        workflow.assignGroups(SEQ_TASK, Arrays.asList(EXECUTING_USER, OTHER_USER));

        workflow.triggerEvent(EVENT_TRIGGER_ID, Collections.singletonMap("toTerminate", "true"));
        rule.commit();

        assertThat("進行先フローノードが正しく取得されていること。", workflow.isActive(TERMINATE_EVENT), is(true));
        assertThat("次の停止イベントまで進行し、進行先フローノードのアクティブ化処理が正しく完了していること。",
                WorkflowUtil.contains(db.findWorkflowInstance(), new WorkflowTestSupport.WorkflowInstanceFilter(workflow)), is(false));
    }

    /**
     * 指定されたイベントトリガーに対応する境界イベントが、アクティブフローノードに存在しない場合は、例外が発生すること。
     */
    @Test
    public void testTriggerEvent_NoTarget() throws Exception {
        WorkflowInstance sut = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, TASK);

        try {
            sut.triggerEvent(EVENT_TRIGGER_ID);
            fail("指定されたイベントトリガーに対応する境界イベントがアクティブフローノードに存在しない場合は、例外が発生しなくてはいけない。");
        } catch (IllegalStateException actual) {
            assertThat(actual.getMessage(), containsString("Boundary Event is not found"));
            assertThat(actual.getMessage(), containsString("event trigger id = [" + EVENT_TRIGGER_ID + "]"));
            assertThat(actual.getMessage(), containsString("active flow node id = [" + TASK + "]"));
        }
    }

    /**
     * {@link Event.EventType#TERMINATE} 以外がアクティブな場合には、
     * {@link BasicWorkflowInstance#isCompleted()} は {@code false} を返却すること。
     */
    @Test
    public void testIsCompleted_NotCompleted() throws Exception {
        WorkflowInstance atStart = WorkflowTestSupport.prepareWorkflowWithoutDb("dummy_____", WORKFLOW_ID, "e01");
        assertThat(atStart.isCompleted(), is(false));

        WorkflowInstance atTask = WorkflowTestSupport.prepareWorkflowWithoutDb("dummy_____", WORKFLOW_ID, TASK);
        assertThat(atTask.isCompleted(), is(false));
    }

    /**
     * {@link Event.EventType#TERMINATE} がアクティブな場合には、
     * {@link BasicWorkflowInstance#isCompleted()} は {@code true} を返却すること。
     */
    @Test
    public void testIsCompleted_Completed() throws Exception {
        WorkflowInstance atTerminate = WorkflowTestSupport.prepareWorkflowWithoutDb("dummy_____", WORKFLOW_ID, TERMINATE_EVENT);
        assertThat(atTerminate.isCompleted(), is(true));
    }

    /**
     * {@link BasicWorkflowInstance#assignUser(String, String)} のテスト。
     */
    @Test
    public void testAssignUser() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, TASK);
        rule.commit();

        workflow.assignUser(TASK, EXECUTING_USER);
        rule.commit();

        WorkflowTestSupport.assertAssignment("指定されたユーザがタスクに割り当てられていること。", workflow, TASK, Arrays.asList(EXECUTING_USER), NOT_ASSIGNED);
        WorkflowTestSupport.assertCurrentTasks("アクティブユーザタスクも更新されていること。", workflow, Arrays.asList(EXECUTING_USER), NOT_ASSIGNED);
    }

    /**
     * アクティブでないタスクにアサインする場合の {@link BasicWorkflowInstance#assignUsers(String, List)} のテスト。
     */
    @Test
    public void testAssignUsers_NotActive() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, TASK);
        rule.commit();

        workflow.assignUsers(PAR_TASK, Arrays.asList(EXECUTING_USER, OTHER_USER));
        rule.commit();

        WorkflowTestSupport.assertAssignment("指定されたユーザがタスクに割り当てられていること。", workflow, PAR_TASK, Arrays.asList(EXECUTING_USER, OTHER_USER), NOT_ASSIGNED);
        WorkflowTestSupport.assertCurrentTasks("アクティブユーザタスクは更新されていないこと。", workflow, NOT_ASSIGNED, NOT_ASSIGNED);
    }

    /**
     * アクティブなタスクにアサインする場合の {@link BasicWorkflowInstance#assignUsers(String, List)} のテスト。
     */
    @Test
    public void testAssignUsers_Active() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, SEQ_TASK);
        rule.commit();

        workflow.assignUsers(SEQ_TASK, Arrays.asList(EXECUTING_USER, OTHER_USER));
        rule.commit();

        WorkflowTestSupport.assertAssignment("指定されたユーザがタスクに割り当てられていること。", workflow, SEQ_TASK, Arrays.asList(EXECUTING_USER, OTHER_USER), NOT_ASSIGNED);
        WorkflowTestSupport.assertCurrentTasks("アクティブユーザタスクも更新されていること。", workflow, Arrays.asList(EXECUTING_USER), NOT_ASSIGNED);
    }

    /**
     * 既に完了しているワークフローに含まれるタスクにアサインする場合の {@link BasicWorkflowInstance#assignUsers(String, List)} のテスト。
     */
    @Test
    public void testAssignUsers_Completed() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithoutDb("dummy_____", WORKFLOW_ID, TERMINATE_EVENT);
        rule.commit();

        List<String> assignee = Arrays.asList(EXECUTING_USER, OTHER_USER);
        try {
            workflow.assignUsers(SEQ_TASK, assignee);
            fail("既に完了しているワークフローのタスクに担当ユーザを割り当てようとした場合は、例外が発生しなくてはいけない。");
        } catch (IllegalStateException e) {
            String actual = e.getMessage();
            assertThat(actual, containsString("Cannot assign users to a completed workflow."));
            assertThat(actual, containsString("instance id = [" + workflow.getInstanceId() + "]"));
            assertThat(actual, containsString("active flow node id = [" + TERMINATE_EVENT + "]"));
            assertThat(actual, containsString("users = [" + assignee + "]"));
        }
    }

    /**
     * アサインしようとしたタスクが見つからない場合の {@link BasicWorkflowInstance#assignUsers(String, List)} のテスト。
     */
    @Test
    public void testAssignUsers_TaskNotFound() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithoutDb("dummy_____", WORKFLOW_ID, TASK);
        rule.commit();

        List<String> assignee = Arrays.asList(EXECUTING_USER, OTHER_USER);
        try {
            workflow.assignUsers("not exist", assignee);
            fail("アサインしようとしたタスクが見つからない場合は、例外が発生しなくてはいけない。");
        } catch (IllegalArgumentException e) {
            String actual = e.getMessage();
            assertThat(actual, containsString("task definition was not found."));
            assertThat(actual, containsString("task id = [not exist]"));
            assertThat(actual, containsString("workflow id = [" + workflow.getWorkflowId() + "]"));
            assertThat(actual, containsString("version = [" + workflow.getVersion() + "]"));
            assertThat(actual, containsString("task id = [not exist]"));
        }
    }

    /**
     * {@link BasicWorkflowInstance#assignUserToLane(String, String)} のテスト
     */
    @Test
    public void testAssignUserToLane() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, OTHER_TASK);
        rule.commit();

        workflow.assignUserToLane("l02", EXECUTING_USER);
        rule.commit();

        WorkflowTestSupport.assertAssignment("対象レーンのすべてのタスクにユーザがアサインされていること。", workflow, TASK, Arrays.asList(EXECUTING_USER), NOT_ASSIGNED);
        WorkflowTestSupport.assertAssignment("対象レーンのすべてのタスクにユーザがアサインされていること。", workflow, PAR_TASK, Arrays.asList(EXECUTING_USER), NOT_ASSIGNED);
        WorkflowTestSupport.assertAssignment("対象レーンのすべてのタスクにユーザがアサインされていること。", workflow, SEQ_TASK, Arrays.asList(EXECUTING_USER), NOT_ASSIGNED);
        WorkflowTestSupport.assertAssignment("対象レーン外のタスクにはユーザがアサインされていないこと。", workflow, OTHER_TASK, NOT_ASSIGNED, NOT_ASSIGNED);
        WorkflowTestSupport.assertCurrentTasks("対象レーン外のアクティブユーザタスクもアサインされていないこと。", workflow, NOT_ASSIGNED, NOT_ASSIGNED);
    }

    /**
     * 対象レーンにタスクが存在する場合の {@link BasicWorkflowInstance#assignUsersToLane(String, List)} のテスト
     */
    @Test
    public void testAssignUsersToLane() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, TASK);
        rule.commit();

        workflow.assignUsersToLane("l02", Arrays.asList(EXECUTING_USER));
        rule.commit();

        WorkflowTestSupport.assertAssignment("対象レーンのすべてのタスクにユーザがアサインされていること。", workflow, TASK, Arrays.asList(EXECUTING_USER), NOT_ASSIGNED);
        WorkflowTestSupport.assertAssignment("対象レーンのすべてのタスクにユーザがアサインされていること。", workflow, PAR_TASK, Arrays.asList(EXECUTING_USER), NOT_ASSIGNED);
        WorkflowTestSupport.assertAssignment("対象レーンのすべてのタスクにユーザがアサインされていること。", workflow, SEQ_TASK, Arrays.asList(EXECUTING_USER), NOT_ASSIGNED);
        WorkflowTestSupport.assertAssignment("対象レーン外のタスクにはユーザがアサインされていないこと。", workflow, OTHER_TASK, NOT_ASSIGNED, NOT_ASSIGNED);
        WorkflowTestSupport.assertCurrentTasks("対象レーンのアクティブユーザタスクも更新されていること。", workflow, Arrays.asList(EXECUTING_USER), NOT_ASSIGNED);
    }

    /**
     * 対象レーンにタスクが存在しない場合の {@link BasicWorkflowInstance#assignUsersToLane(String, List)} のテスト
     */
    @Test
    public void testAssignUsersToLane_Empty() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, TASK);
        rule.commit();

        workflow.assignUsersToLane("l03", Arrays.asList(EXECUTING_USER));
        rule.commit();

        WorkflowTestSupport.assertAssignment("特に例外が発生せず、どのレーンのタスクにもアサインされていないこと。", workflow, TASK, NOT_ASSIGNED, NOT_ASSIGNED);
        WorkflowTestSupport.assertAssignment("特に例外が発生せず、どのレーンのタスクにもアサインされていないこと。", workflow, OTHER_TASK, NOT_ASSIGNED, NOT_ASSIGNED);
        WorkflowTestSupport.assertCurrentTasks("アクティブユーザタスクも更新されていないこと。", workflow, NOT_ASSIGNED, NOT_ASSIGNED);
    }

    /**
     * {@link BasicWorkflowInstance#assignGroup(String, String)} のテスト。
     */
    @Test
    public void testAssignGroup() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, TASK);
        rule.commit();

        workflow.assignGroup(TASK, "0000000001");
        rule.commit();

        WorkflowTestSupport.assertAssignment("指定されたグループがタスクに割り当てられていること。", workflow, TASK, NOT_ASSIGNED, Arrays.asList("0000000001"));
        WorkflowTestSupport.assertCurrentTasks("アクティブグループタスクも更新されていること。", workflow, NOT_ASSIGNED, Arrays.asList("0000000001"));
    }

    /**
     * アクティブでないタスクにアサインする場合の {@link BasicWorkflowInstance#assignGroups(String, List)} のテスト。
     */
    @Test
    public void testAssignGroups_NotActive() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, TASK);
        rule.commit();

        workflow.assignGroups(PAR_TASK, Arrays.asList("0000000001", "0000000002"));
        rule.commit();

        WorkflowTestSupport.assertAssignment("指定されたグループがタスクに割り当てられていること。", workflow, PAR_TASK, NOT_ASSIGNED, Arrays.asList("0000000001", "0000000002"));
        WorkflowTestSupport.assertCurrentTasks("アクティブグループタスクは更新されていないこと。", workflow, NOT_ASSIGNED, NOT_ASSIGNED);
    }

    /**
     * アクティブなタスクにアサインする場合の {@link BasicWorkflowInstance#assignGroups(String, List)} のテスト。
     */
    @Test
    public void testAssignGroups_Active() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, SEQ_TASK);
        rule.commit();

        workflow.assignGroups(SEQ_TASK, Arrays.asList("0000000001", "0000000002"));
        rule.commit();

        WorkflowTestSupport.assertAssignment("指定されたグループがタスクに割り当てられていること。", workflow, SEQ_TASK, NOT_ASSIGNED, Arrays.asList("0000000001", "0000000002"));
        WorkflowTestSupport.assertCurrentTasks("アクティブグループタスクも更新されていること。", workflow, NOT_ASSIGNED, Arrays.asList("0000000001"));
    }

    /**
     * 既に完了しているワークフローに含まれるタスクにアサインする場合の {@link BasicWorkflowInstance#assignGroups(String, List)} のテスト。
     */
    @Test
    public void testAssignGroups_Completed() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithoutDb("dummy_____", WORKFLOW_ID, TERMINATE_EVENT);
        rule.commit();

        List<String> assignee = Arrays.asList("0000000001", "0000000002");
        try {
            workflow.assignGroups(SEQ_TASK, assignee);
            fail("既に完了しているワークフローのタスクに担当グループを割り当てようとした場合は、例外が発生しなくてはいけない。");
        } catch (IllegalStateException e) {
            String actual = e.getMessage();
            assertThat(actual, containsString("Cannot assign groups to a completed workflow."));
            assertThat(actual, containsString("instance id = [" + workflow.getInstanceId() + "]"));
            assertThat(actual, containsString("active flow node id = [" + TERMINATE_EVENT + "]"));
            assertThat(actual, containsString("groups = [" + assignee + "]"));
        }
    }

    /**
     * アサインしようとしたタスクが見つからない場合の {@link BasicWorkflowInstance#assignGroups(String, List)} のテスト。
     */
    @Test
    public void testAssignGroups_TaskNotFound() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithoutDb("dummy_____", WORKFLOW_ID, TASK);
        rule.commit();

        List<String> assignee = Arrays.asList("0000000001", "0000000002");
        try {
            workflow.assignGroups("not exist", assignee);
            fail("アサインしようとしたタスクが見つからない場合は、例外が発生しなくてはいけない。");
        } catch (IllegalArgumentException e) {
            String actual = e.getMessage();
            assertThat(actual, containsString("task definition was not found."));
            assertThat(actual, containsString("task id = [not exist]"));
            assertThat(actual, containsString("workflow id = [" + workflow.getWorkflowId() + "]"));
            assertThat(actual, containsString("version = [" + workflow.getVersion() + "]"));
            assertThat(actual, containsString("task id = [not exist]"));
        }
    }

    /**
     * {@link BasicWorkflowInstance#assignGroupToLane(String, String)} のテスト
     */
    @Test
    public void testAssignGroupToLane() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, OTHER_TASK);
        rule.commit();

        workflow.assignGroupToLane("l02", "0000000001");
        rule.commit();

        WorkflowTestSupport.assertAssignment("対象レーンのすべてのタスクにグループがアサインされていること。", workflow, TASK, NOT_ASSIGNED, Arrays.asList("0000000001"));
        WorkflowTestSupport.assertAssignment("対象レーンのすべてのタスクにグループがアサインされていること。", workflow, PAR_TASK, NOT_ASSIGNED, Arrays.asList("0000000001"));
        WorkflowTestSupport.assertAssignment("対象レーンのすべてのタスクにグループがアサインされていること。", workflow, SEQ_TASK, NOT_ASSIGNED, Arrays.asList("0000000001"));
        WorkflowTestSupport.assertAssignment("対象レーン外のタスクにはグループがアサインされていないこと。", workflow, OTHER_TASK, NOT_ASSIGNED, NOT_ASSIGNED);
        WorkflowTestSupport.assertCurrentTasks("対象レーン外のアクティブグループタスクもアサインされていないこと。", workflow, NOT_ASSIGNED, NOT_ASSIGNED);
    }

    /**
     * 対象レーンにタスクが存在する場合の {@link BasicWorkflowInstance#assignGroupsToLane(String, List)} のテスト
     */
    @Test
    public void testAssignGroupsToLane() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, TASK);
        rule.commit();

        workflow.assignGroupsToLane("l02", Arrays.asList("0000000001"));
        rule.commit();

        WorkflowTestSupport.assertAssignment("対象レーンのすべてのタスクにグループがアサインされていること。", workflow, TASK, NOT_ASSIGNED, Arrays.asList("0000000001"));
        WorkflowTestSupport.assertAssignment("対象レーンのすべてのタスクにグループがアサインされていること。", workflow, PAR_TASK, NOT_ASSIGNED, Arrays.asList("0000000001"));
        WorkflowTestSupport.assertAssignment("対象レーンのすべてのタスクにグループがアサインされていること。", workflow, SEQ_TASK, NOT_ASSIGNED, Arrays.asList("0000000001"));
        WorkflowTestSupport.assertAssignment("対象レーン外のタスクにはグループがアサインされていないこと。", workflow, OTHER_TASK, NOT_ASSIGNED, NOT_ASSIGNED);
        WorkflowTestSupport.assertCurrentTasks("対象レーンのアクティブグループタスクも更新されていること。", workflow, NOT_ASSIGNED, Arrays.asList("0000000001"));
    }

    /**
     * 対象レーンにタスクが存在しない場合の {@link BasicWorkflowInstance#assignGroupsToLane(String, List)} のテスト
     */
    @Test
    public void testAssignGroupsToLane_Empty() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, TASK);
        rule.commit();

        workflow.assignGroupsToLane("l03", Arrays.asList("0000000001"));
        rule.commit();

        WorkflowTestSupport.assertAssignment("特に例外が発生せず、どのレーンのタスクにもアサインされていないこと。", workflow, TASK, NOT_ASSIGNED, NOT_ASSIGNED);
        WorkflowTestSupport.assertAssignment("特に例外が発生せず、どのレーンのタスクにもアサインされていないこと。", workflow, OTHER_TASK, NOT_ASSIGNED, NOT_ASSIGNED);
        WorkflowTestSupport.assertCurrentTasks("アクティブグループタスクも更新されていないこと。", workflow, NOT_ASSIGNED, NOT_ASSIGNED);
    }

    /**
     * アクティブでないタスクに対する {@link BasicWorkflowInstance#changeAssignedUser(String, String, String)} のテスト。
     */
    @Test
    public void testChangeAssignedUser_NotActive() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, TASK);
        workflow.assignUsers(PAR_TASK, Arrays.asList("0000000001", "0000000003"));
        rule.commit();

        workflow.changeAssignedUser(PAR_TASK, "0000000001", "0000000002");
        rule.commit();

        WorkflowTestSupport.assertAssignment("指定されたユーザの割当が変更されていること。", workflow, PAR_TASK, Arrays.asList("0000000002", "0000000003"), NOT_ASSIGNED);
        WorkflowTestSupport.assertCurrentTasks("アクティブタスクは更新されていないこと。", workflow, NOT_ASSIGNED, NOT_ASSIGNED);
    }

    /**
     * アクティブなタスクに対する {@link BasicWorkflowInstance#changeAssignedUser(String, String, String)} のテスト。
     */
    @Test
    public void testChangeAssignedUser_Active() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, PAR_TASK);
        workflow.assignUsers(PAR_TASK, Arrays.asList("0000000001", "0000000003"));
        rule.commit();

        workflow.changeAssignedUser(PAR_TASK, "0000000001", "0000000002");
        rule.commit();

        WorkflowTestSupport.assertAssignment("指定されたユーザの割当が変更されていること。", workflow, PAR_TASK, Arrays.asList("0000000002", "0000000003"), NOT_ASSIGNED);
        WorkflowTestSupport.assertCurrentTasks("アクティブタスクも更新されていること。", workflow, Arrays.asList("0000000002", "0000000003"), NOT_ASSIGNED);
    }

    /**
     * アクティブなタスクに対する {@link BasicWorkflowInstance#changeAssignedUser(String, String, String)} のテスト。
     */
    @Test
    public void testChangeAssignedUser_TaskNotFound() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, PAR_TASK);
        workflow.assignUsers(PAR_TASK, Arrays.asList("0000000001", "0000000003"));
        rule.commit();

        try {
            workflow.changeAssignedUser("not exist", "0000000001", "0000000002");
            fail("指定されたタスクが見つからない場合は、例外が発生しなくてはいけない。");
        } catch (IllegalArgumentException e) {
            String actual = e.getMessage();
            assertThat(actual, containsString("task definition was not found."));
            assertThat(actual, containsString("task id = [not exist]"));
            assertThat(actual, containsString("workflow id = [" + workflow.getWorkflowId() + "]"));
            assertThat(actual, containsString("version = [" + workflow.getVersion() + "]"));
            assertThat(actual, containsString("task id = [not exist]"));
        }
    }

    /**
     * アクティブなタスクに対する {@link BasicWorkflowInstance#changeAssignedUser(String, String, String)} のテスト。
     */
    @Test
    public void testChangeAssignedUser_NotAssigned() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, PAR_TASK);
        workflow.assignUsers(PAR_TASK, Arrays.asList("0000000001", "0000000003"));
        rule.commit();

        try {
            workflow.changeAssignedUser(PAR_TASK, "not assigned", "0000000002");
            fail("振替元ユーザがタスクにアサインされていない場合は、例外が発生しなくてはいけない。");
        } catch (IllegalStateException e) {
            String actual = e.getMessage();
            assertThat(actual, containsString("User is not assigned to task."));
            assertThat(actual, containsString("instance id = [" + workflow.getInstanceId() + "]"));
            assertThat(actual, containsString("task id = [" + PAR_TASK + "]"));
            assertThat(actual, containsString("old user = [not assigned]"));
            assertThat(actual, containsString("assigned user = [" + workflow.getAssignedUsers(PAR_TASK) + "]"));
        }
    }

    /**
     * アクティブでないタスクに対する {@link BasicWorkflowInstance#changeAssignedGroup(String, String, String)} のテスト。
     */
    @Test
    public void testChangeAssignedGroup_NotActive() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, TASK);
        workflow.assignGroups(PAR_TASK, Arrays.asList("0000000001", "0000000003"));
        rule.commit();

        workflow.changeAssignedGroup(PAR_TASK, "0000000001", "0000000002");
        rule.commit();

        WorkflowTestSupport.assertAssignment("指定されたグループの割当が変更されていること。", workflow, PAR_TASK, NOT_ASSIGNED, Arrays.asList("0000000002", "0000000003"));
        WorkflowTestSupport.assertCurrentTasks("アクティブグループタスクは更新されていないこと。", workflow, NOT_ASSIGNED, NOT_ASSIGNED);
    }

    /**
     * アクティブなタスクに対する {@link BasicWorkflowInstance#changeAssignedGroup(String, String, String)} のテスト。
     */
    @Test
    public void testChangeAssignedGroup_Active() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, PAR_TASK);
        workflow.assignGroups(PAR_TASK, Arrays.asList("0000000001", "0000000003"));
        rule.commit();

        workflow.changeAssignedGroup(PAR_TASK, "0000000001", "0000000002");
        rule.commit();

        WorkflowTestSupport.assertAssignment("指定されたグループの割当が変更されていること。", workflow, PAR_TASK, NOT_ASSIGNED, Arrays.asList("0000000002", "0000000003"));
        WorkflowTestSupport.assertCurrentTasks("アクティブグループタスクも更新されていること。", workflow, NOT_ASSIGNED, Arrays.asList("0000000002", "0000000003"));
    }

    /**
     * アクティブなタスクに対する {@link BasicWorkflowInstance#changeAssignedGroup(String, String, String)} のテスト。
     */
    @Test
    public void testChangeAssignedGroup_TaskNotFound() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, PAR_TASK);
        workflow.assignGroups(PAR_TASK, Arrays.asList("0000000001", "0000000003"));
        rule.commit();

        try {
            workflow.changeAssignedGroup("not exist", "0000000001", "0000000002");
            fail("指定されたタスクが見つからない場合は、例外が発生しなくてはいけない。");
        } catch (IllegalArgumentException e) {
            String actual = e.getMessage();
            assertThat(actual, containsString("task definition was not found."));
            assertThat(actual, containsString("task id = [not exist]"));
            assertThat(actual, containsString("workflow id = [" + workflow.getWorkflowId() + "]"));
            assertThat(actual, containsString("version = [" + workflow.getVersion() + "]"));
            assertThat(actual, containsString("task id = [not exist]"));
        }
    }

    /**
     * アクティブなタスクに対する {@link BasicWorkflowInstance#changeAssignedGroup(String, String, String)} のテスト。
     */
    @Test
    public void testChangeAssignedGroup_NotAssigned() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, PAR_TASK);
        workflow.assignGroups(PAR_TASK, Arrays.asList("0000000001", "0000000003"));
        rule.commit();

        try {
            workflow.changeAssignedGroup(PAR_TASK, "not assigned", "0000000002");
            fail("振替元グループがタスクにアサインされていない場合は、例外が発生しなくてはいけない。");
        } catch (IllegalStateException e) {
            String actual = e.getMessage();
            assertThat(actual, containsString("Group is not assigned to task."));
            assertThat(actual, containsString("instance id = [" + workflow.getInstanceId() + "]"));
            assertThat(actual, containsString("task id = [" + PAR_TASK + "]"));
            assertThat(actual, containsString("old group = [not assigned]"));
            assertThat(actual, containsString("assigned group = [" + workflow.getAssignedGroups(PAR_TASK) + "]"));
        }
    }

    /**
     * {@link BasicWorkflowInstance#hasActiveUserTask(String)} のテスト。
     */
    @Test
    public void testHasActiveUserTask() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, PAR_TASK);
        workflow.assignUsers(PAR_TASK, Arrays.asList("0000000001", "0000000003"));
        rule.commit();

        assertThat("指定されたユーザのアクティブユーザタスクが存在する場合はtrueを返却すること。", workflow.hasActiveUserTask("0000000001"), is(true));
        assertThat("複数のアクティブユーザタスクがある場合にも、正しく判定できること。", workflow.hasActiveUserTask("0000000003"), is(true));
        assertThat("指定されたユーザのアクティブユーザタスクが存在しない場合はfalseを返却すること。", workflow.hasActiveUserTask("0000000002"), is(false));
    }

    /**
     * {@link BasicWorkflowInstance#hasActiveGroupTask(String)} のテスト。
     */
    @Test
    public void testHasActiveGroupTask() throws Exception {
        WorkflowInstance workflow = WorkflowTestSupport.prepareWorkflowWithDb(WORKFLOW_ID, PAR_TASK);
        workflow.assignGroups(PAR_TASK, Arrays.asList("0000000001", "0000000003"));
        rule.commit();

        assertThat("指定されたグループのアクティブグループタスクが存在する場合はtrueを返却すること。", workflow.hasActiveGroupTask("0000000001"), is(true));
        assertThat("複数のアクティブグループタスクがある場合にも、正しく判定できること。", workflow.hasActiveGroupTask("0000000003"), is(true));
        assertThat("指定されたグループのアクティブグループタスクが存在しない場合はfalseを返却すること。", workflow.hasActiveGroupTask("0000000002"), is(false));
    }
}
