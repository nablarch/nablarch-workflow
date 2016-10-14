package nablarch.integration.workflow.definition;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collections;
import java.util.Map;

import nablarch.core.db.statement.SqlResultSet;

import nablarch.integration.workflow.WorkflowConfig;
import nablarch.integration.workflow.testhelper.WorkflowTestRule;
import nablarch.integration.workflow.testhelper.entity.LaneEntity;
import nablarch.integration.workflow.testhelper.entity.WorkflowEntity;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import nablarch.integration.workflow.dao.WorkflowInstanceDao;
import nablarch.integration.workflow.testhelper.WorkflowDbAccessSupport;
import nablarch.integration.workflow.testhelper.entity.EventEntity;

public class EventTest {

    @ClassRule
    public static WorkflowTestRule rule = new WorkflowTestRule(true);
    private static WorkflowDbAccessSupport db;

    private static final String WORKFLOW_ID = "WF001";
    private static final String START_EVENT = "e01";
    private static final String TERMINATE_EVENT = "e02";

    @BeforeClass
    public static void setupDb() {
        db = rule.getWorkflowDao();
        prepareWorkflowDefinition();
    }

    @Before
    public void cleanupDb() {
        db.cleanup(
                "WF_INSTANCE"
        );
    }

    // ----- support methods -----
    private static void prepareWorkflowDefinition() {
        db.cleanupAll();
        // スタートイベント後にタスクがあるパターン
        {
            // バージョンが正しく選択されることをテストするために、複数バージョンを用意しておく。
            WorkflowEntity workflow = new WorkflowEntity(WORKFLOW_ID, 1L, "汎用のワークフロー定義", "19700101");
            db.insertWorkflowEntity(workflow);
            LaneEntity lane = new LaneEntity(workflow, "l01", "Lane");
            db.insertLaneEntity(lane);
            db.insertEventEntity(
                    new EventEntity(START_EVENT, lane, "StartEvent", "START"),
                    new EventEntity(TERMINATE_EVENT, lane, "TerminateEvent", "TERMINATE")
            );
        }
        rule.reloadProcessDefinitions();
    }

    /**
     * スタートイベントの場合の {@link Event#activate(String, Map)} のテスト。
     */
    @Test
    public void testActivateStartEvent() throws Exception {
        String instanceId = prepareWorkflowInstance(WORKFLOW_ID);
        FlowNode sut = getWorkflowDefinition(WORKFLOW_ID).findFlowNode(START_EVENT);
        rule.commit();

        sut.activate(instanceId, Collections.<String, Object>emptyMap());
        rule.commit();

        SqlResultSet instance = db.findWorkflowInstance();
        assertThat("スタートイベントのアクティブ化の際には、インスタンス情報は削除されないこと。", instance.get(0).getString("INSTANCE_ID"), is(instanceId));
    }

    /**
     * 停止イベントの場合の {@link Event#activate(String, Map)} のテスト。
     */
    @Test
    public void testActivateTerminateEvent() throws Exception {
        String instanceId = prepareWorkflowInstance(WORKFLOW_ID);
        FlowNode sut = getWorkflowDefinition(WORKFLOW_ID).findFlowNode(TERMINATE_EVENT);
        rule.commit();

        sut.activate(instanceId, Collections.<String, Object>emptyMap());
        rule.commit();

        SqlResultSet instance = db.findWorkflowInstance();
        assertThat("スタートイベントのアクティブ化の際には、インスタンス情報が削除されていること。", instance.size(), is(0));
    }

    /**
     * ワークフローインスタンス情報をデータベースに登録して、インスタンスIDを返却する。
     *
     * @param workflowId 作成するワークフローインスタンスのワークフローID
     * @return 作成されたワークフローインスタンスのインスタンスID
     */
    private String prepareWorkflowInstance(String workflowId) {
        WorkflowDefinition definition = WorkflowConfig.get().getWorkflowDefinitionHolder().getWorkflowDefinition(workflowId);
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        return dao.createWorkflowInstance(definition.getWorkflowId(), definition.getVersion(), definition.getTasks());
    }

    /**
     * ワークフローインスタンス系テーブルへのアクセスクラスを取得する。
     *
     * @return ワークフローインスタンスDAO
     */
    protected WorkflowInstanceDao getWorkflowInstanceDao() {
        return WorkflowConfig.get().getWorkflowInstanceDao();
    }

    /**
     * ワークフロー定義を取得する。
     *
     * @return ワークフロー定義
     */
    protected WorkflowDefinition getWorkflowDefinition(String workflowId) {
        return WorkflowConfig.get().getWorkflowDefinitionHolder().getWorkflowDefinition(workflowId);
    }
}
