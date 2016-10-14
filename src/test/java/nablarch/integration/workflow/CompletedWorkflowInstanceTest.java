package nablarch.integration.workflow;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * {@link WorkflowInstance.CompletedWorkflowInstance} のテストクラス。
 *
 * @author Ryo Tanaka
 * @since 1.4.2
 */
public class CompletedWorkflowInstanceTest {

    /**
     * テスト対象
     */
    private final WorkflowInstance sut = new WorkflowInstance.CompletedWorkflowInstance("completedInstanceId");

    /**
     * {@link WorkflowInstance.CompletedWorkflowInstance#isActive(String)} のテスト。
     */
    @Test
    public void testIsActive() {
        assertThat("完了状態のワークフローインスタンスでは、常にfalseが返却されること。", sut.isActive("taskId"), is(false));
    }

    /**
     * {@link WorkflowInstance.CompletedWorkflowInstance#isCompleted()} のテスト。
     */
    @Test
    public void testIsCompleted() {
        assertThat("完了状態のワークフローインスタンスでは、常にtrueが返却されること。", sut.isCompleted(), is(true));
    }

    /**
     * {@link WorkflowInstance.CompletedWorkflowInstance#getInstanceId()} のテスト。
     */
    @Test
    public void testGetInstanceId() {
        assertThat("コンストラクタで指定したワークフローインスタンスIDが取得できること。", sut.getInstanceId(), is("completedInstanceId"));
    }

    /**
     * {@link WorkflowInstance.CompletedWorkflowInstance#getWorkflowId()} のテスト
     */
    @Test
    public void testGetWorkflowId() {
        try {
            sut.getWorkflowId();
            fail("完了状態のワークフローインスタンスに対してワークフローIDを取得しようとした場合、例外が発生しなくてはいけない。");
        } catch (UnsupportedOperationException e) {
            assertErrorMessage(e);
        }
    }

    /**
     * {@link WorkflowInstance.CompletedWorkflowInstance#getVersion()} のテスト
     */
    @Test
    public void testGetVersion() {
        try {
            sut.getVersion();
            fail("完了状態のワークフローインスタンスに対してバージョンを取得しようとした場合、例外が発生しなくてはいけない。");
        } catch (UnsupportedOperationException e) {
            assertErrorMessage(e);
        }
    }

    /**
     * {@link WorkflowInstance.CompletedWorkflowInstance#completeUserTask(Map, String)} のテスト
     */
    @Test
    public void testCompleteUserTask() {
        try {
            sut.completeUserTask(Collections.<String, Object>emptyMap(), "assigned");
            fail("完了状態のワークフローインスタンスに対してタスクを終了しようとした場合、例外が発生しなくてはいけない。");
        } catch (IllegalStateException e) {
            assertErrorMessage(e);
        }
    }

    /**
     * {@link WorkflowInstance.CompletedWorkflowInstance#completeGroupTask(Map, String)} のテスト
     */
    @Test
    public void testCompleteGroupTask() {
        try {
            sut.completeGroupTask(Collections.<String, Object>emptyMap(), "assigned");
            fail("完了状態のワークフローインスタンスに対してタスクを終了しようとした場合、例外が発生しなくてはいけない。");
        } catch (IllegalStateException e) {
            assertErrorMessage(e);
        }
    }

    /**
     * {@link WorkflowInstance.CompletedWorkflowInstance#triggerEvent(String, Map)} のテスト
     */
    @Test
    public void testTriggerEvent() {
        try {
            sut.triggerEvent("eventId", Collections.<String, Object>emptyMap());
            fail("完了状態のワークフローインスタンスに対して境界イベントを発生させようとした場合、例外が発生しなくてはいけない。");
        } catch (IllegalStateException e) {
            assertErrorMessage(e);
        }
    }

    /**
     * {@link WorkflowInstance.CompletedWorkflowInstance#assignUsers(String, List)} のテスト
     */
    @Test
    public void testAssignUsers() {
        try {
            sut.assignUsers("taskId", Collections.<String>emptyList());
            fail("完了状態のワークフローインスタンスに対して担当ユーザをアサインしようとした場合、例外が発生しなくてはいけない。");
        } catch (IllegalStateException e) {
            assertErrorMessage(e);
        }
    }

