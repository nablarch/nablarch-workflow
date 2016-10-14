package nablarch.integration.workflow.dao;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nablarch.integration.workflow.WorkflowConfig;
import nablarch.integration.workflow.definition.Task;
import nablarch.integration.workflow.testhelper.WorkflowTestRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import nablarch.core.db.statement.SqlResultSet;
import nablarch.core.db.statement.SqlRow;
import nablarch.core.repository.SystemRepository;

import nablarch.integration.workflow.definition.SequenceFlow;
import nablarch.integration.workflow.definition.WorkflowDefinition;
import nablarch.integration.workflow.definition.WorkflowDefinitionHolder;
import nablarch.integration.workflow.testhelper.WorkflowDbAccessSupport;

/**
 * {@link WorkflowInstanceDao}のテストクラス。
 */
public class WorkflowInstanceDaoTest {

    @ClassRule
    public static WorkflowTestRule workflowTestRule = new WorkflowTestRule();

    private WorkflowDbAccessSupport workflowDbAccessSupport;

    @Before
    public void setUp() throws Exception {
        workflowTestRule.commit();
        workflowDbAccessSupport = workflowTestRule.getWorkflowDao();
        workflowDbAccessSupport.cleanupAll();
    }

    /**
     * プロセスインスタンステーブルにデータが登録できること。
     */
    @Test
    public void testCreateProcessInstance() throws Exception {

        // ----- setup -----
        workflowDbAccessSupport.createSimpleProcess();
        WorkflowConfig config = WorkflowConfig.get();
        WorkflowDefinitionHolder workflowDefinitionHolder = config.getWorkflowDefinitionHolder();
        workflowDefinitionHolder.initialize();
        WorkflowDefinition workflowDefinition = workflowDefinitionHolder.getWorkflowDefinition("12345");

        // ----- execute -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        String instanceId = workflowInstanceDao.createWorkflowInstance(
                workflowDefinition.getWorkflowId(), workflowDefinition.getVersion(), workflowDefinition.getTasks());

        // ----- database transaction commit -----
        workflowTestRule.commit();

        // ----- assert result -----
        System.out.println("instanceId = " + instanceId);
        assertThat(instanceId.substring(0, 2), is("01"));
        assertThat(instanceId.length(), is(10));

        // ----- assert wf_instance table -----
        SqlResultSet allInstance = workflowDbAccessSupport.findWorkflowInstance();
        assertThat(allInstance.size(), is(1));
        assertThat("insert処理で返却された値が登録されていること", allInstance.get(0).getString("instance_id"), is(instanceId));
        assertThat(allInstance.get(0).getString("workflow_id"), is("12345"));
        assertThat(allInstance.get(0).getBigDecimal("def_version").longValue(), is(10L));

        // ----- assert wf_instance_flow_node table -----
        SqlResultSet allInstanceFlowNode = workflowDbAccessSupport.findInstanceFlowNode();
        assertThat(allInstanceFlowNode.size(), is(2));
        assertThat(allInstanceFlowNode.get(0).getString("instance_id"), is(instanceId));
        assertThat(allInstanceFlowNode.get(0).getString("flow_node_id"), is("a01"));
        assertThat(allInstanceFlowNode.get(0).getString("workflow_id"), is("12345"));
        assertThat(allInstanceFlowNode.get(0).getBigDecimal("def_version").longValue(), is(10L));

