package nablarch.integration.workflow.definition;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import nablarch.integration.workflow.WorkflowConfig;
import nablarch.integration.workflow.dao.ActiveGroupTaskEntity;
import nablarch.integration.workflow.dao.ActiveUserTaskEntity;
import nablarch.integration.workflow.dao.TaskAssignedGroupEntity;
import nablarch.integration.workflow.dao.TaskAssignedUserEntity;
import nablarch.integration.workflow.testhelper.WorkflowDbAccessSupport;
import nablarch.integration.workflow.testhelper.WorkflowTestRule;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import nablarch.integration.workflow.condition.AllCompletionCondition;
import nablarch.integration.workflow.condition.OrCompletionCondition;
import nablarch.integration.workflow.dao.ActiveFlowNodeEntity;
import nablarch.integration.workflow.dao.WorkflowInstanceDao;

public class TaskTest {

    @ClassRule
    public static WorkflowTestRule rule = new WorkflowTestRule(false);
    private static WorkflowDbAccessSupport db;

    private static final String INSTANCE_ID = "instanceId";
    private static final String TASK_ID = "t01";

    @BeforeClass
    public static void beforeClass() {
        db = rule.getWorkflowDao();
    }

    @Before
    public void before() {
        db.cleanup(
                "WF_ACTIVE_USER_TASK",
                "WF_ACTIVE_GROUP_TASK",
                "WF_ACTIVE_FLOW_NODE",
                "WF_TASK_ASSIGNED_USER",
                "WF_TASK_ASSIGNED_GROUP",
                "WF_INSTANCE_FLOW_NODE",
                "WF_INSTANCE"
        );
    }

    /**
     * タスクに担当ユーザを割り当てる場合の、 {@link Task#assignUsers(String, List)} のテスト。
     */
    @Test
    public void testAssignUsersToSingleTask() throws Exception {
        Task sut = createTask(TASK_ID, Task.MultiInstanceType.NONE, null);
        List<String> assignee = Arrays.asList("u000000001");

        sut.assignUsers(INSTANCE_ID, assignee);
        rule.commit();

        List<TaskAssignedUserEntity> assigned = getWorkflowInstanceDao().findTaskAssignedUser(INSTANCE_ID, TASK_ID);

        assertThat("担当ユーザがアサインされていること。", assigned.get(0).getUserId(), is("u000000001"));
        assertThat("今回アサインされたユーザだけが登録されていること。", assigned.size(), is(1));
    }

