package nablarch.integration.workflow.definition;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.List;

import nablarch.core.repository.SystemRepository;

import nablarch.integration.workflow.testhelper.entity.BoundaryEventTriggerEntity;
import nablarch.integration.workflow.testhelper.entity.GatewayEntity;
import nablarch.integration.workflow.testhelper.entity.WorkflowEntity;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import nablarch.integration.workflow.definition.loader.DatabaseWorkflowDefinitionLoader;
import nablarch.integration.workflow.testhelper.WorkflowDbAccessSupport;
import nablarch.integration.workflow.testhelper.WorkflowTestRule;
import nablarch.integration.workflow.testhelper.entity.BoundaryEventEntity;
import nablarch.integration.workflow.testhelper.entity.EventEntity;
import nablarch.integration.workflow.testhelper.entity.LaneEntity;
import nablarch.integration.workflow.testhelper.entity.SequenceFlowEntity;
import nablarch.integration.workflow.testhelper.entity.TaskEntity;

/**
 * {@link WorkflowDefinition}のテスト
 */
public class WorkflowDefinitionTest {

    @ClassRule
    public static WorkflowTestRule workflowTestRule = new WorkflowTestRule();

    private WorkflowDbAccessSupport workflowDbAccessSupport;

    @Before
    public void setUp() throws Exception {
        workflowDbAccessSupport = workflowTestRule.getWorkflowDao();
        // テストで使用するテーブルをクリーニング
        workflowDbAccessSupport.cleanupAll();
    }

    /**
     * 開始イベントから終了イベント（中断イベント）までの遷移情報が取得できること。
     */
    @Test
    public void testProceedingProcess() throws Exception {

        //----- setup db -----
        WorkflowEntity process = new WorkflowEntity("proc1", 1, "交通費申請", "20140801");
        workflowDbAccessSupport.insertWorkflowEntity(process);

        LaneEntity lane1 = new LaneEntity(process, "001", "申請者");
        LaneEntity lane2 = new LaneEntity(process, "002", "承認者");
        workflowDbAccessSupport.insertLaneEntity(lane1, lane2);

        workflowDbAccessSupport.insertEventEntity(
                new EventEntity("f01", lane1, "開始イベント", "START"),
                new EventEntity("f99", lane2, "終了イベント", "TERMINATE"),
                new EventEntity("f50", lane1, "破棄中断", "TERMINATE"));

        TaskEntity approvalActivity = new TaskEntity("f02", lane2, "承認", "NONE", null);
        workflowDbAccessSupport.insertTaskEntity(
                approvalActivity,
                new TaskEntity("f03", lane2, "上位者承認", "NONE", null),
                new TaskEntity("f04", lane1, "再申請", "NONE", null)
        );

        workflowDbAccessSupport.insertGatewayEntity(
                new GatewayEntity("g01", lane2, "承認・破棄分岐", "EXCLUSIVE")
        );

        workflowDbAccessSupport.insertSequenceEntity(
                new SequenceFlowEntity(process, "seq000001", "申請書作成", "f01", "f02", null),
                new SequenceFlowEntity(process, "seq000002", "承認", "f02", "f03", null),
                new SequenceFlowEntity(process, "seq000003", "破棄・承認", "f03", "g01", null),
                new SequenceFlowEntity(process, "seq000004", "破棄", "g01", "f50",
                        "nablarch.integration.workflow.condition.FlowProceedCondition3(abc, hoge)"),
                new SequenceFlowEntity(process, "seq000005", "承認", "g01", "f99",
                        "nablarch.integration.workflow.condition.FlowProceedCondition3(abc, hoge)")
        );

        BoundaryEventTriggerEntity trigger = new BoundaryEventTriggerEntity(process, "___", "引き戻しメッセージ");
        workflowDbAccessSupport.insertBoundaryEventTriggerEntity(trigger);

        workflowDbAccessSupport.insertBoundaryEventEntity(
                new BoundaryEventEntity("t01", "引き戻し", lane2, approvalActivity, trigger)
        );

        // ----- execute -----
        DatabaseWorkflowDefinitionLoader workflowLoader = SystemRepository.get("workflowLoader");
        List<WorkflowDefinition> workflowDefinitions = workflowLoader.load();

        // assert process definition
        WorkflowDefinition workflowDefinition = workflowDefinitions.get(0);
        assertThat(workflowDefinition.getWorkflowId(), is("proc1"));
        assertThat(workflowDefinition.getVersion(), is(1));
        assertThat(workflowDefinition.getWorkflowName(), is("交通費申請"));

        // assert start event
        Event startEventDefinition = workflowDefinition.getStartEvent();
        assertThat(startEventDefinition.getFlowNodeId(), is("f01"));
        assertThat(startEventDefinition.getFlowNodeName(), is("開始イベント"));
        assertThat(startEventDefinition.getEventType(), is(Event.EventType.START));

        // メッセージ
        List<BoundaryEvent> boundaryDefinitions = workflowDefinition.getBoundaryEvent("___");
        assertThat(boundaryDefinitions.size(), is(1));
        assertThat(boundaryDefinitions.get(0).getBoundaryEventTriggerId(), is("___"));
        assertThat(boundaryDefinitions.get(0).getFlowNodeId(), is("t01"));
        assertThat(boundaryDefinitions.get(0).getFlowNodeName(), is("引き戻し"));
        assertThat(boundaryDefinitions.get(0).getAttachedTaskId(), is("f02"));
    }

