package nablarch.integration.workflow.definition.loader;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nablarch.integration.workflow.definition.Lane;
import nablarch.integration.workflow.definition.SequenceFlow;
import nablarch.integration.workflow.definition.Task;
import nablarch.integration.workflow.testhelper.WorkflowTestRule;
import nablarch.integration.workflow.testhelper.entity.BoundaryEventEntity;
import nablarch.integration.workflow.testhelper.entity.BoundaryEventTriggerEntity;
import nablarch.integration.workflow.testhelper.entity.GatewayEntity;
import nablarch.integration.workflow.testhelper.entity.LaneEntity;
import nablarch.integration.workflow.testhelper.entity.WorkflowEntity;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import nablarch.core.repository.SystemRepository;

import nablarch.integration.workflow.definition.BoundaryEvent;
import nablarch.integration.workflow.definition.Event;
import nablarch.integration.workflow.definition.Gateway;
import nablarch.integration.workflow.definition.WorkflowDefinition;
import nablarch.integration.workflow.testhelper.OnMemoryLogWriter;
import nablarch.integration.workflow.testhelper.WorkflowDbAccessSupport;
import nablarch.integration.workflow.testhelper.entity.EventEntity;
import nablarch.integration.workflow.testhelper.entity.SequenceFlowEntity;
import nablarch.integration.workflow.testhelper.entity.TaskEntity;