    /**
     * シングルタスクに複数の担当ユーザを割り当てる場合の {@link Task#assignUsers(String, List)} のテスト。
     */
    @Test
    public void testAssignUsersToSingleTask_MultipleUsers() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.NONE, null);
        List<String> users = Arrays.asList("u000000001", "u000000002");
        try {
            task.assignUsers(INSTANCE_ID, users);
            fail("タスクに複数人のユーザを割り当てようとした場合は、例外が発生しなくてはいけない。");
        } catch (IllegalArgumentException e) {
            String actual = e.getMessage();
            assertThat(actual, containsString("Multiple users cannot be assigned to NOT Multi-Instance Tasks."));
            assertThat(actual, containsString("instance id = [" + INSTANCE_ID + "]"));
            assertThat(actual, containsString("task id = [" + TASK_ID + "]"));
            assertThat(actual, containsString("users = [" + users + "]"));
        }
    }

    /**
     * 並行タスクに担当ユーザを割り当てる場合の {@link Task#assignUsers(String, List)} のテスト。
     */
    @Test
    public void testAssignUsersToParallelTask() throws Exception {
        Task sut = createTask(TASK_ID, Task.MultiInstanceType.PARALLEL, null);
        List<String> assignee = Arrays.asList("u000000001", "u000000002");

        sut.assignUsers(INSTANCE_ID, assignee);
        rule.commit();

        List<TaskAssignedUserEntity> assigned = getWorkflowInstanceDao().findTaskAssignedUser(INSTANCE_ID, TASK_ID);
        // 実行順でしかソートされていないので、アサート用に担当ユーザでソートしておく。
        Collections.sort(assigned, new Comparator<TaskAssignedUserEntity>() {
            @Override
            public int compare(TaskAssignedUserEntity o1, TaskAssignedUserEntity o2) {
                return o1.getUserId().compareTo(o2.getUserId());
            }
        });

        assertThat("担当ユーザが登録されていること。（一人目）", assigned.get(0).getUserId(), is("u000000001"));
        assertThat("並行タスクの場合、全担当ユーザの実行順が0となること。（一人目）", assigned.get(0).getExecutionOrder(), is(0));
        assertThat("担当ユーザが登録されていること。（二人目）", assigned.get(1).getUserId(), is("u000000002"));
        assertThat("並行タスクの場合、全担当ユーザの実行順が0となること。（二人目）", assigned.get(1).getExecutionOrder(), is(0));
        assertThat("今回アサインされたユーザだけが登録されていること。", assigned.size(), is(2));
    }

    /**
     * 順次タスクに担当ユーザを割り当てる場合の {@link Task#assignUsers(String, List)} のテスト。
     */
    @Test
    public void testAssignUsersToSequentialTask() throws Exception {
        Task sut = createTask(TASK_ID, Task.MultiInstanceType.SEQUENTIAL, null);
        List<String> assignee = Arrays.asList("u000000003", "u000000002", "u000000001");

        sut.assignUsers(INSTANCE_ID, assignee);
        rule.commit();

        List<TaskAssignedUserEntity> assigned = getWorkflowInstanceDao().findTaskAssignedUser(INSTANCE_ID, TASK_ID);
        assertThat("担当ユーザが登録されていること。（一人目）", assigned.get(0).getUserId(), is("u000000003"));
        assertThat("順次タスクの場合、担当ユーザの実行順はassignUsersの引数の順になっていること。（一人目）", assigned.get(0).getExecutionOrder(), is(1));
        assertThat("担当ユーザが登録されていること。（二人目）", assigned.get(1).getUserId(), is("u000000002"));
        assertThat("順次タスクの場合、担当ユーザの実行順はassignUsersの引数の順になっていること。（二人目）", assigned.get(1).getExecutionOrder(), is(2));
        assertThat("担当ユーザが登録されていること。（三人目）", assigned.get(2).getUserId(), is("u000000001"));
        assertThat("順次タスクの場合、担当ユーザの実行順はassignUsersの引数の順になっていること。（三人目）", assigned.get(2).getExecutionOrder(), is(3));
        assertThat("今回アサインされたユーザだけが登録されていること。", assigned.size(), is(3));
    }

    /**
     * タスクに担当ユーザが割り当てられている場合の {@link Task#refreshActiveUserTasks(String, List)} のテスト。
     */
    @Test
    public void testRefreshActiveUserTasksForTask() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.NONE, null);
        task.refreshActiveUserTasks(INSTANCE_ID, Arrays.asList("u000000001"));
        rule.commit();

        List<ActiveUserTaskEntity> activeUserTask = getWorkflowInstanceDao().findActiveUserTask(INSTANCE_ID);
        assertThat("登録されていた担当ユーザのアクティブユーザタスクが作成されること。", activeUserTask.get(0).getUserId(), is("u000000001"));
        assertThat("登録されていた担当ユーザの分だけのアクティブユーザタスクしか作成されないこと。", activeUserTask.size(), is(1));
    }

    /**
     * 並列タスクに担当ユーザが割り当てられている場合の {@link Task#refreshActiveUserTasks(String, List)} のテスト。
     */
    @Test
    public void testRefreshActiveUserTasksForParallelTask() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.PARALLEL, null);
        task.refreshActiveUserTasks(INSTANCE_ID, Arrays.asList("u000000001", "u000000002"));
        rule.commit();

        List<ActiveUserTaskEntity> activeUserTask = getWorkflowInstanceDao().findActiveUserTask(INSTANCE_ID);
        // 実行順でしかソートされていないので、アサート用に担当ユーザでソートしておく。
        Collections.sort(activeUserTask, new Comparator<TaskAssignedUserEntity>() {
            @Override
            public int compare(TaskAssignedUserEntity o1, TaskAssignedUserEntity o2) {
                return o1.getUserId().compareTo(o2.getUserId());
            }
        });

        assertThat("並行タスクの場合、登録されていた全担当ユーザのアクティブユーザタスクが作成されること。（一人目）",
                activeUserTask.get(0).getUserId(), is("u000000001"));
        assertThat("並行タスクの場合、登録されていた全担当ユーザのアクティブユーザタスクが作成されること。（二人目）",
                activeUserTask.get(1).getUserId(), is("u000000002"));
        assertThat("登録されていた担当ユーザの分だけのアクティブユーザタスクしか作成されないこと。", activeUserTask.size(), is(2));
    }

    /**
     * 順次タスクに担当ユーザが割り当てられている場合の、 {@link Task#refreshActiveUserTasks(String, List)} のテスト。
     */
    @Test
    public void testRefreshActiveUserTasksForSequentialTask() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.SEQUENTIAL, null);
        task.refreshActiveUserTasks(INSTANCE_ID, Arrays.asList("u000000003", "u000000002", "u000000001"));
        rule.commit();

        List<ActiveUserTaskEntity> activeUserTask = getWorkflowInstanceDao().findActiveUserTask(INSTANCE_ID);
        assertThat("並行タスクの場合、登録されていた担当ユーザのうち、実行順が先頭のアクティブユーザタスクが作成されること。",
                activeUserTask.get(0).getUserId(), is("u000000003"));
        assertThat("登録されていた担当ユーザのうち、実行順が先頭のアクティブユーザタスクしか作成されないこと。", activeUserTask.size(), is(1));
    }

    /**
     * 並行タスクに担当ユーザがアサインされていない場合の {@link Task#refreshActiveUserTasks(String, List)} のテスト。
     */
    @Test
    public void testRefreshActiveUserTasksForParallelTask_NotAssigned() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.PARALLEL, null);
        task.refreshActiveUserTasks(INSTANCE_ID, Collections.<String>emptyList());
        rule.commit();

        List<ActiveUserTaskEntity> activeUserTask = getWorkflowInstanceDao().findActiveUserTask(INSTANCE_ID);
        assertThat("担当ユーザがアサインされていない場合には、アクティブユーザタスクが空になっていること。", activeUserTask.size(), is(0));
    }

    /**
     * 順次タスクに担当ユーザがアサインされていない場合の {@link Task#refreshActiveUserTasks(String, List)} のテスト。
     */
    @Test
    public void testRefreshActiveUserTasksForSequentialTask_NotAssigned() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.SEQUENTIAL, null);
        task.refreshActiveUserTasks(INSTANCE_ID, Collections.<String>emptyList());
        rule.commit();

        List<ActiveUserTaskEntity> activeUserTask = getWorkflowInstanceDao().findActiveUserTask(INSTANCE_ID);
        assertThat("担当ユーザがアサインされていない場合には、アクティブユーザタスクが空になっていること。", activeUserTask.size(), is(0));
    }

    /**
     * タスクに担当グループを割り当てる場合の、 {@link Task#assignGroups(String, List)} のテスト。
     */
    @Test
    public void testAssignGroupsToSingleTask() throws Exception {
        Task sut = createTask(TASK_ID, Task.MultiInstanceType.NONE, null);
        List<String> groups = Arrays.asList("g000000001");

        sut.assignGroups(INSTANCE_ID, groups);
        rule.commit();

        List<TaskAssignedGroupEntity> assigned = getWorkflowInstanceDao().findTaskAssignedGroup(INSTANCE_ID, TASK_ID);

        assertThat("担当ユーザがアサインされていること。", assigned.get(0).getAssignedGroupId(), is("g000000001"));
        assertThat("今回アサインされたユーザだけが登録されていること。", assigned.size(), is(1));
    }

    /**
     * シングルタスクに複数の担当グループを割り当てる場合の {@link Task#assignGroups(String, List)} のテスト。
     */
    @Test
    public void testAssignGroupsToSingleTask_MultipleGroups() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.NONE, null);
        List<String> groups = Arrays.asList("g000000001", "g000000002");
        try {
            task.assignGroups(INSTANCE_ID, groups);
            fail("タスクに複数のグループを割り当てようとした場合は、例外が発生しなくてはいけない。");
        } catch (IllegalArgumentException e) {
            String actual = e.getMessage();
            assertThat(actual, containsString("Multiple groups cannot be assigned to NOT Multi-Instance Tasks."));
            assertThat(actual, containsString("instance id = [" + INSTANCE_ID + "]"));
            assertThat(actual, containsString("task id = [" + TASK_ID + "]"));
            assertThat(actual, containsString("groups = [" + groups + "]"));
        }
    }

    /**
     * 並行タスクに担当グループを割り当てる場合の {@link Task#assignGroups(String, List)} のテスト。
     */
    @Test
    public void testAssignGroupsToParallelTask() throws Exception {
        Task sut = createTask(TASK_ID, Task.MultiInstanceType.PARALLEL, null);
        List<String> assignee = Arrays.asList("g000000001", "g000000002");

        sut.assignGroups(INSTANCE_ID, assignee);
        rule.commit();

        List<TaskAssignedGroupEntity> assigned = getWorkflowInstanceDao().findTaskAssignedGroup(INSTANCE_ID, TASK_ID);
        // 実行順でしかソートされていないので、アサート用に担当ユーザでソートしておく。
        Collections.sort(assigned, new Comparator<TaskAssignedGroupEntity>() {
            @Override
            public int compare(TaskAssignedGroupEntity o1, TaskAssignedGroupEntity o2) {
                return o1.getAssignedGroupId().compareTo(o2.getAssignedGroupId());
            }
        });

        assertThat("担当ユーザが登録されていること。（一人目）", assigned.get(0).getAssignedGroupId(), is("g000000001"));
        assertThat("並行タスクの場合、全担当ユーザの実行順が0となること。（一人目）", assigned.get(0).getExecutionOrder(), is(0));
        assertThat("担当ユーザが登録されていること。（二人目）", assigned.get(1).getAssignedGroupId(), is("g000000002"));
        assertThat("並行タスクの場合、全担当ユーザの実行順が0となること。（二人目）", assigned.get(1).getExecutionOrder(), is(0));
        assertThat("今回アサインされたユーザだけが登録されていること。", assigned.size(), is(2));
    }

    /**
     * 順次タスクに担当グループを割り当てる場合の {@link Task#assignGroups(String, List)} のテスト。
     */
    @Test
    public void testAssignGroupsToSequentialTask() throws Exception {
        Task sut = createTask(TASK_ID, Task.MultiInstanceType.SEQUENTIAL, null);
        List<String> assignee = Arrays.asList("g000000003", "g000000002", "g000000001");

        sut.assignGroups(INSTANCE_ID, assignee);
        rule.commit();

        List<TaskAssignedGroupEntity> assigned = getWorkflowInstanceDao().findTaskAssignedGroup(INSTANCE_ID, TASK_ID);
        assertThat("担当ユーザが登録されていること。（一人目）", assigned.get(0).getAssignedGroupId(), is("g000000003"));
        assertThat("順次タスクの場合、担当ユーザの実行順はassignUsersの引数の順になっていること。（一人目）", assigned.get(0).getExecutionOrder(), is(1));
        assertThat("担当ユーザが登録されていること。（二人目）", assigned.get(1).getAssignedGroupId(), is("g000000002"));
        assertThat("順次タスクの場合、担当ユーザの実行順はassignUsersの引数の順になっていること。（二人目）", assigned.get(1).getExecutionOrder(), is(2));
        assertThat("担当ユーザが登録されていること。（三人目）", assigned.get(2).getAssignedGroupId(), is("g000000001"));
        assertThat("順次タスクの場合、担当ユーザの実行順はassignUsersの引数の順になっていること。（三人目）", assigned.get(2).getExecutionOrder(), is(3));
        assertThat("今回アサインされたユーザだけが登録されていること。", assigned.size(), is(3));
    }

    /**
     * タスクに担当グループが割り当てられている場合の {@link Task#refreshActiveGroupTasks(String, List)} のテスト。
     */
    @Test
    public void testRefreshActiveGroupTasksForTask() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.NONE, null);
        task.refreshActiveGroupTasks(INSTANCE_ID, Arrays.asList("g000000001"));
        rule.commit();

        List<ActiveGroupTaskEntity> activeGroupTask = getWorkflowInstanceDao().findActiveGroupTask(INSTANCE_ID);
        assertThat("登録されていた担当ユーザのアクティブユーザタスクが作成されること。", activeGroupTask.get(0).getAssignedGroupId(), is("g000000001"));
        assertThat("登録されていた担当ユーザの分だけのアクティブユーザタスクしか作成されないこと。", activeGroupTask.size(), is(1));
    }

    /**
     * 並列タスクに担当ユーザが割り当てられている場合の {@link Task#refreshActiveUserTasks(String, List)} のテスト。
     */
    @Test
    public void testRefreshActiveGroupTasksForParallelTask() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.PARALLEL, null);
        task.refreshActiveGroupTasks(INSTANCE_ID, Arrays.asList("g000000001", "g000000002"));
        rule.commit();

        List<ActiveGroupTaskEntity> activeGroupTask = getWorkflowInstanceDao().findActiveGroupTask(INSTANCE_ID);
        // 実行順でしかソートされていないので、アサート用に担当ユーザでソートしておく。
        Collections.sort(activeGroupTask, new Comparator<TaskAssignedGroupEntity>() {
            @Override
            public int compare(TaskAssignedGroupEntity o1, TaskAssignedGroupEntity o2) {
                return o1.getAssignedGroupId().compareTo(o2.getAssignedGroupId());
            }
        });

        assertThat("並行タスクの場合、登録されていた全担当ユーザのアクティブユーザタスクが作成されること。（一人目）",
                activeGroupTask.get(0).getAssignedGroupId(), is("g000000001"));
        assertThat("並行タスクの場合、登録されていた全担当ユーザのアクティブユーザタスクが作成されること。（二人目）",
                activeGroupTask.get(1).getAssignedGroupId(), is("g000000002"));
        assertThat("登録されていた担当ユーザの分だけのアクティブユーザタスクしか作成されないこと。", activeGroupTask.size(), is(2));
    }

    /**
     * 順次タスクに担当ユーザが割り当てられている場合の、 {@link Task#refreshActiveUserTasks(String, List)} のテスト。
     */
    @Test
    public void testRefreshActiveGroupTasksForSequentialTask() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.SEQUENTIAL, null);
        task.refreshActiveGroupTasks(INSTANCE_ID, Arrays.asList("g000000003", "g000000002", "g000000001"));
        rule.commit();

        List<ActiveGroupTaskEntity> activeGroupTask = getWorkflowInstanceDao().findActiveGroupTask(INSTANCE_ID);
        assertThat("並行タスクの場合、登録されていた担当ユーザのうち、実行順が先頭のアクティブユーザタスクが作成されること。",
                activeGroupTask.get(0).getAssignedGroupId(), is("g000000003"));
        assertThat("登録されていた担当ユーザのうち、実行順が先頭のアクティブユーザタスクしか作成されないこと。", activeGroupTask.size(), is(1));
    }

    /**
     * 並行タスクに担当ユーザがアサインされていない場合の {@link Task#refreshActiveUserTasks(String, List)} のテスト。
     */
    @Test
    public void testRefreshActiveGroupTasksForParallelTask_NotAssigned() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.PARALLEL, null);
        task.refreshActiveGroupTasks(INSTANCE_ID, Collections.<String>emptyList());
        rule.commit();

        List<ActiveGroupTaskEntity> activeGroupTask = getWorkflowInstanceDao().findActiveGroupTask(INSTANCE_ID);
        assertThat("担当ユーザがアサインされていない場合には、アクティブユーザタスクが空になっていること。", activeGroupTask.size(), is(0));
    }

    /**
     * 順次タスクに担当ユーザがアサインされていない場合の {@link Task#refreshActiveUserTasks(String, List)} のテスト。
     */
    @Test
    public void testRefreshActiveGroupTasksForSequentialTask_NotAssigned() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.SEQUENTIAL, null);
        task.refreshActiveGroupTasks(INSTANCE_ID, Collections.<String>emptyList());
        rule.commit();

        List<ActiveGroupTaskEntity> activeGroupTask = getWorkflowInstanceDao().findActiveGroupTask(INSTANCE_ID);
        assertThat("担当ユーザがアサインされていない場合には、アクティブユーザタスクが空になっていること。", activeGroupTask.size(), is(0));
    }

    /**
     * 担当ユーザの振り替えが正しく実行される場合の {@link Task#changeAssignedUser(String, String, String)} のテスト。
     */
    @Test
    public void testChangeAssignedUser() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.SEQUENTIAL, null);
        task.assignUsers(INSTANCE_ID, Arrays.asList("u000000003", "u000000002", "u000000001"));
        rule.commit();

        task.changeAssignedUser(INSTANCE_ID, "u000000002", "u000000004");
        rule.commit();

        List<TaskAssignedUserEntity> assigned = getWorkflowInstanceDao().findTaskAssignedUser(INSTANCE_ID, TASK_ID);
        assertThat("振替え対象でない担当ユーザはそのまま残っていること。", assigned.get(0).getUserId(), is("u000000003"));
        assertThat("振替え対象でない担当ユーザの実行順はそのままになっていること。", assigned.get(0).getExecutionOrder(), is(1));
        assertThat("振替え対象の担当ユーザは振替え先の担当ユーザに更新されていること。", assigned.get(1).getUserId(), is("u000000004"));
        assertThat("振替え対象の担当ユーザの実行順は、振替元の担当ユーザの実行順を引き継ぐこと。", assigned.get(1).getExecutionOrder(), is(2));
        assertThat("振替え対象でない担当ユーザはそのまま残っていること。", assigned.get(2).getUserId(), is("u000000001"));
        assertThat("振替え対象でない担当ユーザの実行順はそのままになっていること。", assigned.get(2).getExecutionOrder(), is(3));
        assertThat("アサインされた担当ユーザだけが登録されていること。", assigned.size(), is(3));
    }

    /**
     * 振替元の担当ユーザがタスクに割当てられていない場合の {@link Task#changeActiveUserTask(String, String, String)} のテスト。
     */
    @Test
    public void testChangeAssignedUser_NotAssigned() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.SEQUENTIAL, null);
        List<String> assigned = Arrays.asList("u000000002", "u000000003");
        task.assignUsers(INSTANCE_ID, assigned);
        rule.commit();

        try {
            task.changeAssignedUser(INSTANCE_ID, "u000000001", "u000000004");
            fail("振替元の担当ユーザがタスクにアサインされていない場合は、例外が発生しなくてはいけない。");
        } catch (IllegalStateException e) {
            String message = e.getMessage();
            assertThat(message, containsString("User is not assigned to task."));
            assertThat(message, containsString("instance id = [" + INSTANCE_ID + "]"));
            assertThat(message, containsString("task id = [" + TASK_ID + "]"));
            assertThat(message, containsString("old user = [u000000001]"));
            assertThat(message, containsString("assigned user = [" + assigned + "]"));
        }
    }

    /**
     * アクティブユーザタスクの更新が行われる場合の {@link Task#changeActiveUserTask(String, String, String)} のテスト。
     */
    @Test
    public void testChangeActiveUserTask() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.PARALLEL, null);
        task.refreshActiveUserTasks(INSTANCE_ID, Arrays.asList("u000000003", "u000000002", "u000000001"));
        rule.commit();

        task.changeActiveUserTask(INSTANCE_ID, "u000000002", "u000000004");
        rule.commit();

        List<ActiveUserTaskEntity> assigned = getWorkflowInstanceDao().findActiveUserTask(INSTANCE_ID);
        // 実行順でしかソートされていないので、アサート用に担当ユーザでソートしておく。
        Collections.sort(assigned, new Comparator<TaskAssignedUserEntity>() {
            @Override
            public int compare(TaskAssignedUserEntity o1, TaskAssignedUserEntity o2) {
                return o1.getUserId().compareTo(o2.getUserId());
            }
        });

        assertThat("振替え対象でないアクティブユーザタスクはそのまま残っていること。", assigned.get(0).getUserId(), is("u000000001"));
        assertThat("振替え対象でないアクティブユーザタスクの実行順はそのままになっていること。", assigned.get(0).getExecutionOrder(), is(0));
        assertThat("振替え対象でないアクティブユーザタスクはそのまま残っていること。", assigned.get(1).getUserId(), is("u000000003"));
        assertThat("振替え対象でないアクティブユーザタスクの実行順はそのままになっていること。", assigned.get(1).getExecutionOrder(), is(0));
        assertThat("振替え対象のアクティブユーザタスクは振替え先のアクティブユーザタスクに更新されていること。", assigned.get(2).getUserId(), is("u000000004"));
        assertThat("振替え対象のアクティブユーザタスクの実行順は、振替元のアクティブユーザタスクの実行順を引き継ぐこと。", assigned.get(2).getExecutionOrder(), is(0));
        assertThat("アサインされたアクティブユーザタスクだけが登録されていること。", assigned.size(), is(3));
    }

    /**
     * 振替元が見つからないため、アクティブユーザタスクの更新が行われない場合の {@link Task#changeActiveUserTask(String, String, String)} のテスト。
     */
    @Test
    public void testChangeActiveUserTask_NotAssigned() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.SEQUENTIAL, null);
        task.refreshActiveUserTasks(INSTANCE_ID, Arrays.asList("u000000003", "u000000002", "u000000001"));
        rule.commit();

        task.changeActiveUserTask(INSTANCE_ID, "u000000002", "u000000004");
        rule.commit();

        List<ActiveUserTaskEntity> assigned = getWorkflowInstanceDao().findActiveUserTask(INSTANCE_ID);
        assertThat("振替え対象でないアクティブユーザタスクはそのまま残っていること。", assigned.get(0).getUserId(), is("u000000003"));
        assertThat("振替え対象でないアクティブユーザタスクの実行順はそのままになっていること。", assigned.get(0).getExecutionOrder(), is(1));
        assertThat("アサインされたアクティブユーザタスクだけが登録されていること。", assigned.size(), is(1));
    }

    /**
     * 担当グループの振り替えが正しく実行される場合の {@link Task#changeAssignedGroup(String, String, String)} のテスト。
     */
    @Test
    public void testChangeAssignedGroup() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.SEQUENTIAL, null);
        task.assignGroups(INSTANCE_ID, Arrays.asList("g000000003", "g000000002", "g000000001"));
        rule.commit();

        task.changeAssignedGroup(INSTANCE_ID, "g000000002", "g000000004");
        rule.commit();

        List<TaskAssignedGroupEntity> assigned = getWorkflowInstanceDao().findTaskAssignedGroup(INSTANCE_ID, TASK_ID);
        assertThat("振替え対象でない担当グループはそのまま残っていること。", assigned.get(0).getAssignedGroupId(), is("g000000003"));
        assertThat("振替え対象でない担当グループの実行順はそのままになっていること。", assigned.get(0).getExecutionOrder(), is(1));
        assertThat("振替え対象の担当グループは振替え先の担当グループに更新されていること。", assigned.get(1).getAssignedGroupId(), is("g000000004"));
        assertThat("振替え対象の担当グループの実行順は、振替元の担当グループの実行順を引き継ぐこと。", assigned.get(1).getExecutionOrder(), is(2));
        assertThat("振替え対象でない担当グループはそのまま残っていること。", assigned.get(2).getAssignedGroupId(), is("g000000001"));
        assertThat("振替え対象でない担当グループの実行順はそのままになっていること。", assigned.get(2).getExecutionOrder(), is(3));
        assertThat("アサインされた担当グループだけが登録されていること。", assigned.size(), is(3));
    }

    /**
     * 振替元の担当グループがタスクに割当てられていない場合の {@link Task#changeActiveGroupTask(String, String, String)} のテスト。
     */
    @Test
    public void testChangeAssignedGroup_NotAssigned() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.SEQUENTIAL, null);
        List<String> assigned = Arrays.asList("g000000002", "g000000003");
        task.assignGroups(INSTANCE_ID, assigned);
        rule.commit();

        try {
            task.changeAssignedGroup(INSTANCE_ID, "g000000001", "g000000004");
            fail("振替元の担当グループがタスクにアサインされていない場合は、例外が発生しなくてはいけない。");
        } catch (IllegalStateException e) {
            String message = e.getMessage();
            assertThat(message, containsString("Group is not assigned to task."));
            assertThat(message, containsString("instance id = [" + INSTANCE_ID + "]"));
            assertThat(message, containsString("task id = [" + TASK_ID + "]"));
            assertThat(message, containsString("old group = [g000000001]"));
            assertThat(message, containsString("assigned group = [" + assigned + "]"));
        }
    }

    /**
     * 振替元が見つからないため、アクティブグループタスクの更新が行われる場合の {@link Task#changeActiveGroupTask(String, String, String)} のテスト。
     */
    @Test
    public void testChangeActiveGroupTask() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.PARALLEL, null);
        task.refreshActiveGroupTasks(INSTANCE_ID, Arrays.asList("g000000003", "g000000002", "g000000001"));
        rule.commit();

        task.changeActiveGroupTask(INSTANCE_ID, "g000000002", "g000000004");
        rule.commit();

        List<ActiveGroupTaskEntity> assigned = getWorkflowInstanceDao().findActiveGroupTask(INSTANCE_ID);
        // 実行順でしかソートされていないので、アサート用に担当グループでソートしておく。
        Collections.sort(assigned, new Comparator<TaskAssignedGroupEntity>() {
            @Override
            public int compare(TaskAssignedGroupEntity o1, TaskAssignedGroupEntity o2) {
                return o1.getAssignedGroupId().compareTo(o2.getAssignedGroupId());
            }
        });

        assertThat("振替え対象でないアクティブグループタスクはそのまま残っていること。", assigned.get(0).getAssignedGroupId(), is("g000000001"));
        assertThat("振替え対象でないアクティブグループタスクの実行順はそのままになっていること。", assigned.get(0).getExecutionOrder(), is(0));
        assertThat("振替え対象でないアクティブグループタスクはそのまま残っていること。", assigned.get(1).getAssignedGroupId(), is("g000000003"));
        assertThat("振替え対象でないアクティブグループタスクの実行順はそのままになっていること。", assigned.get(1).getExecutionOrder(), is(0));
        assertThat("振替え対象のアクティブグループタスクは振替え先のアクティブグループタスクに更新されていること。", assigned.get(2).getAssignedGroupId(), is("g000000004"));
        assertThat("振替え対象のアクティブグループタスクの実行順は、振替元のアクティブグループタスクの実行順を引き継ぐこと。", assigned.get(2).getExecutionOrder(), is(0));
        assertThat("アサインされたアクティブグループタスクだけが登録されていること。", assigned.size(), is(3));
    }

    /**
     * アクティブグループタスクの更新が行われない場合の {@link Task#changeActiveGroupTask(String, String, String)} のテスト。
     */
    @Test
    public void testChangeActiveGroupTask_NotAssigned() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.SEQUENTIAL, null);
        task.refreshActiveGroupTasks(INSTANCE_ID, Arrays.asList("g000000003", "g000000002", "g000000001"));
        rule.commit();

        task.changeActiveGroupTask(INSTANCE_ID, "g000000002", "g000000004");
        rule.commit();

        List<ActiveGroupTaskEntity> assigned = getWorkflowInstanceDao().findActiveGroupTask(INSTANCE_ID);
        assertThat("振替え対象でないアクティブグループタスクはそのまま残っていること。", assigned.get(0).getAssignedGroupId(), is("g000000003"));
        assertThat("振替え対象でないアクティブグループタスクの実行順はそのままになっていること。", assigned.get(0).getExecutionOrder(), is(1));
        assertThat("アサインされたアクティブグループタスクだけが登録されていること。", assigned.size(), is(1));
    }

    /**
     * タスク担当ユーザ/タスク担当グループが割り当てられていない場合の {@link Task#activate(String, Map)} のテスト。
     */
    @Test
    public void testActivateNotAssigned() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.SEQUENTIAL, null);

        task.activate(INSTANCE_ID, Collections.<String, Object>emptyMap());
        rule.commit();

        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        ActiveFlowNodeEntity node = dao.findActiveFlowNode(INSTANCE_ID);
        assertThat("タスクがアクティブフローノードに登録されること", node.getFlowNodeId(), is(TASK_ID));
        List<ActiveUserTaskEntity> userTasks = dao.findActiveUserTask(INSTANCE_ID);
        assertThat("担当ユーザはアサインされていないので、アクティブユーザタスクは登録されていないこと。", userTasks.size(), is(0));
        List<ActiveGroupTaskEntity> groupTasks = dao.findActiveGroupTask(INSTANCE_ID);
        assertThat("担当グループはアサインされていないので、アクティブグループタスクは登録されていないこと。", groupTasks.size(), is(0));
    }

    /**
     * タスク担当ユーザが割り当てられている場合の {@link Task#activate(String, Map)} のテスト。
     */
    @Test
    public void testActivateUserAssigned() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.SEQUENTIAL, null);
        task.assignUsers(INSTANCE_ID, Arrays.asList("u000000001", "u000000002"));

        task.activate(INSTANCE_ID, Collections.<String, Object>emptyMap());
        rule.commit();

        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        ActiveFlowNodeEntity node = dao.findActiveFlowNode(INSTANCE_ID);
        assertThat("タスクがアクティブフローノードに登録されること", node.getFlowNodeId(), is(TASK_ID));
        List<ActiveUserTaskEntity> userTasks = dao.findActiveUserTask(INSTANCE_ID);
        assertThat("必要なアクティブユーザタスクだけが作成されていること。", userTasks.size(), is(1));
        assertThat("アサインされたタスク担当ユーザのアクティブユーザタスクが作成されていること。", userTasks.get(0).getUserId(), is("u000000001"));
        List<ActiveGroupTaskEntity> groupTasks = dao.findActiveGroupTask(INSTANCE_ID);
        assertThat("担当グループはアサインされていないので、アクティブグループタスクは登録されていないこと。", groupTasks.size(), is(0));
    }

    /**
     * タスク担当グループが割り当てられている場合の {@link Task#activate(String, Map)} のテスト。
     */
    @Test
    public void testActivateGroupAssigned() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.PARALLEL, null);
        task.assignGroups(INSTANCE_ID, Arrays.asList("g000000001", "g000000002"));

        task.activate(INSTANCE_ID, Collections.<String, Object>emptyMap());
        rule.commit();

        WorkflowInstanceDao dao = getWorkflowInstanceDao();
        ActiveFlowNodeEntity node = dao.findActiveFlowNode(INSTANCE_ID);
        assertThat("タスクがアクティブフローノードに登録されること", node.getFlowNodeId(), is(TASK_ID));
        List<ActiveUserTaskEntity> userTasks = dao.findActiveUserTask(INSTANCE_ID);
        assertThat("担当ユーザはアサインされていないので、アクティブユーザタスクは登録されていないこと。", userTasks.size(), is(0));
        List<ActiveGroupTaskEntity> groupTasks = dao.findActiveGroupTask(INSTANCE_ID);
        assertThat("必要なアクティブグループタスクだけが作成されていること。", groupTasks.size(), is(2));
        assertThat("アサインされたタスク担当ユーザのアクティブユーザタスクが作成されていること。", groupTasks.get(0).getAssignedGroupId(), is("g000000001"));
        assertThat("アサインされたタスク担当ユーザのアクティブユーザタスクが作成されていること。", groupTasks.get(1).getAssignedGroupId(), is("g000000002"));
    }

    /**
     * タスクに担当ユーザが割り当てられている場合の {@link Task#processNodeByUser(String, Map, String)} のテスト。
     */
    @Test
    public void testProcessNodeOfTask_UserAssigned() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.NONE, "");
        task.assignUsers(INSTANCE_ID, Arrays.asList("someUser__"));
        task.refreshActiveUserTasks(INSTANCE_ID, Arrays.asList("someUser__"));
        rule.commit();

        boolean proceed = task.processNodeByUser(INSTANCE_ID, Collections.<String, Object>emptyMap(), "someUser__");
        rule.commit();

        List<ActiveUserTaskEntity> tasks = getWorkflowInstanceDao().findActiveUserTask(INSTANCE_ID);
        assertThat("実行ユーザに指定されたユーザのアクティブユーザタスクが削除されていること。", tasks, is(empty()));
        assertThat("ノードを処理した結果、CompletionConditionが評価されてtrueが返却されること。", proceed, is(true));
    }

    /**
     * タスクに担当グループが割り当てられている場合の {@link Task#processNodeByGroup(String, Map, String)} のテスト。
     */
    @Test
    public void testProcessNodeOfTask_GroupAssigned() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.NONE, "");
        task.assignGroups(INSTANCE_ID, Arrays.asList("someGroup_"));
        task.refreshActiveGroupTasks(INSTANCE_ID, Arrays.asList("someGroup_"));
        rule.commit();

        boolean proceed = task.processNodeByGroup(INSTANCE_ID, Collections.<String, Object>emptyMap(), "someGroup_");
        rule.commit();

        List<ActiveGroupTaskEntity> tasks = getWorkflowInstanceDao().findActiveGroupTask(INSTANCE_ID);
        assertThat("実行グループに指定されたグループのアクティブグループタスクが削除されていること。", tasks, is(empty()));
        assertThat("ノードを処理した結果、CompletionConditionが評価されてtrueが返却されること。", proceed, is(true));
    }

    /**
     * 並行タスクに担当ユーザが割り当てられている場合の {@link Task#processNodeByUser(String, Map, String)} のテスト。
     */
    @Test
    public void testProcessNodeOfParallelTask_UserAssigned() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.PARALLEL, AllCompletionCondition.class.getName());
        task.assignUsers(INSTANCE_ID, Arrays.asList("someUser1_", "someUser2_", "someUser3_"));
        task.refreshActiveUserTasks(INSTANCE_ID, Arrays.asList("someUser1_", "someUser2_", "someUser3_"));
        rule.commit();

        boolean proceed = task.processNodeByUser(INSTANCE_ID, Collections.<String, Object>emptyMap(), "someUser2_");
        rule.commit();

        List<ActiveUserTaskEntity> tasks = getWorkflowInstanceDao().findActiveUserTask(INSTANCE_ID);
        assertThat("実行ユーザに指定されたユーザのアクティブユーザタスクが削除されていること。", tasks.size(), is(2));
        assertThat("実行ユーザに指定されたユーザ以外のアクティブユーザタスクは削除されていないこと。（一人目）", tasks.get(0).getUserId(), is("someUser1_"));
        assertThat("実行ユーザに指定されたユーザ以外のアクティブユーザタスクは削除されていないこと。（二人目）", tasks.get(1).getUserId(), is("someUser3_"));
        assertThat("ノードを処理した結果、CompletionConditionが評価されてfalseが返却されること。", proceed, is(false));
    }

    /**
     * 並行タスクに担当グループが割り当てられている場合の {@link Task#processNodeByGroup(String, Map, String)} のテスト。
     */
    @Test
    public void testProcessNodeOfParallelTask_GroupAssigned() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.PARALLEL, OrCompletionCondition.class.getName() + "(1)");
        task.assignGroups(INSTANCE_ID, Arrays.asList("someGroup1", "someGroup2", "someGroup3"));
        task.refreshActiveGroupTasks(INSTANCE_ID, Arrays.asList("someGroup1", "someGroup2", "someGroup3"));
        rule.commit();

        boolean proceed = task.processNodeByGroup(INSTANCE_ID, Collections.<String, Object>emptyMap(), "someGroup2");
        rule.commit();

        List<ActiveGroupTaskEntity> tasks = getWorkflowInstanceDao().findActiveGroupTask(INSTANCE_ID);
        assertThat("実行グループに指定されたグループのアクティブグループタスクが削除されていること。", tasks.size(), is(2));
        assertThat("実行グループに指定されたグループ以外のアクティブグループタスクは削除されていないこと。（一人目）",
                tasks.get(0).getAssignedGroupId(), is("someGroup1"));
        assertThat("実行グループに指定されたグループ以外のアクティブグループタスクは削除されていないこと。（二人目）",
                tasks.get(1).getAssignedGroupId(), is("someGroup3"));
        assertThat("ノードを処理した結果、CompletionConditionが評価されてtrueが返却されること。", proceed, is(true));
    }

    /**
     * 順次タスクに担当ユーザが割り当てられている場合の {@link Task#processNodeByUser(String, Map, String)} のテスト。
     * <p/>
     * 実行順が次の担当ユーザが存在する場合。
     */
    @Test
    public void testProcessNodeOfSequentialTask_UserAssigned_HasNext() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.SEQUENTIAL, AllCompletionCondition.class.getName());
        task.assignUsers(INSTANCE_ID, Arrays.asList("someUser1_", "someUser2_", "someUser3_"));
        task.refreshActiveUserTasks(INSTANCE_ID, Arrays.asList("someUser1_", "someUser2_", "someUser3_"));
        rule.commit();

        boolean proceed = task.processNodeByUser(INSTANCE_ID, Collections.<String, Object>emptyMap(), "someUser1_");
        rule.commit();

        List<ActiveUserTaskEntity> tasks = getWorkflowInstanceDao().findActiveUserTask(INSTANCE_ID);
        assertThat("実行ユーザに指定されたユーザの次の実行順となっているアクティブユーザタスクだけがあること。", tasks.size(), is(1));
        assertThat("実行ユーザに指定されたユーザの次の実行順となっているアクティブユーザタスクが残っていること。", tasks.get(0).getUserId(), is("someUser2_"));
        assertThat("ノードを処理した結果、CompletionConditionが評価されてfalseが返却されること。", proceed, is(false));
    }

    /**
     * 順次タスクに担当ユーザが割り当てられている場合の {@link Task#processNodeByUser(String, Map, String)} のテスト。
     * <p/>
     * 実行順が次の担当ユーザが存在しない場合。
     */
    @Test
    public void testProcessNodeOfSequentialTask_UserAssigned_NoNext() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.SEQUENTIAL, AllCompletionCondition.class.getName());
        task.assignUsers(INSTANCE_ID, Arrays.asList("someUser1_"));
        task.refreshActiveUserTasks(INSTANCE_ID, Arrays.asList("someUser1_"));
        rule.commit();

        boolean proceed = task.processNodeByUser(INSTANCE_ID, Collections.<String, Object>emptyMap(), "someUser1_");
        rule.commit();

        List<ActiveUserTaskEntity> tasks = getWorkflowInstanceDao().findActiveUserTask(INSTANCE_ID);
        assertThat("実行ユーザに指定されたユーザのアクティブユーザタスクが削除されていること。", tasks.size(), is(0));
        assertThat("ノードを処理した結果、CompletionConditionが評価されてtrueが返却されること。", proceed, is(true));
    }

    /**
     * 順次タスクに担当グループが割り当てられている場合の {@link Task#processNodeByGroup(String, Map, String)} のテスト。
     * <p/>
     * 実行順が次の担当グループが存在する場合。
     */
    @Test
    public void testProcessNodeOfSequentialTask_GroupAssigned_HasNext() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.SEQUENTIAL, AllCompletionCondition.class.getName());
        task.assignGroups(INSTANCE_ID, Arrays.asList("someGroup1", "someGroup2", "someGroup3"));
        task.refreshActiveGroupTasks(INSTANCE_ID, Arrays.asList("someGroup1", "someGroup2", "someGroup3"));
        rule.commit();

        boolean proceed = task.processNodeByGroup(INSTANCE_ID, Collections.<String, Object>emptyMap(), "someGroup1");
        rule.commit();

        List<ActiveGroupTaskEntity> tasks = getWorkflowInstanceDao().findActiveGroupTask(INSTANCE_ID);
        assertThat("実行グループに指定されたグループの次の実行順となっているアクティブグループタスクだけがあること。", tasks.size(), is(1));
        assertThat("実行グループに指定されたグループの次の実行順となっているアクティブグループタスクが残っていること。", tasks.get(0).getAssignedGroupId(), is("someGroup2"));
        assertThat("ノードを処理した結果、CompletionConditionが評価されてfalseが返却されること。", proceed, is(false));
    }

    /**
     * 順次タスクに担当グループが割り当てられている場合の {@link Task#processNodeByGroup(String, Map, String)} のテスト。
     * <p/>
     * 実行順が次の担当グループが存在しない場合。
     */
    @Test
    public void testProcessNodeOfSequentialTask_GroupAssigned_NoNext() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.SEQUENTIAL, AllCompletionCondition.class.getName());
        task.assignGroups(INSTANCE_ID, Arrays.asList("someGroup1"));
        task.refreshActiveGroupTasks(INSTANCE_ID, Arrays.asList("someGroup1"));
        rule.commit();

        boolean proceed = task.processNodeByGroup(INSTANCE_ID, Collections.<String, Object>emptyMap(), "someGroup1");
        rule.commit();

        List<ActiveGroupTaskEntity> tasks = getWorkflowInstanceDao().findActiveGroupTask(INSTANCE_ID);
        assertThat("実行ユーザに指定されたユーザのアクティブユーザタスクが削除されていること。", tasks.size(), is(0));
        assertThat("ノードを処理した結果、CompletionConditionが評価されてtrueが返却されること。", proceed, is(true));
    }

    /**
     * 指定された実行ユーザのアクティブユーザタスクが存在しない場合の {@link Task#processNodeByUser(String, Map, String)} のテスト。
     */
    @Test
    public void testProcessNode_NotAssignedUser() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.NONE, null);
        task.assignUsers(INSTANCE_ID, Arrays.asList("someUser1_"));
        task.refreshActiveUserTasks(INSTANCE_ID, Arrays.asList("someUser1_"));
        rule.commit();

        try {
            task.processNodeByUser(INSTANCE_ID, Collections.<String, Object>emptyMap(), "not an assigned user");
            fail("指定された実行ユーザのアクティブユーザタスクが存在しない場合には、例外が発生しなくてはいけない。");
        } catch (IllegalStateException e) {
            String actual = e.getMessage();
            assertThat(actual, containsString("Active task is not found for "));
            assertThat(actual, containsString("user = [not an assigned user]"));
            assertThat(actual, containsString("instance id = [" + INSTANCE_ID + "]"));
            assertThat(actual, containsString("task id = [" + TASK_ID + "]"));
        }
    }

    /**
     * 指定された実行グループのアクティブグループタスクが存在しない場合の {@link Task#processNodeByGroup(String, Map, String)} のテスト。
     */
    @Test
    public void testProcessNode_NotAssignedGroup() throws Exception {
        Task task = createTask(TASK_ID, Task.MultiInstanceType.NONE, null);
        task.assignGroups(INSTANCE_ID, Arrays.asList("someGroup1"));
        task.refreshActiveGroupTasks(INSTANCE_ID, Arrays.asList("someGroup1"));
        rule.commit();

        try {
            task.processNodeByGroup(INSTANCE_ID, Collections.<String, Object>emptyMap(), "not an assigned group");
            fail("指定された実行グループのアクティブグループタスクが存在しない場合には、例外が発生しなくてはいけない。");
        } catch (IllegalStateException e) {
            String actual = e.getMessage();
            assertThat(actual, containsString("Active task is not found for "));
            assertThat(actual, containsString("group = [not an assigned group]"));
            assertThat(actual, containsString("instance id = [" + INSTANCE_ID + "]"));
            assertThat(actual, containsString("task id = [" + TASK_ID + "]"));
        }
    }

    public static Task createTask(String taskId, Task.MultiInstanceType type, String condition) {
        return new Task(taskId, "dummy", "l01", type.toString(), condition, Collections.<SequenceFlow>emptyList());
    }

    /**
     * ワークフローインスタンス系テーブルへのアクセスクラスを取得する。
     *
     * @return ワークフローインスタンスDAO
     */
    private WorkflowInstanceDao getWorkflowInstanceDao() {
        return WorkflowConfig.get().getWorkflowInstanceDao();
    }
}