        assertThat(allInstanceFlowNode.get(1).getString("instance_id"), is(instanceId));
        assertThat(allInstanceFlowNode.get(1).getString("flow_node_id"), is("a02"));
        assertThat(allInstanceFlowNode.get(1).getString("workflow_id"), is("12345"));
        assertThat(allInstanceFlowNode.get(1).getBigDecimal("def_version").longValue(), is(10L));
    }

    /**
     * 担当者の保存ができること。
     */
    @Test
    public void testSaveAssignedUser() {
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();

        workflowInstanceDao.saveAssignedUser("1234512345", "a01", Arrays.asList("1234567890"));

        workflowTestRule.commit();

        SqlResultSet user = workflowDbAccessSupport.findAssignedUser();
        assertThat(user.size(), is(1));
        assertThat(user.get(0).getString("instance_id"), is("1234512345"));
        assertThat(user.get(0).getString("flow_node_id"), is("a01"));
        assertThat(user.get(0).getString("assigned_user_id"), is("1234567890"));
        assertThat(user.get(0).getBigDecimal("execution_order").intValue(), is(0));
    }

    /**
     * 複数の担当者の保存ができること。
     */
    @Test
    public void testSaveMultiAssignedUser() {

        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        workflowInstanceDao.saveAssignedUser("1234512345", "a01", Arrays.asList(
                        "0000000001", "0000000002", "0000000003")
        );
        workflowTestRule.commit();

        SqlResultSet user = workflowDbAccessSupport.findAssignedUser();
        assertThat(user.size(), is(3));
        assertThat(user.get(0).getString("instance_id"), is("1234512345"));
        assertThat(user.get(0).getString("flow_node_id"), is("a01"));
        assertThat(user.get(0).getString("assigned_user_id"), is("0000000001"));
        assertThat(user.get(0).getBigDecimal("execution_order").intValue(), is(0));

        assertThat(user.get(1).getString("instance_id"), is("1234512345"));
        assertThat(user.get(1).getString("flow_node_id"), is("a01"));
        assertThat(user.get(1).getString("assigned_user_id"), is("0000000002"));
        assertThat(user.get(1).getBigDecimal("execution_order").intValue(), is(0));

        assertThat(user.get(2).getString("instance_id"), is("1234512345"));
        assertThat(user.get(2).getString("flow_node_id"), is("a01"));
        assertThat(user.get(2).getString("assigned_user_id"), is("0000000003"));
        assertThat(user.get(2).getBigDecimal("execution_order").intValue(), is(0));
    }

    /**
     * 複数の担当者で処理順序が設定されている場合でも保存ができること。
     */
    @Test
    public void testSaveSequentialMultiAssignedUser() {
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        workflowInstanceDao.saveAssignedSequentialUser("1234512345", "a01",
                Arrays.asList("0000000001", "0000000002", "0000000003")
        );
        workflowTestRule.commit();

        SqlResultSet user = workflowDbAccessSupport.findAssignedUser();
        assertThat(user.size(), is(3));
        assertThat(user.get(0).getString("instance_id"), is("1234512345"));
        assertThat(user.get(0).getString("flow_node_id"), is("a01"));
        assertThat(user.get(0).getString("assigned_user_id"), is("0000000001"));
        assertThat(user.get(0).getBigDecimal("execution_order").intValue(), is(1));

        assertThat(user.get(1).getString("instance_id"), is("1234512345"));
        assertThat(user.get(1).getString("flow_node_id"), is("a01"));
        assertThat(user.get(1).getString("assigned_user_id"), is("0000000002"));
        assertThat(user.get(1).getBigDecimal("execution_order").intValue(), is(2));

        assertThat(user.get(2).getString("instance_id"), is("1234512345"));
        assertThat(user.get(2).getString("flow_node_id"), is("a01"));
        assertThat(user.get(2).getString("assigned_user_id"), is("0000000003"));
        assertThat(user.get(2).getBigDecimal("execution_order").intValue(), is(3));

    }

    /**
     * 担当者がすでに登録されている場合、今回指定した値で上書きされること。
     */
    @Test
    public void testOverrideAssignUser() {
        // ----- setup -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        // 削除対象外
        workflowInstanceDao.saveAssignedUser("0000000001", "111", Arrays.asList("1111111111"));
        workflowInstanceDao.saveAssignedUser("0000000003", "333", Arrays.asList("3333333333"));
        workflowInstanceDao.saveAssignedUser("0000000002", "__1", Arrays.asList("1111111111"));
        workflowInstanceDao.saveAssignedUser("0000000002", "__3", Arrays.asList("3333333333"));
        // 削除対象
        workflowInstanceDao.saveAssignedUser("0000000002", "__2", Arrays.asList("2222222222"));
        workflowTestRule.commit();

        // ----- execute -----
        workflowInstanceDao.saveAssignedUser("0000000002", "__2", Arrays.asList(
                "9999999999", "8888888888"));
        workflowTestRule.commit();

        // ----- assert -----
        SqlResultSet users = workflowDbAccessSupport.findAssignedUser();
        assertThat(users.size(), is(6));
        assertThat("インスタンスIDが異なるので削除されないデータ", users.get(0).getString("instance_id"), is("0000000001"));
        assertThat("インスタンスIDが異なるので削除されないデータ", users.get(0).getString("flow_node_id"), is("111"));
        assertThat("インスタンスIDが異なるので削除されないデータ", users.get(5).getString("instance_id"), is("0000000003"));
        assertThat("インスタンスIDが異なるので削除されないデータ", users.get(5).getString("flow_node_id"), is("333"));

        assertThat("フローノードIDが異なるので削除されないデータ", users.get(1).getString("instance_id"), is("0000000002"));
        assertThat("フローノードIDが異なるので削除されないデータ", users.get(1).getString("flow_node_id"), is("__1"));
        assertThat("フローノードIDが異なるので削除されないデータ", users.get(4).getString("instance_id"), is("0000000002"));
        assertThat("フローノードIDが異なるので削除されないデータ", users.get(4).getString("flow_node_id"), is("__3"));

        assertThat(users.get(2).getString("instance_id"), is("0000000002"));
        assertThat(users.get(2).getString("flow_node_id"), is("__2"));
        assertThat(users.get(2).getString("assigned_user_id"), is("8888888888"));
        assertThat(users.get(2).getBigDecimal("execution_order").intValue(), is(0));

        assertThat(users.get(3).getString("instance_id"), is("0000000002"));
        assertThat(users.get(3).getString("flow_node_id"), is("__2"));
        assertThat(users.get(3).getString("assigned_user_id"), is("9999999999"));
        assertThat(users.get(3).getBigDecimal("execution_order").intValue(), is(0));
    }

    /**
     * 担当者を割り当てた場合、同一のフローノードに割り当てられているグループ情報は削除されること。
     */
    @Test
    public void testSaveAssignUserDeleteGroup() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        workflowInstanceDao.saveAssignedGroup("0000000001", "__1", Arrays.asList("AAAAAAAAAA"));
        workflowInstanceDao.saveAssignedGroup("0000000002", "__1", Arrays.asList("AAAAAAAAAA"));
        workflowInstanceDao.saveAssignedGroup("0000000002", "__2", Arrays.asList("AAAAAAAAAA"));
        workflowInstanceDao.saveAssignedGroup("0000000002", "__3", Arrays.asList("AAAAAAAAAA"));
        workflowInstanceDao.saveAssignedGroup("0000000003", "__1", Arrays.asList("AAAAAAAAAA"));
        workflowTestRule.commit();

        // ----- execute -----
        workflowInstanceDao.saveAssignedUser("0000000002", "__2", Arrays.asList(
                "9999999999", "8888888888"));
        workflowTestRule.commit();

        // ----- assert -----
        assertThat(workflowDbAccessSupport.findAssignedGroup().size(), is(4));
        assertThat(workflowDbAccessSupport.findAssignedUser().size(), is(2));
    }

    /**
     * グループの割り当てができること。
     */
    @Test
    public void testSaveAssignGroup() {
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        workflowInstanceDao.saveAssignedGroup("1234554321", "a01", Arrays.asList("9999999999"));
        workflowTestRule.commit();

        SqlResultSet group = workflowDbAccessSupport.findAssignedGroup();
        assertThat(group.size(), is(1));

        assertThat(group.get(0).getString("flow_node_id"), is("a01"));
        assertThat(group.get(0).getString("assigned_group_id"), is("9999999999"));
    }

    /**
     * 複数のグループの割り当てができること
     */
    @Test
    public void testSaveMultiAssignedGroup() {
        WorkflowInstanceDao sut = getWorkflowInstanceDao();
        sut.saveAssignedGroup("1234512345", "aaa", Arrays.asList("aaaaaaaaaa", "bbbbbbbbbb"));
        workflowTestRule.commit();

        SqlResultSet group = workflowDbAccessSupport.findAssignedGroup();
        assertThat(group.size(), is(2));
        assertThat(group.get(0).getString("flow_node_id"), is("aaa"));
        assertThat(group.get(0).getString("assigned_group_id"), is("aaaaaaaaaa"));

        assertThat(group.get(1).getString("flow_node_id"), is("aaa"));
        assertThat(group.get(1).getString("assigned_group_id"), is("bbbbbbbbbb"));
    }

    /**
     * 複数のグループで処理順序が設定されている場合、その順序を実行順として担当グループ情報が保存されること。
     *
     * @throws Exception
     */
    @Test
    public void testSaveSequentialMultiAssignedGroup() throws Exception {
        WorkflowInstanceDao sut = getWorkflowInstanceDao();
        sut.saveAssignedSequentialGroup("1234512345", "bbb",
                Arrays.asList("bbbbbbbbbb", "aaaaaaaaaa", "cccccccccc"));
        workflowTestRule.commit();

        SqlResultSet group = workflowDbAccessSupport.findAssignedGroup();
        assertThat(group.size(), is(3));

        assertThat(group.get(0).getString("flow_node_id"), is("bbb"));
        assertThat(group.get(0).getString("assigned_group_id"), is("aaaaaaaaaa"));
        assertThat(group.get(0).getBigDecimal("execution_order").intValue(), is(2));

        assertThat(group.get(1).getString("flow_node_id"), is("bbb"));
        assertThat(group.get(1).getString("assigned_group_id"), is("bbbbbbbbbb"));
        assertThat(group.get(1).getBigDecimal("execution_order").intValue(), is(1));

        assertThat(group.get(2).getString("flow_node_id"), is("bbb"));
        assertThat(group.get(2).getString("assigned_group_id"), is("cccccccccc"));
        assertThat(group.get(2).getBigDecimal("execution_order").intValue(), is(3));
    }

    /**
     * グループが既に割り当てられている場合、今回指定した値で上書きされること。
     */
    @Test
    public void testOverrideAssignedGroup() {

        // ----- setup -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        // 削除対象外
        workflowInstanceDao.saveAssignedGroup("0000000001", "__1", Arrays.asList("0000000000"));
        workflowInstanceDao.saveAssignedGroup("0000000003", "__3", Arrays.asList("3333333333"));
        // 削除対象
        workflowInstanceDao.saveAssignedGroup("0000000002", "a01", Arrays.asList("9999999999"));
        workflowTestRule.commit();

        // ----- execute -----
        workflowInstanceDao.saveAssignedGroup("0000000002", "a01", Arrays.asList("1234567890"));
        workflowTestRule.commit();

        // ----- assert -----
        SqlResultSet user = workflowDbAccessSupport.findAssignedGroup();
        assertThat(user.size(), is(3));
        assertThat("異なるインスタンスIDのデータはそのまま", user.get(0).getString("instance_id"), is("0000000001"));
        assertThat("異なるインスタンスIDのデータはそのまま", user.get(0).getString("flow_node_id"), is("__1"));
        assertThat("異なるインスタンスIDのデータはそのまま", user.get(2).getString("instance_id"), is("0000000003"));
        assertThat("異なるインスタンスIDのデータはそのまま", user.get(2).getString("flow_node_id"), is("__3"));

        assertThat("洗替されること", user.get(1).getString("instance_id"), is("0000000002"));
        assertThat("洗替されること", user.get(1).getString("flow_node_id"), is("a01"));
        assertThat("洗替されること", user.get(1).getString("assigned_group_id"), is("1234567890"));
    }

    /**
     * グループを割り当てた場合、同一フローノードに割り当てられているユーザ情報が削除されること。
     *
     * @throws Exception
     */
    @Test
    public void testSaveAssignedGroupDeleteUser() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        workflowInstanceDao.saveAssignedUser("0000000001", "__1", Arrays.asList("1111111111"));
        workflowInstanceDao.saveAssignedUser("0000000002", "__1", Arrays.asList("1111111111"));
        workflowInstanceDao.saveAssignedUser("0000000002", "__2", Arrays.asList("0000000000", "1111111111"));
        workflowInstanceDao.saveAssignedUser("0000000002", "__3", Arrays.asList("1111111111"));
        workflowInstanceDao.saveAssignedUser("0000000003", "__4", Arrays.asList("1111111111"));
        workflowTestRule.commit();

        // ----- execute -----
        workflowInstanceDao.saveAssignedGroup("0000000002", "__2", Arrays.asList("aaaaaaaaaa"));
        workflowTestRule.commit();

        // ----- assert -----
        assertThat(workflowDbAccessSupport.findAssignedUser().size(), is(4));
        assertThat(workflowDbAccessSupport.findAssignedGroup().size(), is(1));
    }

    /**
     * アクティブなフローノードの状態が保存できること。
     */
    @Test
    public void testSaveActiveFlowNode() throws Exception {
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        workflowInstanceDao.saveActiveFlowNode("1234512345", new Task(
                "___", "最初のあくてぃびてぃ", null, "NONE",
                null, Collections.<SequenceFlow>emptyList()));
        workflowTestRule.commit();

        SqlResultSet activeFlowNode = workflowDbAccessSupport.findActiveFlowNode();
        assertThat(activeFlowNode.size(), is(1));
        assertThat(activeFlowNode.get(0).getString("instance_id"), is("1234512345"));
        assertThat(activeFlowNode.get(0).getString("flow_node_id"), is("___"));
    }

    /**
     * アクティブなフローノードが登録済みの場合、指定したノードで上書きされること。
     */
    @Test
    public void testOverrideActiveFlowNode() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        // 削除対象外
        workflowInstanceDao.saveActiveFlowNode("0000000001",
                new Task("001", "削除されない", null, "NONE", null, Collections.<SequenceFlow>emptyList()));
        workflowInstanceDao.saveActiveFlowNode("0000000003",
                new Task("999", "削除されない", null, "NONE", null, Collections.<SequenceFlow>emptyList()));
        // 削除対象
        workflowInstanceDao.saveActiveFlowNode("0000000002",
                new Task("__1", "最初", null, "NONE", null, Collections.<SequenceFlow>emptyList()));
        workflowInstanceDao.saveActiveUserTask("0000000002", "__1", "9999999999", 1);
        workflowInstanceDao.saveActiveGroupTask("0000000002", "__1", Arrays.asList("8888888888"));
        workflowTestRule.commit();

        // ----- execute -----
        workflowInstanceDao.saveActiveFlowNode("0000000002", new Task(
                "_99", "上書き", null, "NONE", null, Collections.<SequenceFlow>emptyList()));
        workflowTestRule.commit();

        // ----- assert -----
        SqlResultSet flowNode = workflowDbAccessSupport.findActiveFlowNode();
        assertThat(flowNode.size(), is(3));
        assertThat("削除されないデータ", flowNode.get(0).getString("instance_id"), is("0000000001"));
        assertThat("削除されないデータ", flowNode.get(0).getString("flow_node_id"), is("001"));
        assertThat("削除されないデータ", flowNode.get(2).getString("instance_id"), is("0000000003"));
        assertThat("削除されないデータ", flowNode.get(2).getString("flow_node_id"), is("999"));

        assertThat(flowNode.get(1).getString("instance_id"), is("0000000002"));
        assertThat(flowNode.get(1).getString("flow_node_id"), is("_99"));

        assertThat(workflowDbAccessSupport.findActiveUserTask().size(), is(0));
        assertThat(workflowDbAccessSupport.findActiveGroupTask().size(), is(0));
    }

    /**
     * アクティブタスクのユーザ情報が保存できること
     */
    @Test
    public void testSaveActiveUserTask() throws Exception {
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        workflowInstanceDao.saveActiveUserTask("1234512345", "___", Arrays.asList("aaaaaaaaaa"));

        workflowTestRule.commit();

        SqlResultSet userTask = workflowDbAccessSupport.findActiveUserTask();
        assertThat(userTask.size(), is(1));
        assertThat(userTask.get(0).getString("instance_id"), is("1234512345"));
        assertThat(userTask.get(0).getString("flow_node_id"), is("___"));
        assertThat(userTask.get(0).getString("assigned_user_id"), is("aaaaaaaaaa"));
        assertThat(userTask.get(0).getBigDecimal("execution_order").intValue(), is(0));
    }

    /**
     * アクティブタスクのユーザ情報が登録されている場合、
     * 削除後に登録されること
     */
    @Test
    public void testOverrideActiveUserTask() throws Exception {

        // ----- setup -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        // 削除対象外
        workflowInstanceDao.saveActiveUserTask("0000000001", "__1", Arrays.asList("aaaaaaaaaa"));
        workflowInstanceDao.saveActiveUserTask("0000000003", "__2", Arrays.asList("aaaaaaaaaa"));
        // 削除対象
        workflowInstanceDao.saveActiveUserTask("0000000002", "___", Arrays.asList("aaaaaaaaaa", "bbbbbbbbbb"));
        workflowTestRule.commit();

        // ----- execute -----
        workflowInstanceDao.saveActiveUserTask("0000000002", "__1", Arrays.asList("bbbbbbbbbb"));
        workflowTestRule.commit();

        SqlResultSet userTask = workflowDbAccessSupport.findActiveUserTask();
        assertThat(userTask.size(), is(3));
        assertThat("インスタンスIDが異なるのでそのまま", userTask.get(0).getString("instance_id"), is("0000000001"));
        assertThat("インスタンスIDが異なるのでそのまま", userTask.get(2).getString("instance_id"), is("0000000003"));

        assertThat(userTask.get(1).getString("instance_id"), is("0000000002"));
        assertThat(userTask.get(1).getString("flow_node_id"), is("__1"));
        assertThat(userTask.get(1).getString("assigned_user_id"), is("bbbbbbbbbb"));
        assertThat(userTask.get(1).getBigDecimal("execution_order").intValue(), is(0));
    }

    /**
     * 処理順序有りのアクティブユーザが保存できること。
     */
    @Test
    public void testSaveOrderActiveUserTask() throws Exception {

        // ----- execute -----
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        dao.saveActiveUserTask("0000000001", "001", "user000001", 99);
        workflowTestRule.commit();

        // ----- assert -----
        SqlResultSet activeUserTask = workflowDbAccessSupport.findActiveUserTask();
        assertThat(activeUserTask.size(), is(1));
        assertThat(activeUserTask.get(0).getString("instance_id"), is("0000000001"));
        assertThat(activeUserTask.get(0).getString("flow_node_id"), is("001"));
        assertThat(activeUserTask.get(0).getString("assigned_user_id"), is("user000001"));
        assertThat(activeUserTask.get(0).getBigDecimal("execution_order").intValue(), is(99));
    }


    /**
     * アクティブタスクのユーザ情報が登録されている場合、
     * 削除後に登録されること
     */
    @Test
    public void testOverrideOrderActiveUserTask() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        dao.saveActiveUserTask("0000000001", "001", Arrays.asList("9999999999"));
        dao.saveActiveUserTask("0000000002", "002", Arrays.asList("aaaaaaaaaa"));
        dao.saveActiveUserTask("0000000002", "002", Arrays.asList("bbbbbbbbbb"));
        dao.saveActiveUserTask("0000000003", "003", Arrays.asList("9999999999"));
        workflowTestRule.commit();

        // ----- execute -----
        dao.saveActiveUserTask("0000000002", "003", "cccccccccc", 50);
        workflowTestRule.commit();

        // ----- assert -----
        SqlResultSet activeUserTask = workflowDbAccessSupport.findActiveUserTask();
        assertThat(activeUserTask.size(), is(3));
        assertThat(activeUserTask.get(0).getString("instance_id"), is("0000000001"));
        assertThat(activeUserTask.get(2).getString("instance_id"), is("0000000003"));

        assertThat(activeUserTask.get(1).getString("instance_id"), is("0000000002"));
        assertThat(activeUserTask.get(1).getString("flow_node_id"), is("003"));
        assertThat(activeUserTask.get(1).getString("assigned_user_id"), is("cccccccccc"));
        assertThat(activeUserTask.get(1).getBigDecimal("execution_order").intValue(), is(50));
    }

    /**
     * アクティブユーザの情報が、ユーザを条件に削除できること。
     *
     * @throws Exception
     */
    @Test
    public void deleteActiveUserTaskByUser() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        dao.saveActiveUserTask("0000000001", "001", Arrays.asList("1111111111", "2222222222", "3333333333"));
        workflowTestRule.commit();

        // ----- execute -----
        dao.deleteActiveUserTaskByUserId("0000000001", "001", "2222222222");
        workflowTestRule.commit();

        // ----- assert -----
        SqlResultSet activeUserTask = workflowDbAccessSupport.findActiveUserTask();
        assertThat(activeUserTask.size(), is(2));
        assertThat(activeUserTask.get(0).getString("instance_id"), is("0000000001"));
        assertThat(activeUserTask.get(0).getString("flow_node_id"), is("001"));
        assertThat(activeUserTask.get(0).getString("assigned_user_id"), is("1111111111"));

        assertThat(activeUserTask.get(1).getString("instance_id"), is("0000000001"));
        assertThat(activeUserTask.get(1).getString("flow_node_id"), is("001"));
        assertThat(activeUserTask.get(1).getString("assigned_user_id"), is("3333333333"));
    }

    /**
     * アクティブグループ情報が保存できること
     */
    @Test
    public void testSaveActiveGroupTask() throws Exception {

        // ----- execute -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        workflowInstanceDao.saveActiveGroupTask("0000000001", "xxx", Arrays.asList("cccccccccc", "aaaaaaaaaa"));
        workflowTestRule.commit();

        SqlResultSet groupTask = workflowDbAccessSupport.findActiveGroupTask();
        assertThat(groupTask.size(), is(2));
        assertThat(groupTask.get(0).getString("instance_id"), is("0000000001"));
        assertThat(groupTask.get(0).getString("flow_node_id"), is("xxx"));
        assertThat(groupTask.get(0).getString("assigned_group_id"), is("aaaaaaaaaa"));
        assertThat(groupTask.get(0).getBigDecimal("execution_order").intValue(), is(0));

        assertThat(groupTask.get(1).getString("instance_id"), is("0000000001"));
        assertThat(groupTask.get(1).getString("flow_node_id"), is("xxx"));
        assertThat(groupTask.get(1).getString("assigned_group_id"), is("cccccccccc"));
        assertThat(groupTask.get(1).getBigDecimal("execution_order").intValue(), is(0));
    }

    /**
     * アクティブタスクのグループ情報が登録されている場合、
     * 削除後に登録されること
     */
    @Test
    public void testOverrideActiveGroupTask() {
        // ----- setup -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        // 削除対象外
        workflowInstanceDao.saveActiveGroupTask("0000000001", "111", Arrays.asList("1111111111"));
        workflowInstanceDao.saveActiveGroupTask("0000000003", "333", Arrays.asList("3333333333"));
        // 削除対象
        workflowInstanceDao.saveActiveGroupTask("0000000002", "__2", Arrays.asList("aaaaaaaaaa"));
        workflowTestRule.commit();

        // ----- execute -----
        workflowInstanceDao.saveActiveGroupTask("0000000002", "__3", Arrays.asList("bbbbbbbbbb"));
        workflowTestRule.commit();

        SqlResultSet groupTask = workflowDbAccessSupport.findActiveGroupTask();
        assertThat(groupTask.size(), is(3));
        assertThat("インスタンスIDが異なるので削除されないデータ", groupTask.get(0).getString("instance_id"), is("0000000001"));
        assertThat("インスタンスIDが異なるので削除されないデータ", groupTask.get(0).getString("flow_node_id"), is("111"));

        assertThat("インスタンスIDが異なるので削除されないデータ", groupTask.get(2).getString("instance_id"), is("0000000003"));
        assertThat("インスタンスIDが異なるので削除されないデータ", groupTask.get(2).getString("flow_node_id"), is("333"));

        assertThat(groupTask.get(1).getString("instance_id"), is("0000000002"));
        assertThat(groupTask.get(1).getString("flow_node_id"), is("__3"));
        assertThat(groupTask.get(1).getString("assigned_group_id"), is("bbbbbbbbbb"));
    }

    /**
     * 処理順序有りのアクティブグループが保存できること。
     */
    @Test
    public void testSaveOrderActiveGroupTask() throws Exception {

        // ----- setup -----
        WorkflowInstanceDao sut = getWorkflowInstanceDao();
        sut.saveActiveGroupTask("1234512345", "999", "group00001", 55);
        workflowTestRule.commit();

        // ----- assert -----
        SqlResultSet activeGroupTask = workflowDbAccessSupport.findActiveGroupTask();
        assertThat(activeGroupTask.size(), is(1));

        assertThat(activeGroupTask.get(0).getString("flow_node_id"), is("999"));
        assertThat(activeGroupTask.get(0).getString("assigned_group_id"), is("group00001"));
        assertThat(activeGroupTask.get(0).getBigDecimal("execution_order").intValue(), is(55));
    }

    /**
     * アクティブグループの情報がグループを条件に削除できること
     */
    @Test
    public void deleteActiveGroupTaskByGroup() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        dao.saveActiveGroupTask("0000000000", "001", Arrays.asList("1111111111"));
        dao.saveActiveGroupTask("0000000001", "001", Arrays.asList("1111111111", "2222222222", "3333333333"));
        dao.saveActiveGroupTask("0000000002", "001", Arrays.asList("1111111111"));
        workflowTestRule.commit();

        // ----- execute -----
        dao.deleteActiveGroupTaskByGroupId("0000000001", "001", "2222222222");
        workflowTestRule.commit();

        // ----- assert -----
        SqlResultSet activeGroupTask = workflowDbAccessSupport.findActiveGroupTask();
        assertThat(activeGroupTask.size(), is(4));
        assertThat(activeGroupTask.get(0).getString("instance_id"), is("0000000000"));

        assertThat(activeGroupTask.get(1).getString("instance_id"), is("0000000001"));
        assertThat(activeGroupTask.get(1).getString("assigned_group_id"), is("1111111111"));

        assertThat(activeGroupTask.get(2).getString("instance_id"), is("0000000001"));
        assertThat(activeGroupTask.get(2).getString("assigned_group_id"), is("3333333333"));

        assertThat(activeGroupTask.get(3).getString("instance_id"), is("0000000002"));
    }

    /**
     * プロセスインスタンスの状態が全てクリーニングできること。
     */
    @Test
    public void testDeleteInstance() throws Exception {

        // ----- setup -----
        String instanceId = createInstanceData();

        // ----- execute -----
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        dao.deleteInstance(instanceId);
        workflowTestRule.commit();

        // ----- assert instance -----
        SqlResultSet instance = workflowDbAccessSupport.findWorkflowInstance();
        assertThat(instance.size(), is(2));
        assertThat(instance.get(0).getString("workflow_id"), is("00001"));
        assertThat(instance.get(1).getString("workflow_id"), is("00003"));

        // ----- assert instance flow node -----
        SqlResultSet instanceFlowNode = workflowDbAccessSupport.findInstanceFlowNode();
        assertThat(instanceFlowNode.size(), is(2));
        for (SqlRow row : instanceFlowNode) {
            assertThat("削除対象のインスタンスIDの情報が削除されていること", row.getString("instance_id"), is(not(instanceId)));
        }

        // ----- assert assigned user -----
        SqlResultSet assignedUser = workflowDbAccessSupport.findAssignedUser();
        assertThat(assignedUser.size(), is(2));
        for (SqlRow row : assignedUser) {
            assertThat("削除対象のインスタンスIDの情報が削除されていること", row.getString("instance_id"), is(not(instanceId)));
        }

        // ----- assert assigned gruop -----
        SqlResultSet assignedGroup = workflowDbAccessSupport.findAssignedGroup();
        assertThat(assignedGroup.size(), is(2));
        for (SqlRow row : assignedGroup) {
            assertThat("削除対象のインスタンスIDの情報が削除されていること", row.getString("instance_id"), is(not(instanceId)));
        }

        // ----- assert active flow node -----
        SqlResultSet activeFlowNode = workflowDbAccessSupport.findActiveFlowNode();
        assertThat(activeFlowNode.size(), is(2));
        for (SqlRow row : activeFlowNode) {
            assertThat("削除対象のインスタンスIDの情報が削除されていること", row.getString("instance_id"), is(not(instanceId)));
        }

        // ----- assert active user task -----
        SqlResultSet activeUserTask = workflowDbAccessSupport.findActiveUserTask();
        assertThat(activeUserTask.size(), is(2));
        for (SqlRow row : activeUserTask) {
            assertThat("削除対象のインスタンスIDの情報が削除されていること", row.getString("instance_id"), is(not(instanceId)));
        }

        // ----- assert active grup task -----
        SqlResultSet activeGroupTask = workflowDbAccessSupport.findActiveGroupTask();
        assertThat(activeGroupTask.size(), is(2));
        for (SqlRow row : activeGroupTask) {
            assertThat("削除対象のインスタンスIDの情報が削除されていること", row.getString("instance_id"), is(not(instanceId)));
        }
    }

    /**
     * ワークフローインスタンの情報が取得できること
     */
    @Test
    public void testFindInstance() throws Exception {

        //----- setup -----
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        dao.createWorkflowInstance("00001", 1, Arrays.asList(
                new Task("001", "001", null, "NONE", null, Collections.<SequenceFlow>emptyList())));
        String instanceId = dao.createWorkflowInstance("00001", 999,
                Arrays.asList(new Task("001", "001", null, "NONE", null, Collections.<SequenceFlow>emptyList())));
        workflowTestRule.commit();

        // ----- execute -----
        WorkflowInstanceEntity instance = dao.findInstance(instanceId);

        // ----- assert -----
        assertThat(instance.getInstanceId(), is(instanceId));
        assertThat(instance.getWorkflowId(), is("00001"));
        assertThat(instance.getVersion(), is(999L));
    }

    /**
     * ワークフローインスタンスの情報が存在しない場合例外が発生すること。
     */
    @Test
    public void testFindInstanceNotFound() throws Exception {
        //----- setup -----
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        dao.createWorkflowInstance("00001", 1, Arrays.asList(new Task("001", "001", null, "NONE", null,
                Collections.<SequenceFlow>emptyList())));
        dao.createWorkflowInstance("00002", 1, Arrays.asList(new Task("001", "001", null, "NONE", null,
                Collections.<SequenceFlow>emptyList())));
        dao.createWorkflowInstance("00003", 1, Arrays.asList(new Task("001", "001", null, "NONE", null,
                Collections.<SequenceFlow>emptyList())));
        workflowTestRule.commit();

        // ----- execute -----
        assertThat(dao.findInstance("notfound"), is(nullValue()));
    }

    /**
     * タスク担当ユーザ情報が取得できること。
     */
    @Test
    public void testFindTaskAssignedUser() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        dao.saveAssignedUser("0000000001", "001", Arrays.asList("0000000001"));
        dao.saveAssignedUser("0000000002", "__1", Arrays.asList("0000000001"));
        dao.saveAssignedSequentialUser("0000000002", "__2", Arrays.asList("aaaaaaaaaa", "bbbbbbbbbb"));
        dao.saveAssignedUser("0000000002", "__3", Arrays.asList("0000000001"));
        dao.saveAssignedUser("0000000003", "001", Arrays.asList("0000000001"));
        workflowTestRule.commit();

        // ----- execute -----
        List<TaskAssignedUserEntity> actual = dao.findTaskAssignedUser("0000000002", "__2");

        // ----- assert -----
        assertThat(actual.size(), is(2));

        assertThat(actual.get(0).getInstanceId(), is("0000000002"));
        assertThat(actual.get(0).getFlowNodeId(), is("__2"));
        assertThat(actual.get(0).getUserId(), is("aaaaaaaaaa"));
        assertThat(actual.get(0).getExecutionOrder(), is(1));

        assertThat(actual.get(1).getInstanceId(), is("0000000002"));
        assertThat(actual.get(1).getFlowNodeId(), is("__2"));
        assertThat(actual.get(1).getUserId(), is("bbbbbbbbbb"));
        assertThat(actual.get(1).getExecutionOrder(), is(2));
    }

    /**
     * タスク担当ユーザ情報が存在しない場合、例外は発生せずにサイズ0の結果が取得できること。
     */
    @Test
    public void testFindTaskAssignedUserNotFound() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        dao.saveAssignedUser("0000000001", "001", Arrays.asList("0000000001"));
        dao.saveAssignedUser("0000000002", "001", Arrays.asList("0000000001"));
        dao.saveAssignedUser("0000000002", "003", Arrays.asList("0000000001"));
        dao.saveAssignedUser("0000000003", "001", Arrays.asList("0000000001"));
        workflowTestRule.commit();

        // ----- execute -----
        List<TaskAssignedUserEntity> actual = dao.findTaskAssignedUser("0000000002", "002");

        // ----- assert -----
        assertThat(actual.size(), is(0));
    }

    /**
     * タスク担当グループ情報が取得できること。
     */
    @Test
    public void testFindTaskAssignedGroup() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        dao.saveAssignedGroup("0000000001", "001", Arrays.asList("000000000a"));
        dao.saveAssignedGroup("0000000002", "001", Arrays.asList("000000000a"));
        dao.saveAssignedSequentialGroup("0000000002", "002", Arrays.asList("000000000c", "000000000b", "000000000a"));
        dao.saveAssignedGroup("0000000002", "003", Arrays.asList("000000000c"));
        dao.saveAssignedGroup("0000000003", "001", Arrays.asList("000000000a"));
        workflowTestRule.commit();

        // ----- execute -----
        List<TaskAssignedGroupEntity> actual = dao.findTaskAssignedGroup("0000000002", "002");

        // ----- assert -----
        assertThat(actual.size(), is(3));
        assertThat(actual.get(0).getInstanceId(), is("0000000002"));
        assertThat(actual.get(0).getFlowNodeId(), is("002"));
        assertThat(actual.get(0).getAssignedGroupId(), is("000000000c"));
        assertThat(actual.get(0).getExecutionOrder(), is(1));

        assertThat(actual.get(1).getInstanceId(), is("0000000002"));
        assertThat(actual.get(1).getFlowNodeId(), is("002"));
        assertThat(actual.get(1).getAssignedGroupId(), is("000000000b"));
        assertThat(actual.get(1).getExecutionOrder(), is(2));

        assertThat(actual.get(2).getInstanceId(), is("0000000002"));
        assertThat(actual.get(2).getFlowNodeId(), is("002"));
        assertThat(actual.get(2).getAssignedGroupId(), is("000000000a"));
        assertThat(actual.get(2).getExecutionOrder(), is(3));
    }

    /**
     * タスク担当グループが存在しない場合、例外は発生せずにサイズ0の結果が取得できること。
     */
    @Test
    public void testFindTaskAssignedGroupNotFound() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        dao.saveAssignedGroup("0000000001", "001", Arrays.asList("000000000a"));
        dao.saveAssignedGroup("0000000002", "111", Arrays.asList("000000000a"));
        dao.saveAssignedGroup("0000000002", "102", Arrays.asList("000000000b"));
        dao.saveAssignedGroup("0000000002", "113", Arrays.asList("000000000c"));
        dao.saveAssignedGroup("0000000003", "001", Arrays.asList("000000000a"));
        workflowTestRule.commit();

        // ----- execute -----
        List<TaskAssignedGroupEntity> actual = dao.findTaskAssignedGroup("0000000002", "112");

        // ----- assert -----
        assertThat(actual.size(), is(0));
    }

    /**
     * アクティブフローノード情報が取得できること
     */
    @Test
    public void testFindActiveFlowNode() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        dao.saveActiveFlowNode("0000000001", new Task("001", null, "001", "NONE", null,
                Collections.<SequenceFlow>emptyList()));
        dao.saveActiveFlowNode("0000000002", new Task("002", null, "002", "NONE", null,
                Collections.<SequenceFlow>emptyList()));
        dao.saveActiveFlowNode("0000000003", new Task("003", null, "001", "NONE", null,
                Collections.<SequenceFlow>emptyList()));
        workflowTestRule.commit();

        // ----- execute -----
        ActiveFlowNodeEntity actual = dao.findActiveFlowNode("0000000002");

        // ----- assert -----
        assertThat(actual, is(not(nullValue())));
        assertThat(actual.getInstanceId(), is("0000000002"));
        assertThat(actual.getFlowNodeId(), is("002"));
    }

    /**
     * アクティブフローノードが存在しない場合、例外が発生すること
     */
    @Test
    public void testFindActiveFlowNodeNotFound() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        dao.saveActiveFlowNode("1111111111", new Task("001", null, "001", "NONE", null,
                Collections.<SequenceFlow>emptyList()));
        dao.saveActiveFlowNode("1111111113", new Task("003", null, "001", "NONE", null,
                Collections.<SequenceFlow>emptyList()));
        workflowTestRule.commit();

        // ----- execute -----
        try {
            dao.findActiveFlowNode("1111111112");
            fail("ここはとおおらない");
        } catch (IllegalArgumentException e) {

            // ----- assert -----
            assertThat(e.getMessage(), is("active flow node was not found. instance id = [1111111112]"));
        }
    }

    /**
     * アクティブユーザタスク情報が取得できること
     */
    @Test
    public void testFindActiveUserTask() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        dao.saveActiveUserTask("9999999990", "__1", Arrays.asList("0000000001"));
        dao.saveActiveUserTask("9999999991", "__2", Arrays.asList("1000000001", "1000000002"));
        dao.saveActiveUserTask("9999999992", "__1", Arrays.asList("0000000001"));
        workflowTestRule.commit();

        // ----- execute -----
        List<ActiveUserTaskEntity> actual = dao.findActiveUserTask("9999999991");

        // ----- assert -----
        assertThat(actual.size(), is(2));

        assertThat(actual.get(0).getInstanceId(), is("9999999991"));
        assertThat(actual.get(0).getFlowNodeId(), is("__2"));
        assertThat(actual.get(0).getUserId(), is("1000000001"));
        assertThat(actual.get(0).getExecutionOrder(), is(0));

        assertThat(actual.get(1).getInstanceId(), is("9999999991"));
        assertThat(actual.get(1).getFlowNodeId(), is("__2"));
        assertThat(actual.get(1).getUserId(), is("1000000002"));
        assertThat(actual.get(1).getExecutionOrder(), is(0));
    }

    /**
     * アクティブユーザタスク情報が存在しない場合、サイズ0の結果が取得できること
     */
    @Test
    public void testFindActiveUserTaskNotFound() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        dao.saveActiveUserTask("9999999991", "__1", Arrays.asList("aaaaaaaaaa"));
        dao.saveActiveUserTask("9999999993", "__1", Arrays.asList("bbbbbbbbbb"));
        workflowTestRule.commit();

        // ----- execute -----
        List<ActiveUserTaskEntity> actual = dao.findActiveUserTask("9999999992");

        // ----- assert -----
        assertThat(actual.size(), is(0));
    }

    /**
     * ユーザID指定でアクティブユーザタスク情報が取得できること
     */
    @Test
    public void testFindActiveUserTaskByUserId() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        dao.saveActiveUserTask("9999999990", "__1",
                Arrays.asList("0000000001", "0000000002", "0000000003"));
        dao.saveActiveUserTask("9999999991", "__1",
                Arrays.asList("0000000001", "0000000002", "0000000003"));
        dao.saveActiveUserTask("9999999992", "__1",
                Arrays.asList("0000000001", "0000000002", "0000000003"));
        workflowTestRule.commit();

        // ----- execute -----
        ActiveUserTaskEntity userTaskEntity = dao.findActiveUserTaskByPk("0000000002", "__1", "9999999991");
        assertThat(userTaskEntity.getInstanceId(), is("9999999991"));
        assertThat(userTaskEntity.getUserId(), is("0000000002"));
        assertThat(userTaskEntity.getFlowNodeId(), is("__1"));
    }

    /**
     * ユーザID指定でアクティブユーザタスクを取得した場合でデータが存在しない場合
     */
    @Test
    public void testFindActiveUserTaskByUserIdNotFound() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        dao.saveActiveUserTask("9999999990", "__1",
                Arrays.asList("0000000001", "0000000002", "0000000003"));
        dao.saveActiveUserTask("9999999991", "__1",
                Arrays.asList("0000000001", "0000000003"));
        dao.saveActiveUserTask("9999999992", "__1",
                Arrays.asList("0000000001", "0000000002", "0000000003"));
        workflowTestRule.commit();

        // ----- execute -----
        ActiveUserTaskEntity userTaskEntity = dao.findActiveUserTaskByPk("9999999991", "__1", "0000000002");
        assertThat(userTaskEntity, is(nullValue()));
    }

    /**
     * アクティブグループタスク情報が取得できること
     */
    @Test
    public void testFindActiveGroupTask() {
        // ----- setup -----
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        dao.saveActiveGroupTask("9999999990", "__1", Arrays.asList("xxxxxxxxxx"));

        dao.saveActiveGroupTask("9999999991", "__2", Arrays.asList("zzzzzzzzzz", "aaaaaaaaaa"));
        dao.saveActiveGroupTask("9999999992", "__3", Arrays.asList("oooooooooo"));
        workflowTestRule.commit();

        // ----- execute -----
        List<ActiveGroupTaskEntity> actual = dao.findActiveGroupTask("9999999991");

        // ----- assert -----
        assertThat(actual.size(), is(2));
        assertThat(actual.get(0).getInstanceId(), is("9999999991"));
        assertThat(actual.get(0).getFlowNodeId(), is("__2"));
        assertThat(actual.get(0).getAssignedGroupId(), is("aaaaaaaaaa"));

        assertThat(actual.get(1).getInstanceId(), is("9999999991"));
        assertThat(actual.get(1).getFlowNodeId(), is("__2"));
        assertThat(actual.get(1).getAssignedGroupId(), is("zzzzzzzzzz"));
    }

    /**
     * アクティブグループタスクが存在しない場合、サイズ0の結果が取得できること。
     */
    @Test
    public void testFindActiveGroupTaskNotFound() {
        // ----- setup -----
        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        dao.saveActiveGroupTask("9999999993", "__2", Arrays.asList("zzzzzzzzzz"));
        dao.saveActiveGroupTask("9999999992", "__3", Arrays.asList("oooooooooo"));
        workflowTestRule.commit();

        // ----- execute -----
        List<ActiveGroupTaskEntity> actual = dao.findActiveGroupTask("9999999991");

        // ----- assert -----
        assertThat(actual.isEmpty(), is(true));
    }

    /**
     * グループID指定でアクティブグループタスク情報が取得できること
     */
    @Test
    public void testFindActiveGroupTaskByGroupId() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao sut = getWorkflowInstanceDao();
        sut.saveActiveGroupTask("9999999990", "__1", Arrays.asList("aaaaaaaaaa", "bbbbbbbbbb"));
        sut.saveActiveGroupTask("9999999991", "__1", Arrays.asList("aaaaaaaaaa", "bbbbbbbbbb"));
        sut.saveActiveGroupTask("9999999992", "__1", Arrays.asList("aaaaaaaaaa", "bbbbbbbbbb"));

        ActiveGroupTaskEntity actual = sut.findActiveGroupTaskByPk("9999999991", "__1", "bbbbbbbbbb");
        assertThat(actual.getInstanceId(), is("9999999991"));
        assertThat(actual.getFlowNodeId(), is("__1"));
        assertThat(actual.getAssignedGroupId(), is("bbbbbbbbbb"));
        assertThat(actual.getExecutionOrder(), is(0));
    }

    /**
     * グループID指定で検索の場合で、アクティブグループタスクが存在しない場合、nullが取得できること。
     */
    @Test
    public void testFindActiveGroupTaskByGroupIdNotFound() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao sut = getWorkflowInstanceDao();
        sut.saveActiveGroupTask("9999999990", "__1", Arrays.asList("aaaaaaaaaa", "bbbbbbbbbb"));
        sut.saveActiveGroupTask("9999999991", "__2", Arrays.asList("0000000001", "0000000003"));
        sut.saveActiveGroupTask("9999999992", "__1", Arrays.asList("aaaaaaaaaa", "bbbbbbbbbb"));

        ActiveGroupTaskEntity actual = sut.findActiveGroupTaskByPk("9999999991", "__2", "0000000002");
        assertThat(actual, is(nullValue()));
    }

    /**
     * アクティブユーザの件数が取得できること
     */
    @Test
    public void testGetActiveUserCount() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        workflowInstanceDao.saveActiveUserTask("0000000001", "001", Arrays.asList("9999999999", "8888888888"));
        workflowInstanceDao.saveActiveUserTask("0000000002", "002", Arrays.asList("9999999999", "8888888888",
                "7777777777"));
        workflowInstanceDao.saveActiveUserTask("0000000003", "003", "0000000000", 1);

        // ----- execute & assert -----
        assertThat(workflowInstanceDao.getActiveUserTaskCount("0000000001"), is(2));
        assertThat(workflowInstanceDao.getActiveUserTaskCount("0000000002"), is(3));
        assertThat(workflowInstanceDao.getActiveUserTaskCount("0000000003"), is(1));
        assertThat(workflowInstanceDao.getActiveUserTaskCount("0000000004"), is(0));
    }

    /**
     * 担当ユーザを指定してアクティブユーザタスクの件数が取得できること
     */
    @Test
    public void testGetActiveUserCountByPk() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        workflowInstanceDao.saveActiveUserTask("0000000001", "001", Arrays.asList("9999999999", "8888888888"));
        workflowInstanceDao.saveActiveUserTask("0000000002", "002", Arrays.asList("9999999999", "8888888888", "7777777777"));
        workflowInstanceDao.saveActiveUserTask("0000000003", "003", "0000000000", 1);

        // ----- execute & assert -----
        assertThat(workflowInstanceDao.getActiveUserTaskCountByPk("0000000001", "001", "9999999999"), is(1));
        assertThat(workflowInstanceDao.getActiveUserTaskCountByPk("0000000004", "001", "9999999999"), is(0));
        assertThat(workflowInstanceDao.getActiveUserTaskCountByPk("0000000001", "004", "9999999999"), is(0));
        assertThat(workflowInstanceDao.getActiveUserTaskCountByPk("0000000001", "001", "0000000000"), is(0));
    }

    /**
     * アサインユーザの件数が取得できること
     */
    @Test
    public void testGetTaskAssignedUserCount() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        workflowInstanceDao.saveAssignedUser("0000000001", "001", Arrays.asList("9999999999", "8888888888"));
        workflowInstanceDao.saveAssignedUser("0000000001", "002", Arrays.asList("9999999999", "8888888888",
                "7777777777"));
        workflowInstanceDao.saveAssignedUser("0000000002", "002", Arrays.asList("9999999999"));
        workflowInstanceDao.saveAssignedSequentialUser("0000000003", "003", Arrays.asList("0000000000"));

        // ----- execute & assert -----
        assertThat(workflowInstanceDao.getTaskAssignedUserCount("0000000001", "001"), is(2));
        assertThat(workflowInstanceDao.getTaskAssignedUserCount("0000000001", "002"), is(3));
        assertThat(workflowInstanceDao.getTaskAssignedUserCount("0000000002", "002"), is(1));
        assertThat(workflowInstanceDao.getTaskAssignedUserCount("0000000003", "003"), is(1));
    }

    /**
     * アクティブグループの件数が取得できること。
     */
    @Test
    public void testGetActiveGroupCount() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao sut = getWorkflowInstanceDao();
        sut.saveActiveGroupTask("0000000001", "002", Arrays.asList("0000000001"));
        sut.saveActiveGroupTask("0000000002", "002", Arrays.asList("0000000001", "0000000002"));
        sut.saveActiveGroupTask("0000000003", "002", Arrays.asList("0000000001"));
        workflowTestRule.commit();

        // ----- execute & assert -----
        assertThat(sut.getActiveGroupTaskCount("0000000001"), is(1));
        assertThat(sut.getActiveGroupTaskCount("0000000002"), is(2));
        assertThat(sut.getActiveGroupTaskCount("0000000003"), is(1));

    }

    /**
     * 担当グループを指定してアクティブグループタスクの件数が取得できること
     */
    @Test
    public void testGetActiveGroupCountByPk() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao workflowInstanceDao = getWorkflowInstanceDao();
        workflowInstanceDao.saveActiveGroupTask("0000000001", "001", Arrays.asList("9999999999", "8888888888"));
        workflowInstanceDao.saveActiveGroupTask("0000000002", "002", Arrays.asList("9999999999", "8888888888", "7777777777"));
        workflowInstanceDao.saveActiveGroupTask("0000000003", "003", "0000000000", 1);

        // ----- execute & assert -----
        assertThat(workflowInstanceDao.getActiveGroupTaskCountByPk("0000000001", "001", "9999999999"), is(1));
        assertThat(workflowInstanceDao.getActiveGroupTaskCountByPk("0000000004", "001", "9999999999"), is(0));
        assertThat(workflowInstanceDao.getActiveGroupTaskCountByPk("0000000001", "004", "9999999999"), is(0));
        assertThat(workflowInstanceDao.getActiveGroupTaskCountByPk("0000000001", "001", "0000000000"), is(0));
    }

    /**
     * アサイングループの件数が取得できること
     */
    @Test
    public void testGetTaskAssignedGroupCount() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao sut = getWorkflowInstanceDao();
        sut.saveAssignedGroup("0000000001", "001", Arrays.asList("0000000001"));
        sut.saveAssignedGroup("0000000001", "002", Arrays.asList("0000000002", "0000000003"));
        sut.saveAssignedGroup("0000000002", "001", Arrays.asList("0000000003", "0000000001"));
        workflowTestRule.commit();

        // ----- execute & assert -----
        assertThat(sut.getTaskAssignedGroupCount("0000000001", "001"), is(1));
        assertThat(sut.getTaskAssignedGroupCount("0000000001", "002"), is(2));
        assertThat(sut.getTaskAssignedGroupCount("0000000002", "001"), is(2));
    }

    /**
     * アサインユーザの振替ができること。
     */
    @Test
    public void testChangeAssignedUser() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao sut = getWorkflowInstanceDao();
        sut.saveAssignedUser("9999999991", "__1", Arrays.asList("0000000001"));
        sut.saveAssignedSequentialUser("9999999991", "__2", Arrays.asList("0000000001", "0000000002", "0000000003"));
        sut.saveAssignedUser("9999999991", "__3", Arrays.asList("0000000001"));
        sut.saveAssignedUser("9999999990", "__2", Arrays.asList("0000000001"));
        sut.saveAssignedUser("9999999992", "__2", Arrays.asList("0000000001"));
        workflowTestRule.commit();

        // ----- execute -----
        sut.changeAssignedUser("9999999991", "__2", "0000000002", "aaaaaaaaaa");
        workflowTestRule.commit();

        // ----- assert -----
        SqlResultSet assignedUser = workflowDbAccessSupport.findAssignedUser();

        assertThat(assignedUser.size(), is(7));

        // 変更対象
        assertThat("インスタンスIDはそのまま", assignedUser.get(4).getString("instance_id"), is("9999999991"));
        assertThat("タスクIDはそのまま", assignedUser.get(4).getString("flow_node_id"), is("__2"));
        assertThat("ユーザが切り替わっていること", assignedUser.get(4).getString("assigned_user_id"), is("aaaaaaaaaa"));
        assertThat("実行順はそのまま", assignedUser.get(4).getBigDecimal("execution_order").intValue(), is(2));
        assertThat(assignedUser.get(0).getString("instance_id"), is("9999999990"));

        // 変更対象外
        assertThat(assignedUser.get(1).getString("instance_id"), is("9999999991"));
        assertThat(assignedUser.get(1).getString("flow_node_id"), is("__1"));

        assertThat(assignedUser.get(2).getString("instance_id"), is("9999999991"));
        assertThat(assignedUser.get(2).getString("flow_node_id"), is("__2"));
        assertThat(assignedUser.get(2).getString("assigned_user_id"), is("0000000001"));
        assertThat(assignedUser.get(2).getBigDecimal("execution_order").intValue(), is(1));

        assertThat(assignedUser.get(3).getString("instance_id"), is("9999999991"));
        assertThat(assignedUser.get(3).getString("flow_node_id"), is("__2"));
        assertThat(assignedUser.get(3).getString("assigned_user_id"), is("0000000003"));
        assertThat(assignedUser.get(3).getBigDecimal("execution_order").intValue(), is(3));

        assertThat(assignedUser.get(5).getString("instance_id"), is("9999999991"));
        assertThat(assignedUser.get(5).getString("flow_node_id"), is("__3"));

        assertThat(assignedUser.get(6).getString("instance_id"), is("9999999992"));
    }

    /**
     * タスク担当ユーザが存在しない場合、データが更新されないこと
     */
    @Test
    public void testChangeAssignedUserNotFound() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao sut = getWorkflowInstanceDao();
        sut.saveAssignedUser("9999999991", "__1", Arrays.asList("0000000001"));
        sut.saveAssignedUser("9999999991", "__3", Arrays.asList("0000000001"));
        workflowTestRule.commit();

        // ----- execute -----
        sut.changeAssignedUser("9999999991", "__2", "0000000002", "aaaaaaaaaa");
        workflowTestRule.commit();

        // ----- assert -----
        SqlResultSet assignedUser = workflowDbAccessSupport.findAssignedUser();

        assertThat(assignedUser.size(), is(2));
        assertThat(assignedUser.get(0).getString("instance_id"), is("9999999991"));
        assertThat(assignedUser.get(0).getString("flow_node_id"), is("__1"));
        assertThat(assignedUser.get(1).getString("instance_id"), is("9999999991"));
        assertThat(assignedUser.get(1).getString("flow_node_id"), is("__3"));
    }

    /**
     * アクティブユーザタスクを別のユーザに振替できること
     */
    @Test
    public void testChangeActiveUser() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao sut = getWorkflowInstanceDao();
        sut.saveActiveUserTask("9999999991", "__2", Arrays.asList("0000000001"));
        sut.saveActiveUserTask("9999999992", "__2", "0000000001", 2);
        sut.saveActiveUserTask("9999999993", "__2", Arrays.asList("0000000001"));
        workflowTestRule.commit();

        // ----- execute -----
        sut.changeActiveUser("9999999992", "__2", "0000000001", "bbbbbbbbbb");
        workflowTestRule.commit();

        // ----- assert -----
        SqlResultSet activeUserTask = workflowDbAccessSupport.findActiveUserTask();

        assertThat(activeUserTask.size(), is(3));
        // 変更対象
        assertThat(activeUserTask.get(1).getString("instance_id"), is("9999999992"));
        assertThat(activeUserTask.get(1).getString("flow_node_id"), is("__2"));
        assertThat(activeUserTask.get(1).getString("assigned_user_id"), is("bbbbbbbbbb"));
        assertThat(activeUserTask.get(1).getBigDecimal("execution_order").intValue(), is(2));

        // 変更対象外
        assertThat(activeUserTask.get(0).getString("instance_id"), is("9999999991"));
        assertThat(activeUserTask.get(0).getString("flow_node_id"), is("__2"));
        assertThat(activeUserTask.get(0).getString("assigned_user_id"), is("0000000001"));
        assertThat(activeUserTask.get(0).getBigDecimal("execution_order").intValue(), is(0));

        assertThat(activeUserTask.get(2).getString("instance_id"), is("9999999993"));
        assertThat(activeUserTask.get(2).getString("flow_node_id"), is("__2"));
        assertThat(activeUserTask.get(2).getString("assigned_user_id"), is("0000000001"));
        assertThat(activeUserTask.get(2).getBigDecimal("execution_order").intValue(), is(0));
    }

    /**
     * アクティブユーザタスクが存在しない場合、テーブルの状態は変更されないこと
     */
    @Test
    public void testChangeActiveUserNotFound() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao sut = getWorkflowInstanceDao();
        sut.saveActiveUserTask("9999999991", "__2", Arrays.asList("0000000001"));
        sut.saveActiveUserTask("9999999992", "__2", Arrays.asList("0000000001", "0000000003"));
        sut.saveActiveUserTask("9999999993", "__2", Arrays.asList("0000000001"));
        workflowTestRule.commit();

        // ----- execute -----
        sut.changeActiveUser("9999999992", "__2", "0000000002", "bbbbbbbbbb");
        workflowTestRule.commit();

        // ----- assert -----
        SqlResultSet activeUserTask = workflowDbAccessSupport.findActiveUserTask();

        assertThat(activeUserTask.size(), is(4));
        assertThat(activeUserTask.get(0).getString("instance_id"), is("9999999991"));
        assertThat(activeUserTask.get(0).getString("flow_node_id"), is("__2"));
        assertThat(activeUserTask.get(0).getString("assigned_user_id"), is("0000000001"));
        assertThat(activeUserTask.get(0).getBigDecimal("execution_order").intValue(), is(0));

        assertThat(activeUserTask.get(1).getString("instance_id"), is("9999999992"));
        assertThat(activeUserTask.get(1).getString("flow_node_id"), is("__2"));
        assertThat(activeUserTask.get(1).getString("assigned_user_id"), is("0000000001"));
        assertThat(activeUserTask.get(1).getBigDecimal("execution_order").intValue(), is(0));

        assertThat(activeUserTask.get(2).getString("instance_id"), is("9999999992"));
        assertThat(activeUserTask.get(2).getString("flow_node_id"), is("__2"));
        assertThat(activeUserTask.get(2).getString("assigned_user_id"), is("0000000003"));
        assertThat(activeUserTask.get(2).getBigDecimal("execution_order").intValue(), is(0));

        assertThat(activeUserTask.get(3).getString("instance_id"), is("9999999993"));
        assertThat(activeUserTask.get(3).getString("flow_node_id"), is("__2"));
        assertThat(activeUserTask.get(3).getString("assigned_user_id"), is("0000000001"));
        assertThat(activeUserTask.get(3).getBigDecimal("execution_order").intValue(), is(0));
    }

    /**
     * グループの振替ができること
     */
    @Test
    public void testChangeAssignedGroup() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao sut = getWorkflowInstanceDao();
        sut.saveAssignedGroup("9999999991", "__1", Arrays.asList("0000000001"));
        sut.saveAssignedSequentialGroup("9999999991", "__2", Arrays.asList("0000000001", "0000000002", "0000000003"));
        sut.saveAssignedGroup("9999999991", "__3", Arrays.asList("0000000001"));
        sut.saveAssignedGroup("9999999990", "__2", Arrays.asList("0000000001"));
        sut.saveAssignedGroup("9999999992", "__2", Arrays.asList("0000000001"));

        workflowTestRule.commit();

        // ----- execute -----
        sut.changeAssignedGroup("9999999991", "__2", "0000000002", "aaaaaaaaaa");
        workflowTestRule.commit();

        // ----- assert -----
        SqlResultSet assignedGroup = workflowDbAccessSupport.findAssignedGroup();

        assertThat(assignedGroup.size(), is(7));

        // 変更対象
        assertThat("インスタンスIDはそのまま", assignedGroup.get(4).getString("instance_id"), is("9999999991"));
        assertThat("タスクIDはそのまま", assignedGroup.get(4).getString("flow_node_id"), is("__2"));
        assertThat("グループが切り替わっていること", assignedGroup.get(4).getString("assigned_group_id"), is("aaaaaaaaaa"));
        assertThat("実行順はそのまま", assignedGroup.get(4).getBigDecimal("execution_order").intValue(), is(2));
        assertThat(assignedGroup.get(0).getString("instance_id"), is("9999999990"));

        // 変更対象外
        assertThat(assignedGroup.get(1).getString("instance_id"), is("9999999991"));
        assertThat(assignedGroup.get(1).getString("flow_node_id"), is("__1"));

        assertThat(assignedGroup.get(2).getString("instance_id"), is("9999999991"));
        assertThat(assignedGroup.get(2).getString("flow_node_id"), is("__2"));
        assertThat(assignedGroup.get(2).getString("assigned_group_id"), is("0000000001"));
        assertThat(assignedGroup.get(2).getBigDecimal("execution_order").intValue(), is(1));

        assertThat(assignedGroup.get(3).getString("instance_id"), is("9999999991"));
        assertThat(assignedGroup.get(3).getString("flow_node_id"), is("__2"));
        assertThat(assignedGroup.get(3).getString("assigned_group_id"), is("0000000003"));
        assertThat(assignedGroup.get(3).getBigDecimal("execution_order").intValue(), is(3));

        assertThat(assignedGroup.get(5).getString("instance_id"), is("9999999991"));
        assertThat(assignedGroup.get(5).getString("flow_node_id"), is("__3"));

        assertThat(assignedGroup.get(6).getString("instance_id"), is("9999999992"));
    }

    /**
     * タスク担当グループが存在しない場合、データが更新されないこと
     */
    @Test
    public void testChangeAssignedNotFound() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao sut = getWorkflowInstanceDao();
        sut.saveAssignedGroup("9999999991", "__1", Arrays.asList("0000000001"));
        sut.saveAssignedGroup("9999999991", "__3", Arrays.asList("0000000001"));
        workflowTestRule.commit();

        // ----- execute -----
        sut.changeAssignedGroup("9999999991", "__2", "0000000002", "aaaaaaaaaa");
        workflowTestRule.commit();

        // ----- assert -----
        SqlResultSet assignedGroup = workflowDbAccessSupport.findAssignedGroup();

        assertThat(assignedGroup.size(), is(2));
        assertThat(assignedGroup.get(0).getString("instance_id"), is("9999999991"));
        assertThat(assignedGroup.get(0).getString("flow_node_id"), is("__1"));
        assertThat(assignedGroup.get(1).getString("instance_id"), is("9999999991"));
        assertThat(assignedGroup.get(1).getString("flow_node_id"), is("__3"));

    }

    /**
     * アクティブグループタスクのグループ情報を別のグループに変更できること。
     */
    @Test
    public void testChangeActiveGroup() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao sut = getWorkflowInstanceDao();
        sut.saveActiveGroupTask("9999999991", "__1", Arrays.asList("0000000001"));
        sut.saveActiveGroupTask("9999999992", "__1", "0000000002", 99);
        sut.saveActiveGroupTask("9999999993", "__1", Arrays.asList("0000000001"));
        workflowTestRule.commit();

        // ----- execute -----
        sut.changeActiveGroup("9999999992", "__1", "0000000002", "aaaaaaaaaa");
        workflowTestRule.commit();

        // ----- assert -----
        SqlResultSet activeGroupTask = workflowDbAccessSupport.findActiveGroupTask();

        assertThat(activeGroupTask.size(), is(3));
        // 変更対象
        assertThat(activeGroupTask.get(1).getString("instance_id"), is("9999999992"));
        assertThat(activeGroupTask.get(1).getString("flow_node_id"), is("__1"));
        assertThat(activeGroupTask.get(1).getString("assigned_group_id"), is("aaaaaaaaaa"));
        assertThat(activeGroupTask.get(1).getBigDecimal("execution_order").intValue(), is(99));

        // 変更対象外
        assertThat(activeGroupTask.get(0).getString("instance_id"), is("9999999991"));
        assertThat(activeGroupTask.get(2).getString("instance_id"), is("9999999993"));
    }

    /**
     * アクティブグループタスクが存在しない場合、テーブルの状態は変わらないこと。
     *
     * @throws Exception
     */
    @Test
    public void testChangeActiveGroupNotFound() throws Exception {
        // ----- setup -----
        WorkflowInstanceDao sut = getWorkflowInstanceDao();
        sut.saveActiveGroupTask("9999999991", "__2", Arrays.asList("0000000002"));
        sut.saveActiveGroupTask("9999999992", "__2", Arrays.asList("0000000001", "0000000003"));
        sut.saveActiveGroupTask("9999999993", "__2", Arrays.asList("0000000002"));
        workflowTestRule.commit();

        // ----- execute -----
        sut.changeActiveGroup("9999999992", "__2", "0000000002", "aaaaaaaaaa");
        workflowTestRule.commit();

        // ----- assert -----
        SqlResultSet activeGroupTask = workflowDbAccessSupport.findActiveGroupTask();
        assertThat(activeGroupTask.size(), is(4));

        assertThat(activeGroupTask.get(0).getString("instance_id"), is("9999999991"));
        assertThat(activeGroupTask.get(0).getString("flow_node_id"), is("__2"));
        assertThat(activeGroupTask.get(0).getString("assigned_group_id"), is("0000000002"));
        assertThat(activeGroupTask.get(0).getBigDecimal("execution_order").intValue(), is(0));

        assertThat(activeGroupTask.get(1).getString("instance_id"), is("9999999992"));
        assertThat(activeGroupTask.get(1).getString("flow_node_id"), is("__2"));
        assertThat(activeGroupTask.get(1).getString("assigned_group_id"), is("0000000001"));
        assertThat(activeGroupTask.get(1).getBigDecimal("execution_order").intValue(), is(0));

        assertThat(activeGroupTask.get(2).getString("instance_id"), is("9999999992"));
        assertThat(activeGroupTask.get(2).getString("flow_node_id"), is("__2"));
        assertThat(activeGroupTask.get(2).getString("assigned_group_id"), is("0000000003"));
        assertThat(activeGroupTask.get(2).getBigDecimal("execution_order").intValue(), is(0));

        assertThat(activeGroupTask.get(3).getString("instance_id"), is("9999999993"));
        assertThat(activeGroupTask.get(3).getString("flow_node_id"), is("__2"));
        assertThat(activeGroupTask.get(3).getString("assigned_group_id"), is("0000000002"));
        assertThat(activeGroupTask.get(3).getBigDecimal("execution_order").intValue(), is(0));
    }

    /**
     * テスト対象のインスタンスを取得する。
     *
     * @return テスト対象オブジェクト
     */
    private WorkflowInstanceDao getWorkflowInstanceDao() {
        return SystemRepository.get("workflowInstanceDao");
    }

    private String createInstanceData() {
        WorkflowInstanceDao dao = getWorkflowInstanceDao();

        // データその１
        Task flow1Task = new Task("__1", "タスク1", null, "NONE", null, Collections.<SequenceFlow>emptyList());
        String instance1 = dao.createWorkflowInstance("00001", 1, Arrays.asList(flow1Task));
        dao.saveAssignedUser(instance1, "__1", Arrays.asList("zzzzzzzzzz"));
        dao.saveAssignedGroup(instance1, "__2", Arrays.asList("xxxxxxxxxx"));
        dao.saveActiveFlowNode(instance1, flow1Task);
        dao.saveActiveUserTask(instance1, "__1", Arrays.asList("zzzzzzzzzz"));
        dao.saveActiveGroupTask(instance1, "__1", Arrays.asList("xxxxxxxxxx"));

        // データその２
        Task flow2Task = new Task("__1", "タスク1", null, "NONE", null, Collections.<SequenceFlow>emptyList());
        String instance2 = dao.createWorkflowInstance("00003", 1, Arrays.asList(flow2Task));
        dao.saveAssignedUser(instance2, "__1", Arrays.asList("zzzzzzzzzz"));
        dao.saveAssignedGroup(instance2, "__2", Arrays.asList("xxxxxxxxxx"));
        dao.saveActiveFlowNode(instance2, flow2Task);
        dao.saveActiveUserTask(instance2, "__1", Arrays.asList("zzzzzzzzzz"));
        dao.saveActiveGroupTask(instance2, "__1", Arrays.asList("xxxxxxxxxx"));

        // データその３
        String instance3 = dao.createWorkflowInstance("00002", 1, Arrays.asList(
                new Task("__1", "タスク1", null, "NONE", null, Collections.<SequenceFlow>emptyList()),
                new Task("__2", "タスク2", null, "NONE", null, Collections.<SequenceFlow>emptyList())));
        dao.saveAssignedUser(instance3, "__1", Arrays.asList(
                "aaaaaaaaaa", "bbbbbbbbbb"));
        dao.saveAssignedGroup(instance3, "__2", Arrays.asList("pppppppppp"));
        dao.saveActiveFlowNode(instance3, new Task("__2", "タスク2", null, "NONE", null,
                Collections.<SequenceFlow>emptyList()));
        dao.saveActiveUserTask(instance3, "__1", Arrays.asList("aaaaaaaaaa"));
        dao.saveActiveGroupTask(instance3, "__2", Arrays.asList("pppppppppp"));

        return instance3;
    }
}