/**
 * {@link DatabaseWorkflowDefinitionLoader}のテスト。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class DatabaseWorkflowDefinitionLoaderTest {

    @ClassRule
    public static WorkflowTestRule workflowTestRule = new WorkflowTestRule();

    private WorkflowDbAccessSupport workflowDbAccessSupport;

    @Before
    public void setUp() throws Exception {
        workflowDbAccessSupport = workflowTestRule.getWorkflowDao();
        // テストで使用するテーブルをクリーニング
        workflowDbAccessSupport.cleanupAll();
        OnMemoryLogWriter.clear();
    }

    /**
     * データベースの単一のプロセス情報がロードできること。
     */
    @Test
    public void testLoadSingleProcess() throws Exception {

        //----- setup db -----
        WorkflowEntity process = new WorkflowEntity("proc1", 1, "交通費申請", "20140725");
        workflowDbAccessSupport.insertWorkflowEntity(process);

        LaneEntity lane1 = new LaneEntity(process, "001", "申請者");
        LaneEntity lane2 = new LaneEntity(process, "002", "承認者");
        workflowDbAccessSupport.insertLaneEntity(lane1, lane2);

        workflowDbAccessSupport.insertEventEntity(
                new EventEntity("f01", lane1, "開始イベント", "START"),
                new EventEntity("f99", lane2, "終了イベント", "TERMINATE"),
                new EventEntity("f50", lane1, "破棄中断", "TERMINATE"));

        TaskEntity taskEntity = new TaskEntity("f02", lane1, "承認待ち", "SEQUENTIAL",
                "nablarch.integration.workflow.condition.CompletionCondition1");
        workflowDbAccessSupport.insertTaskEntity(
                taskEntity,
                new TaskEntity("f03", lane2, "上位者承認", "NONE", null),
                new TaskEntity("f04", lane1, "再申請", "NONE", null)
        );

        workflowDbAccessSupport.insertGatewayEntity(
                new GatewayEntity("g01", lane2, "承認・破棄分岐", "EXCLUSIVE")
        );

        workflowDbAccessSupport.insertSequenceEntity(
                new SequenceFlowEntity(process, "seq000001", "申請書作成", "f01", "f02", null),
                new SequenceFlowEntity(process, "seq000002", "承認依頼", "f02", "f03", null),
                new SequenceFlowEntity(process, "seq000003", "破棄・承認", "f03", "g01", null),
                new SequenceFlowEntity(process, "seq000004", "破棄", "g01", "f50",
                        "nablarch.integration.workflow.condition.EqFlowProceedCondition(var, 1000)"),
                new SequenceFlowEntity(process, "seq000005", "承認", "g01", "f99",
                        "nablarch.integration.workflow.condition.NeFlowProceedCondition(var, 1000)"),
                new SequenceFlowEntity(process, "seq000006", "引き戻し", "e01", "f04", null),
                new SequenceFlowEntity(process, "seq000007", "再申請", "f04", "f02", null)
        );

        BoundaryEventTriggerEntity boundaryEventTriggerEntity = new BoundaryEventTriggerEntity(process, "__1",
                "引き戻しメッセージ");
        workflowDbAccessSupport.insertBoundaryEventTriggerEntity(boundaryEventTriggerEntity);

        workflowDbAccessSupport.insertBoundaryEventEntity(
                new BoundaryEventEntity("e01", "引き戻し", lane1, taskEntity, boundaryEventTriggerEntity)
        );

        // ----- execute -----
        DatabaseWorkflowDefinitionLoader workflowLoader = SystemRepository.get("workflowLoader");

        // ----- assert result -----
        List<WorkflowDefinition> workflowDefinitions = workflowLoader.load();

        assertThat(workflowDefinitions.size(), is(1));
        WorkflowDefinition workflowDefinition = workflowDefinitions.get(0);
        assertThat(workflowDefinition.getWorkflowId(), is("proc1"));
        assertThat(workflowDefinition.getWorkflowName(), is("交通費申請"));
        assertThat(workflowDefinition.getVersion(), is(1));
        assertThat(workflowDefinition.getEffectiveDate(), is("20140725"));

        List<SequenceFlow> sequenceDefinitions = workflowDefinition.getSequenceFlows();
        assertThat(sequenceDefinitions.size(), is(7));
        assertThat(sequenceDefinitions.get(0).getSequenceFlowId(), is("seq000001"));
        assertThat(sequenceDefinitions.get(0).getSequenceFlowName(), is("申請書作成"));
        assertThat(sequenceDefinitions.get(0).getSourceFlowNodeId(), is("f01"));
        assertThat(sequenceDefinitions.get(0).getTargetFlowNodeId(), is("f02"));

        assertThat(sequenceDefinitions.get(1).getSequenceFlowId(), is("seq000002"));
        assertThat(sequenceDefinitions.get(1).getSequenceFlowName(), is("承認依頼"));
        assertThat(sequenceDefinitions.get(1).getSourceFlowNodeId(), is("f02"));
        assertThat(sequenceDefinitions.get(1).getTargetFlowNodeId(), is("f03"));

        assertThat(sequenceDefinitions.get(2).getSequenceFlowId(), is("seq000003"));
        assertThat(sequenceDefinitions.get(2).getSequenceFlowName(), is("破棄・承認"));
        assertThat(sequenceDefinitions.get(2).getSourceFlowNodeId(), is("f03"));
        assertThat(sequenceDefinitions.get(2).getTargetFlowNodeId(), is("g01"));

        assertThat(sequenceDefinitions.get(3).getSequenceFlowId(), is("seq000004"));
        assertThat(sequenceDefinitions.get(3).getSequenceFlowName(), is("破棄"));
        assertThat(sequenceDefinitions.get(3).getSourceFlowNodeId(), is("g01"));
        assertThat(sequenceDefinitions.get(3).getTargetFlowNodeId(), is("f50"));

        assertThat(sequenceDefinitions.get(4).getSequenceFlowId(), is("seq000005"));
        assertThat(sequenceDefinitions.get(4).getSequenceFlowName(), is("承認"));
        assertThat(sequenceDefinitions.get(4).getSourceFlowNodeId(), is("g01"));
        assertThat(sequenceDefinitions.get(4).getTargetFlowNodeId(), is("f99"));

        assertThat(sequenceDefinitions.get(5).getSequenceFlowId(), is("seq000006"));
        assertThat(sequenceDefinitions.get(5).getSequenceFlowName(), is("引き戻し"));
        assertThat(sequenceDefinitions.get(5).getSourceFlowNodeId(), is("e01"));
        assertThat(sequenceDefinitions.get(5).getTargetFlowNodeId(), is("f04"));

        assertThat(sequenceDefinitions.get(6).getSequenceFlowId(), is("seq000007"));
        assertThat(sequenceDefinitions.get(6).getSequenceFlowName(), is("再申請"));
        assertThat(sequenceDefinitions.get(6).getSourceFlowNodeId(), is("f04"));
        assertThat(sequenceDefinitions.get(6).getTargetFlowNodeId(), is("f02"));

        List<Event> events = workflowDefinition.getEvents();
        assertThat(events.size(), is(3));
        {
            Event event = events.get(0);
            assertThat(event.getFlowNodeId(), is("f01"));
            assertThat(event.getFlowNodeName(), is("開始イベント"));
            assertThat(event.getEventType(), is(Event.EventType.START));
            assertThat(event.getLaneId(), is("001"));
            assertThat(event.getNextFlowNodeId(null, null), is("f02"));
            List<SequenceFlow> sequenceFlows = event.getSequenceFlows();
            assertThat(sequenceFlows.size(), is(1));
            assertThat(sequenceFlows.get(0).getSequenceFlowId(), is("seq000001"));
        }

        {
            Event event = events.get(1);
            assertThat(event.getFlowNodeId(), is("f50"));
            assertThat(event.getFlowNodeName(), is("破棄中断"));
            assertThat(event.getEventType(), is(Event.EventType.TERMINATE));
            assertThat(event.getLaneId(), is("001"));
            assertThat(event.getNextFlowNodeId(null, null), is(nullValue()));
            List<SequenceFlow> sequenceFlows = event.getSequenceFlows();
            assertThat(sequenceFlows.size(), is(0));
        }

        {
            Event event = events.get(2);
            assertThat(event.getFlowNodeId(), is("f99"));
            assertThat(event.getFlowNodeName(), is("終了イベント"));
            assertThat(event.getEventType(), is(Event.EventType.TERMINATE));
            assertThat(event.getLaneId(), is("002"));
            assertThat(event.getSequenceFlows().size(), is(0));
            assertThat(event.getNextFlowNodeId(null, null), is(nullValue()));
            List<SequenceFlow> sequenceFlows = event.getSequenceFlows();
            assertThat(sequenceFlows.size(), is(0));
        }

        List<Task> tasks = workflowDefinition.getTasks();
        assertThat(tasks.size(), is(3));
        {
            Task task = tasks.get(0);
            assertThat(task.getFlowNodeId(), is("f02"));
            assertThat(task.getFlowNodeName(), is("承認待ち"));
            assertThat(task.getMultiInstanceType(), is(Task.MultiInstanceType.SEQUENTIAL));
            assertThat(task.getLaneId(), is("001"));
            assertThat(task.getNextFlowNodeId(null, null), is("f03"));

            List<SequenceFlow> sequenceFlows = task.getSequenceFlows();
            assertThat(sequenceFlows.size(), is(1));
            assertThat(sequenceFlows.get(0).getSequenceFlowId(), is("seq000002"));
        }

        {
            Task task = tasks.get(1);
            assertThat(task.getFlowNodeId(), is("f03"));
            assertThat(task.getFlowNodeName(), is("上位者承認"));
            assertThat(task.getMultiInstanceType(), is(Task.MultiInstanceType.NONE));
            assertThat(task.getLaneId(), is("002"));
            assertThat(task.getNextFlowNodeId(null, null), is("g01"));

            List<SequenceFlow> sequenceFlows = task.getSequenceFlows();
            assertThat(sequenceFlows.size(), is(1));
            assertThat(sequenceFlows.get(0).getSequenceFlowId(), is("seq000003"));
        }

        {
            Task task = tasks.get(2);
            assertThat(task.getFlowNodeId(), is("f04"));
            assertThat(task.getFlowNodeName(), is("再申請"));
            assertThat(task.getMultiInstanceType(),
                    is(Task.MultiInstanceType.NONE));
            assertThat(task.getLaneId(), is("001"));
            assertThat(task.getNextFlowNodeId(null, null), is("f02"));

            List<SequenceFlow> sequenceFlows = task.getSequenceFlows();
            assertThat(sequenceFlows.size(), is(1));
            assertThat(sequenceFlows.get(0).getSequenceFlowId(), is("seq000007"));
        }

        List<Gateway> gateways = workflowDefinition.getGateways();
        assertThat(gateways.size(), is(1));

        {
            Gateway gateway = gateways.get(0);
            assertThat(gateway.getFlowNodeId(), is("g01"));
            assertThat(gateway.getFlowNodeName(), is("承認・破棄分岐"));
            assertThat(gateway.getGatewayType(), is(Gateway.GatewayType.EXCLUSIVE));
            assertThat(gateway.getLaneId(), is("002"));

            Map<String, Integer> parameter = new HashMap<String, Integer>();
            parameter.put("var", 1000);
            assertThat(gateway.getNextFlowNodeId("1234512345", parameter), is("f50"));
            parameter.put("var", 1001);
            assertThat(gateway.getNextFlowNodeId("1234512345", parameter), is("f99"));

            List<SequenceFlow> sequenceFlows = gateway.getSequenceFlows();
            assertThat(sequenceFlows.size(), is(2));
            assertThat(sequenceFlows.get(0).getSequenceFlowId(), is("seq000004"));
            assertThat(sequenceFlows.get(1).getSequenceFlowId(), is("seq000005"));
        }

        List<Lane> laneDefinitions = workflowDefinition.getLanes();
        assertThat(laneDefinitions.size(), is(2));

        assertThat(laneDefinitions.get(0).getLaneId(), is("001"));
        assertThat(laneDefinitions.get(0).getLaneName(), is("申請者"));

        assertThat(laneDefinitions.get(1).getLaneId(), is("002"));
        assertThat(laneDefinitions.get(1).getLaneName(), is("承認者"));

        List<BoundaryEvent> boundaryEvents = workflowDefinition.getBoundaryEvents();
        assertThat(boundaryEvents.size(), is(1));
        {
            BoundaryEvent boundaryEvent = boundaryEvents.get(0);
            assertThat(boundaryEvent.getFlowNodeId(), is("e01"));
            assertThat(boundaryEvent.getFlowNodeName(), is("引き戻し"));
            assertThat(boundaryEvent.getBoundaryEventTriggerId(), is("__1"));
            assertThat(boundaryEvent.getBoundaryEventTriggerName(), is("引き戻しメッセージ"));
            assertThat(boundaryEvent.getAttachedTaskId(), is("f02"));
            assertThat(boundaryEvent.getLaneId(), is("001"));

            assertThat(boundaryEvent.getSequenceFlows().size(), is(1));
            assertThat(boundaryEvent.getSequenceFlows().get(0).getSequenceFlowId(), is("seq000006"));
        }

        List<String> messages = OnMemoryLogWriter.getMessages("writer.workflow");
        assertThat("ログが出力されていること", messages.size(), is(1));
        assertThat(messages.get(0), containsString("INFO"));
        assertThat(messages.get(0), containsString("load workflow definition."));
        assertThat(messages.get(0), containsString("workflowId = [proc1], workflowName = [交通費申請], version = [1], effectiveDate = [20140725]"));

    }

    /**
     * 複数のバージョンが存在していた場合でも正しくロードできること
     *
     * @throws Exception
     */
    @Test
    public void testMultiVersion() throws Exception {
        //----- setup db -----
        WorkflowEntity processVer1 = new WorkflowEntity("proc1", 1, "交通費申請", "20120101");
        WorkflowEntity processVer2 = new WorkflowEntity("proc1", 2, "交通費申請バージョン2", "20140401");
        workflowDbAccessSupport.insertWorkflowEntity(processVer1, processVer2);

        LaneEntity lane1Ver1 = new LaneEntity(processVer1, "001", "申請者");
        LaneEntity lane2Ver1 = new LaneEntity(processVer1, "002", "承認者");
        LaneEntity lane3Ver1 = new LaneEntity(processVer1, "003", "その他");
        LaneEntity lane1Ver2 = new LaneEntity(processVer2, "a01", "申請者バージョン2");
        LaneEntity lane2Ver2 = new LaneEntity(processVer2, "a02", "承認者バージョン2");
        workflowDbAccessSupport.insertLaneEntity(lane1Ver1, lane2Ver1, lane3Ver1, lane1Ver2, lane2Ver2);

        workflowDbAccessSupport.insertEventEntity(
                new EventEntity("f01", lane1Ver1, "開始イベント", "START"),
                new EventEntity("f99", lane2Ver1, "終了イベント", "TERMINATE"),
                new EventEntity("f50", lane1Ver1, "破棄中断", "TERMINATE"),
                new EventEntity("faa", lane1Ver2, "開始イベントバージョン2", "START"),
                new EventEntity("fac", lane2Ver2, "終了イベントバージョン2", "TERMINATE"),
                new EventEntity("fab", lane1Ver2, "破棄中断バージョン2", "TERMINATE")
        );

        workflowDbAccessSupport.insertTaskEntity(
                new TaskEntity("f02", lane1Ver1, "申請", "NONE", null),
                new TaskEntity("f03", lane2Ver1, "承認", "NONE", null),
                new TaskEntity("fz2", lane1Ver2, "申請バージョン2", "NONE", null),
                new TaskEntity("fz3", lane2Ver2, "承認バージョン2", "NONE", null)
        );

        workflowDbAccessSupport.insertGatewayEntity(
                new GatewayEntity("g01", lane2Ver1, "承認・破棄分岐", "EXCLUSIVE"),
                new GatewayEntity("gz1", lane2Ver2, "承認・破棄分岐バージョン2", "EXCLUSIVE"),
                new GatewayEntity("gz2", lane2Ver2, "分岐", "EXCLUSIVE")
        );

        workflowDbAccessSupport.insertSequenceEntity(
                new SequenceFlowEntity(processVer1, "seq000001", "申請書作成", "f01", "f02", null),
                new SequenceFlowEntity(processVer1, "seq000002", "承認依頼", "f02", "f03", null),
                new SequenceFlowEntity(processVer1, "seq000003", "破棄・承認", "f03", "g01", null),
                new SequenceFlowEntity(processVer1, "seq000004", "破棄", "g01", "f50", null),
                new SequenceFlowEntity(processVer1, "seq000005", "承認", "g01", "f99", null),
                new SequenceFlowEntity(processVer2, "seqa00001", "申請書作成バージョン2", "f01", "f02", null),
                new SequenceFlowEntity(processVer2, "seqa00002", "承認依頼バージョン2", "f02", "f03", null),
                new SequenceFlowEntity(processVer2, "seqa00003", "破棄・承認バージョン2", "f03", "g01", null),
                new SequenceFlowEntity(processVer2, "seqa00004", "破棄バージョン2", "g01", "f50", null),
                new SequenceFlowEntity(processVer2, "seqa00005", "承認バージョン2", "g01", "f99", null)
        );

        // ----- execute -----
        DatabaseWorkflowDefinitionLoader workflowLoader = SystemRepository.get("workflowLoader");

        // ----- assert result -----
        List<WorkflowDefinition> workflowDefinitions = workflowLoader.load();
        // プロセスホルダーの内部状態をアサートする。
        assertThat(workflowDefinitions.size(), is(2));

        // バージョン1
        WorkflowDefinition version1Process = workflowDefinitions.get(0);
        assertThat(version1Process.getWorkflowId(), is("proc1"));
        assertThat(version1Process.getWorkflowName(), is("交通費申請"));
        assertThat(version1Process.getVersion(), is(1));
        List<Lane> version1Lane = version1Process.getLanes();
        assertThat(version1Lane.size(), is(3));
        assertThat(version1Lane.get(0).getLaneId(), is("001"));
        assertThat(version1Lane.get(0).getLaneName(), is("申請者"));
        assertThat(version1Lane.get(1).getLaneId(), is("002"));
        assertThat(version1Lane.get(1).getLaneName(), is("承認者"));
        assertThat(version1Lane.get(2).getLaneId(), is("003"));
        assertThat(version1Lane.get(2).getLaneName(), is("その他"));

        List<Event> version1Event = version1Process.getEvents();
        assertThat(version1Event.size(), is(3));
        assertThat(version1Event.get(0).getFlowNodeId(), is("f01"));
        assertThat(version1Event.get(1).getFlowNodeId(), is("f50"));
        assertThat(version1Event.get(2).getFlowNodeId(), is("f99"));

        List<Task> version1Activity = version1Process.getTasks();
        assertThat(version1Activity.size(), is(2));
        assertThat(version1Activity.get(0).getFlowNodeId(), is("f02"));
        assertThat(version1Activity.get(1).getFlowNodeId(), is("f03"));

        List<Gateway> version1Gateway = version1Process.getGateways();
        assertThat(version1Gateway.size(), is(1));
        assertThat(version1Gateway.get(0).getFlowNodeId(), is("g01"));

        List<SequenceFlow> version1Sequence = version1Process.getSequenceFlows();
        assertThat(version1Sequence.size(), is(5));
        assertThat(version1Sequence.get(0).getSequenceFlowId(), is("seq000001"));
        assertThat(version1Sequence.get(1).getSequenceFlowId(), is("seq000002"));
        assertThat(version1Sequence.get(2).getSequenceFlowId(), is("seq000003"));
        assertThat(version1Sequence.get(3).getSequenceFlowId(), is("seq000004"));
        assertThat(version1Sequence.get(4).getSequenceFlowId(), is("seq000005"));

        // バージョン2
        WorkflowDefinition version2Process = workflowDefinitions.get(1);
        assertThat(version2Process.getWorkflowId(), is("proc1"));
        assertThat(version2Process.getWorkflowName(), is("交通費申請バージョン2"));
        assertThat(version2Process.getVersion(), is(2));

        List<Lane> version2Lane = version2Process.getLanes();
        assertThat(version2Lane.size(), is(2));
        assertThat(version2Lane.get(0).getLaneId(), is("a01"));
        assertThat(version2Lane.get(1).getLaneId(), is("a02"));

        List<Event> version2Event = version2Process.getEvents();
        assertThat(version2Event.size(), is(3));
        assertThat(version2Event.get(0).getFlowNodeId(), is("faa"));
        assertThat(version2Event.get(1).getFlowNodeId(), is("fab"));
        assertThat(version2Event.get(2).getFlowNodeId(), is("fac"));

        List<Task> version2Activity = version2Process.getTasks();
        assertThat(version2Activity.size(), is(2));
        assertThat(version2Activity.get(0).getFlowNodeId(), is("fz2"));
        assertThat(version2Activity.get(1).getFlowNodeId(), is("fz3"));

        List<Gateway> version2Gateway = version2Process.getGateways();
        assertThat(version2Gateway.size(), is(2));
        assertThat(version2Gateway.get(0).getFlowNodeId(), is("gz1"));
        assertThat(version2Gateway.get(1).getFlowNodeId(), is("gz2"));

        List<SequenceFlow> version2Sequence = version2Process.getSequenceFlows();
        assertThat(version2Sequence.size(), is(5));
        assertThat(version2Sequence.get(0).getSequenceFlowId(), is("seqa00001"));
        assertThat(version2Sequence.get(1).getSequenceFlowId(), is("seqa00002"));
        assertThat(version2Sequence.get(2).getSequenceFlowId(), is("seqa00003"));
        assertThat(version2Sequence.get(3).getSequenceFlowId(), is("seqa00004"));
        assertThat(version2Sequence.get(4).getSequenceFlowId(), is("seqa00005"));

        List<String> messages = OnMemoryLogWriter.getMessages("writer.workflow");
        assertThat("ログが出力されていること", messages.size(), is(2));
        assertThat(messages.get(0), containsString("INFO"));
        assertThat(messages.get(0), containsString("load workflow definition."));
        assertThat(messages.get(0), containsString(
                "workflowId = [proc1], workflowName = [交通費申請], version = [1], effectiveDate = [20120101]"));

        assertThat(messages.get(1), containsString("INFO"));
        assertThat(messages.get(1), containsString("load workflow definition."));
        assertThat(messages.get(1), containsString(
                "workflowId = [proc1], workflowName = [交通費申請バージョン2], version = [2], effectiveDate = [20140401]"));

    }

    /**
     * 複数プロセスのロードができること
     */
    @Test
    public void testMultiProcess() throws Exception {

        //----- setup db -----
        WorkflowEntity proc1 = new WorkflowEntity("proc1", 1, "プロセス1", "20140401");
        WorkflowEntity proc2 = new WorkflowEntity("proc2", 2, "プロセス2", "20141001");
        workflowDbAccessSupport.insertWorkflowEntity(proc1, proc2);

        LaneEntity proc1Lane = new LaneEntity(proc1, "001", "申請者");
        LaneEntity proc2Lane = new LaneEntity(proc2, "a01", "申請者バージョン2");
        workflowDbAccessSupport.insertLaneEntity(proc1Lane, proc2Lane);

        workflowDbAccessSupport.insertEventEntity(
                new EventEntity("f01", proc1Lane, "開始イベント", "START"),
                new EventEntity("f02", proc1Lane, "終了イベント", "TERMINATE"),
                new EventEntity("aaa", proc2Lane, "開始", "START"),
                new EventEntity("zzz", proc2Lane, "終了", "TERMINATE")
        );

        workflowDbAccessSupport.insertTaskEntity(
                new TaskEntity("f03", proc1Lane, "申請", "NONE", null),
                new TaskEntity("aab", proc2Lane, "申請", "NONE", null)
        );

        workflowDbAccessSupport.insertGatewayEntity(
                new GatewayEntity("g01", proc1Lane, "承認・破棄分岐", "EXCLUSIVE")
        );

        workflowDbAccessSupport.insertSequenceEntity(
                new SequenceFlowEntity(proc1, "seq000001", "申請書作成", "f01", "f03", null),
                new SequenceFlowEntity(proc1, "seq000002", "承認依頼", "f03", "g01", null),
                new SequenceFlowEntity(proc1, "seq000003", "破棄・承認", "g01", "f02", null),
                new SequenceFlowEntity(proc2, "seqa00001", "シーケンス1", "aaa", "aab", null),
                new SequenceFlowEntity(proc2, "seqa00002", "シーケンス2", "aab", "zzz", null)
        );

        // ----- execute -----
        DatabaseWorkflowDefinitionLoader workflowLoader = SystemRepository.get("workflowLoader");

        // ----- assert result -----
        List<WorkflowDefinition> workflowDefinitions = workflowLoader.load();
        // プロセスホルダーの内部状態をアサートする。
        assertThat(workflowDefinitions.size(), is(2));

        assertThat(workflowDefinitions.get(0).getWorkflowId(), is("proc1"));
        assertThat(workflowDefinitions.get(1).getWorkflowId(), is("proc2"));

        List<String> messages = OnMemoryLogWriter.getMessages("writer.workflow");
        assertThat(messages.size(), is(2));
        assertThat(messages.get(0), containsString("INFO"));
        assertThat(messages.get(0), containsString(
                "workflowId = [proc1], workflowName = [プロセス1], version = [1], effectiveDate = [20140401]"));

        assertThat(messages.get(1), containsString("INFO"));
        assertThat(messages.get(1), containsString(
                "workflowId = [proc2], workflowName = [プロセス2], version = [2], effectiveDate = [20141001]"));
    }

    /**
     * シングルタスク(非マルチインスタンス)に、終了条件が指定されていた場合はエラーとなること。
     *
     * @throws Exception
     */
    @Test
    public void testSpecificConditionForSingleTask() throws Exception {

        //----- setup db -----
        WorkflowEntity proc1 = new WorkflowEntity("proc1", 1, "プロセス1", "20140101");
        workflowDbAccessSupport.insertWorkflowEntity(proc1);

        LaneEntity proc1Lane = new LaneEntity(proc1, "001", "申請者");
        workflowDbAccessSupport.insertLaneEntity(proc1Lane);

        workflowDbAccessSupport.insertEventEntity(
                new EventEntity("f01", proc1Lane, "開始イベント", "START"),
                new EventEntity("f02", proc1Lane, "終了イベント", "TERMINATE")
        );

        // シングルタスクに終了条件を指定
        workflowDbAccessSupport.insertTaskEntity(
                new TaskEntity("f03", proc1Lane, "申請", "NONE",
                        "nablarch.integration.workflow.condition.AllCompletionCondition")
        );

        workflowDbAccessSupport.insertGatewayEntity(
                new GatewayEntity("g01", proc1Lane, "承認・破棄分岐", "EXCLUSIVE")
        );

        workflowDbAccessSupport.insertSequenceEntity(
                new SequenceFlowEntity(proc1, "seq000001", "申請書作成", "f01", "f03", null),
                new SequenceFlowEntity(proc1, "seq000002", "承認依頼", "f03", "g01", null),
                new SequenceFlowEntity(proc1, "seq000003", "破棄・承認", "g01", "f02", null)
        );

        // ----- execute -----
        DatabaseWorkflowDefinitionLoader workflowLoader = SystemRepository.get("workflowLoader");

        // ----- assert result -----
        try {
            workflowLoader.load();
            fail("とおらない");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("Single Task must be CompletionCondition is empty."));
        }
    }
}