    /**
     * ゲートウェイ情報が連続して定義されていてもフロー情報が取得できること。
     *
     * @throws Exception
     */
    @Test
    public void testConsecutiveGateway() throws Exception {
        //----- setup db -----
        WorkflowEntity process = new WorkflowEntity("proc1", 9999, "交通費申請", "20140501");
        workflowDbAccessSupport.insertWorkflowEntity(process);

        LaneEntity lane1 = new LaneEntity(process, "001", "申請者");
        workflowDbAccessSupport.insertLaneEntity(lane1);

        workflowDbAccessSupport.insertEventEntity(
                new EventEntity("f01", lane1, "開始イベント", "START"),
                new EventEntity("f98", lane1, "中断", "TERMINATE"),
                new EventEntity("f99", lane1, "終了イベント", "TERMINATE"));

        workflowDbAccessSupport.insertTaskEntity(
                new TaskEntity("f02", lane1, "申請", "NONE", null),
                new TaskEntity("f03", lane1, "承認", "NONE", null),
                new TaskEntity("f04", lane1, "上位者承認", "NONE", null)
        );

        workflowDbAccessSupport.insertGatewayEntity(
                new GatewayEntity("g01", lane1, "差し戻し・承認・破棄分岐", "EXCLUSIVE"),
                new GatewayEntity("g02", lane1, "上位者承認", "EXCLUSIVE")
        );

        workflowDbAccessSupport.insertSequenceEntity(
                new SequenceFlowEntity(process, "seq000001", "申請書作成", "f01", "f02", null),
                new SequenceFlowEntity(process, "seq000002", "承認依頼", "f02", "f03", null),

                new SequenceFlowEntity(process, "seq000003", "差し戻し・破棄・承認", "f03", "g01", null),
                new SequenceFlowEntity(process, "seq000004", "差し戻し", "g01", "f02", null),
                new SequenceFlowEntity(process, "seq000005", "上位者承認へ", "g01", "g02", null),
                new SequenceFlowEntity(process, "seq000006", "破棄", "g01", "f98", null),
                new SequenceFlowEntity(process, "seq000007", "承認", "g01", "f99", null),

                new SequenceFlowEntity(process, "seq000008", "上位者承認あり", "g02", "f04", null),
                new SequenceFlowEntity(process, "seq000009", "上位者承認なし", "g02", "f99", null),

                new SequenceFlowEntity(process, "seq000010", "上位者承認あり", "f04", "f99", null)
        );

        // ----- execute -----
        DatabaseWorkflowDefinitionLoader workflowLoader = SystemRepository.get("workflowLoader");
        List<WorkflowDefinition> workflowDefinitions = workflowLoader.load();

        WorkflowDefinition workflowDefinition = workflowDefinitions.get(0);
        // assert process definition
        assertThat(workflowDefinition.getWorkflowId(), is("proc1"));
        assertThat(workflowDefinition.getVersion(), is(9999));
        assertThat(workflowDefinition.getWorkflowName(), is("交通費申請"));

        // 開始イベント
        Event startEvent = workflowDefinition.getStartEvent();
        assertThat(startEvent.getFlowNodeId(), is("f01"));
    }

