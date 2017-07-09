package nablarch.integration.workflow.integration;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;

import org.hamcrest.Matchers;

import nablarch.core.db.statement.SqlRow;
import nablarch.integration.workflow.WorkflowInstance;
import nablarch.integration.workflow.WorkflowManager;
import nablarch.integration.workflow.definition.Gateway;
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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * ステートマシンが扱えることを検証するテスト。
 */
public class StatemachineIntegrationTest {

    @ClassRule
    public static WorkflowTestRule workflowTestRule = new WorkflowTestRule(true);

    @BeforeClass
    public static void setupClass() throws Exception {
        WorkflowDbAccessSupport workflowDbAccessSupport = workflowTestRule.getWorkflowDao();
        workflowDbAccessSupport.cleanupAll();

        WorkflowEntity workflow = new WorkflowEntity("simple", 1, "シンプルなステートマシン", "20110101");

        LaneEntity lane1 = new LaneEntity(workflow, "lane", "レーン");

        EventEntity start = new EventEntity("start", lane1, "開始", "START");
        EventEntity terminate = new EventEntity("end", lane1, "TerminateEndEvent", "TERMINATE");

        TaskEntity task1 = new TaskEntity("task1", lane1, "タスク1", "NONE", null);
        TaskEntity task2 = new TaskEntity("task2", lane1, "タスク2", "NONE", null);

        final GatewayEntity gateway = new GatewayEntity("gateway", lane1, "gateway", "EXCLUSIVE");

        BoundaryEventTriggerEntity trigger = new BoundaryEventTriggerEntity(workflow, "t01", "t01");
        BoundaryEventEntity boundaryEvent = new BoundaryEventEntity("m1", "Message", lane1, task1, trigger);

        BoundaryEventTriggerEntity trigger2 = new BoundaryEventTriggerEntity(workflow, "t02", "t02");
        BoundaryEventEntity boundaryEvent2 = new BoundaryEventEntity("m2", "Message", lane1, task2, trigger2);

        workflowDbAccessSupport.insertWorkflowEntity(workflow);
        workflowDbAccessSupport.insertLaneEntity(lane1);
        workflowDbAccessSupport.insertEventEntity(start, terminate);
        workflowDbAccessSupport.insertTaskEntity(task1, task2);
        workflowDbAccessSupport.insertGatewayEntity(gateway);
        workflowDbAccessSupport.insertBoundaryEventTriggerEntity(trigger, trigger2);
        workflowDbAccessSupport.insertBoundaryEventEntity(boundaryEvent, boundaryEvent2);
        workflowDbAccessSupport.insertSequenceEntity(
                new SequenceFlowEntity(workflow, "s01", null, "start", "task1", null),
                new SequenceFlowEntity(workflow, "s02", null, "m1", "task2", null),
                new SequenceFlowEntity(workflow, "s03", null, "m2", "gateway", null),
                new SequenceFlowEntity(workflow, "s04", null, "gateway", "task1", "nablarch.integration.workflow.condition.EqFlowProceedCondition(var, 1)"),
                new SequenceFlowEntity(workflow, "s05", null, "gateway", "end", "nablarch.integration.workflow.condition.NeFlowProceedCondition(var, 1)")
        );
        workflowTestRule.reloadProcessDefinitions();
    }

    @Before
    public void setUp() throws Exception {
        workflowTestRule.getWorkflowDao()
                        .cleanup(
                                "WF_ACTIVE_GROUP_TASK",
                                "WF_ACTIVE_USER_TASK",
                                "WF_ACTIVE_FLOW_NODE",
                                "WF_INSTANCE_FLOW_NODE",
                                "WF_INSTANCE"
                        );
    }

    @Test
    public void シンプルなステートマシンが実現できること() throws Exception {
        
        // ********************************** start
        WorkflowInstance workflow = WorkflowManager.startInstance("simple");
        assertThat("タスク1がアクティブ", workflow.isActive("task1"), is(true));
        workflowTestRule.commit();
        assertThat("DBでもタスク1がアクティブになっていること", workflowTestRule.getWorkflowDao()
                                                             .findActiveFlowNode()
                                                             .get(0)
                                                             .getString("FLOW_NODE_ID"), is("task1"));
        assertThat("人は割り当てないのでアクティブユーザは空", workflowTestRule.getWorkflowDao()
                                                           .findActiveUserTask(), empty());
        assertThat("グループは割り当てないのでアクティブユーザは空", workflowTestRule.getWorkflowDao()
                                                              .findActiveGroupTask(), empty());

        // ********************************** trigger
        workflow.triggerEvent("t01");
        workflowTestRule.commit();
        assertThat("タスク2がアクティブ", workflow.isActive("task2"), is(true));
        assertThat("DBでもタスク2がアクティブになっていること", workflowTestRule.getWorkflowDao()
                                                             .findActiveFlowNode()
                                                             .get(0)
                                                             .getString("FLOW_NODE_ID"), is("task2"));
        
        // ********************************** trigger
        workflow.triggerEvent("t02", Collections.singletonMap("var", "1"));
        assertThat("gatewayによりタスク1がアクティブ", workflow.isActive("task1"), is(true));
        workflowTestRule.commit();
        assertThat("DBでもタスク1がアクティブになっていること", workflowTestRule.getWorkflowDao()
                                                             .findActiveFlowNode()
                                                             .get(0)
                                                             .getString("FLOW_NODE_ID"), is("task1"));
        
        workflow.triggerEvent("t01");
        assertThat("タスク2がアクティブ", workflow.isActive("task2"), is(true));

        workflow.triggerEvent("t02", Collections.singletonMap("var", "2"));
        assertThat("完了していること", workflow.isCompleted(), is(true));
        workflowTestRule.commit();
        assertThat("完了したのでアクティブタスクはなし", workflowTestRule.getWorkflowDao()
                                                             .findActiveFlowNode(), empty());
    }
}