    /**
     * {@link WorkflowInstance.CompletedWorkflowInstance#assignGroups(String, List)} のテスト
     */
    @Test
    public void testAssignGroups() {
        try {
            sut.assignGroups("taskId", Collections.<String>emptyList());
            fail("完了状態のワークフローインスタンスに対して担当グループをアサインしようとした場合、例外が発生しなくてはいけない。");
        } catch (IllegalStateException e) {
            assertErrorMessage(e);
        }
    }

    /**
     * {@link WorkflowInstance.CompletedWorkflowInstance#assignUsersToLane(String, List)} のテスト
     */
    @Test
    public void testAssignUsersToLane() {
        try {
            sut.assignUsersToLane("laneId", Collections.<String>emptyList());
            fail("完了状態のワークフローインスタンスに対してレーンを指定して担当ユーザをアサインしようとした場合、例外が発生しなくてはいけない。");
        } catch (IllegalStateException e) {
            assertErrorMessage(e);
        }
    }

    /**
     * {@link WorkflowInstance.CompletedWorkflowInstance#assignGroupsToLane(String, List)} のテスト
     */
    @Test
    public void testAssignGroupsToLane() {
        try {
            sut.assignGroupsToLane("laneId", Collections.<String>emptyList());
            fail("完了状態のワークフローインスタンスに対してレーンを指定して担当グループをアサインしようとした場合、例外が発生しなくてはいけない。");
        } catch (IllegalStateException e) {
            assertErrorMessage(e);
        }
    }

    /**
     * {@link WorkflowInstance.CompletedWorkflowInstance#changeAssignedUser(String, String, String)} のテスト
     */
    @Test
    public void testChangeAssignedUser() {
        try {
            sut.changeAssignedUser("taskId", "from", "to");
            fail("完了状態のワークフローインスタンスに対して担当ユーザを振替えようとした場合、例外が発生しなくてはいけない。");
        } catch (IllegalStateException e) {
            assertErrorMessage(e);
        }
    }

    /**
     * {@link WorkflowInstance.CompletedWorkflowInstance#changeAssignedGroup(String, String, String)} のテスト
     */
    @Test
    public void testChangeAssignedGroup() {
        try {
            sut.changeAssignedGroup("taskId", "from", "to");
            fail("完了状態のワークフローインスタンスに対して担当グループを振替えようとした場合、例外が発生しなくてはいけない。");
        } catch (IllegalStateException e) {
            assertErrorMessage(e);
        }
    }

    /**
     * {@link WorkflowInstance.CompletedWorkflowInstance#getAssignedUsers(String)} のテスト
     */
    @Test
    public void testGetAssignedUsers() {
        assertThat("完了状態のワークフローでは、常に空のリストが取得できること。", sut.getAssignedUsers("taskId"), is(Collections.<String>emptyList()));
    }

    /**
     * {@link WorkflowInstance.CompletedWorkflowInstance#getAssignedGroups(String)} のテスト
     */
    @Test
    public void testGetAssignedGroups() {
        assertThat("完了状態のワークフローでは、常に空のリストが取得できること。", sut.getAssignedGroups("taskId"), is(Collections.<String>emptyList()));
    }

    /**
     * {@link WorkflowInstance.CompletedWorkflowInstance#hasActiveUserTask(String)} のテスト
     */
    @Test
    public void testHasActiveUserTask() {
        assertThat("完了状態のワークフローでは、常にfalseが返却されること。", sut.hasActiveUserTask("user"), is(false));
    }

    /**
     * {@link WorkflowInstance.CompletedWorkflowInstance#hasActiveGroupTask(String)} のテスト
     */
    @Test
    public void testHasActiveGroupTask() {
        assertThat("完了状態のワークフローでは、常にfalseが返却されること。", sut.hasActiveGroupTask("group"), is(false));
    }

    /**
     * 発生したエラーのメッセージを検証する。
     *
     * @param e 発生したエラー
     */
    private static void assertErrorMessage(Throwable e) {
        assertThat(e.getMessage(), is("Workflow is already completed. instance id = [completedInstanceId]"));
    }
}