    /**
     * 開始イベントが存在しない場合例外が発生すること。
     */
    @Test
    public void testStartEventNotFound() throws Exception {

        //----- setup db -----
        WorkflowEntity process2 = new WorkflowEntity("proc2", 1, "プロセス2", "20141001");
        workflowDbAccessSupport.insertWorkflowEntity(process2);

        LaneEntity process2Lane = new LaneEntity(process2, "001", "プロセス2レーン");
        workflowDbAccessSupport.insertLaneEntity(process2Lane);

        workflowDbAccessSupport.insertEventEntity(
                new EventEntity("f50", process2Lane, "終了イベント", "TERMINATE")
        );

        // ----- execute -----
        DatabaseWorkflowDefinitionLoader workflowLoader = SystemRepository.get("workflowLoader");
        WorkflowDefinition workflowDefinition = workflowLoader.load().get(0);
        try {
            workflowDefinition.getStartEvent();
            fail("ここは通らない");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(),
                    containsString("\"Start Event\" is not defined. \"Start Event\" must be a single definition."));
            assertThat(e.getMessage(), containsString("workflow = [proc2(プロセス2)], version = [1]"));
        }
    }

    /**
     * 複数の開始イベントが定義されている場合例外が発生すること。
     */
    @Test
    public void testMultipleStartEvent() throws Exception {
        //----- setup db -----
        WorkflowEntity process1 = new WorkflowEntity("proc1", 9999, "プロセス1", "20140501");
        workflowDbAccessSupport.insertWorkflowEntity(process1);

        LaneEntity process1Lane = new LaneEntity(process1, "001", "プロセス1レーン");
        workflowDbAccessSupport.insertLaneEntity(process1Lane);

        workflowDbAccessSupport.insertEventEntity(
                new EventEntity("f01", process1Lane, "開始イベント", "START"),
                new EventEntity("f02", process1Lane, "開始イベントふたつめ", "START"),
                new EventEntity("f03", process1Lane, "終了イベント", "TERMINATE")
        );

        // ----- execute -----
        DatabaseWorkflowDefinitionLoader workflowLoader = SystemRepository.get("workflowLoader");
        WorkflowDefinition workflowDefinition = workflowLoader.load().get(0);
        try {
            workflowDefinition.getStartEvent();
            fail("ここは通らない");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(),
                    containsString(
                            "\"Start Event\" is multiply defined. \"Start Event\" must be a single definition."));
            assertThat(e.getMessage(), containsString("workflow = [proc1(プロセス1)], version = [9999]"));
        }
    }

    /**
     * フローノードが取得できること
     */
    @Test
    public void testFindFlowNode() throws Exception {
        WorkflowDbAccessSupport testDao = workflowTestRule.getWorkflowDao();
        testDao.createSimpleProcess();
        workflowTestRule.reloadProcessDefinitions();

        DatabaseWorkflowDefinitionLoader workflowLoader = SystemRepository.get("workflowLoader");
        WorkflowDefinition workflowDefinition = workflowLoader.load().get(0);

        FlowNode actual = workflowDefinition.findFlowNode("a01");
        assertThat(actual.getFlowNodeId(), is("a01"));
        assertThat(actual.getFlowNodeName(), is("承認"));
    }

    /**
     * フローノードが存在しない場合例外が発生すること。
     */
    @Test
    public void testFlowNodeNotFound() throws Exception {
        WorkflowDbAccessSupport testDao = workflowTestRule.getWorkflowDao();
        testDao.createSimpleProcess();
        workflowTestRule.reloadProcessDefinitions();

        DatabaseWorkflowDefinitionLoader workflowLoader = SystemRepository.get("workflowLoader");
        WorkflowDefinition workflowDefinition = workflowLoader.load().get(0);

        try {
            workflowDefinition.findFlowNode("a03");
            fail("ここはとおらない");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("flow node definitions was not found. " + "workflow id = [" + workflowDefinition.getWorkflowId() + "]," +
                    " version = [" + workflowDefinition.getVersion()+"], flow node id = [a03]"));
        }
    }
}

