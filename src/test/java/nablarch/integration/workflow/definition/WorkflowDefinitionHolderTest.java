package nablarch.integration.workflow.definition;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import nablarch.integration.workflow.testhelper.SystemTimeProviderImpl;

/**
 * {@link WorkflowDefinitionHolder}のテスト。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class WorkflowDefinitionHolderTest {

    /**
     * プロセスIDを指定してプロセス定義が取得できること。
     */
    @Test
    public void testGetProcess() throws Exception {
        WorkflowDefinitionHolder holder = new WorkflowDefinitionHolder();
        holder.setSystemTimeProvider(new SystemTimeProviderImpl());
        holder.addWorkflow(new WorkflowDefinition("process1", 1, "プロセス名", "20140101"));
        holder.addWorkflow(new WorkflowDefinition("process2", 1, "プロセス名", "20140102"));

        WorkflowDefinition process = holder.getWorkflowDefinition("process1");
        assertThat(process.getWorkflowId(), is("process1"));
    }

    /**
     * プロセスIDに紐づくデータが複数バージョンあった場合、
     * プロセスID指定の場合、有効日付内の最新のバージョンが取得できること。
     */
    @Test
    public void testMultiVersion() {
        WorkflowDefinitionHolder holder = new WorkflowDefinitionHolder();
        holder.setSystemTimeProvider(new SystemTimeProviderImpl());
        SystemTimeProviderImpl.now = "20140725";

        holder.addWorkflow(new WorkflowDefinition("process1", 1, "プロセス名", "20140724"));
        holder.addWorkflow(new WorkflowDefinition("process1", 1, "プロセス名", "20140724"));
        holder.addWorkflow(new WorkflowDefinition("process1", 3, "プロセス名", "20140724"));
        holder.addWorkflow(new WorkflowDefinition("process1", 2, "プロセス名", "20140724"));
        holder.addWorkflow(new WorkflowDefinition("process1", 99, "最新プロセス", "20140726"));
        holder.addWorkflow(new WorkflowDefinition("process1", 50, "プロセス名", "20140725"));

        WorkflowDefinition process = holder.getWorkflowDefinition("process1");
        assertThat(process.getWorkflowId(), is("process1"));
        assertThat("最新バージョンの99は有効期間未到来なので、1つ前のバージョンが取得できる。", process.getVersion(), is(50));
    }

    /**
     * バージョン番号を指定した場合、そのバージョンのプロセスが取得できること。
     */
    @Test
    public void testGetSpecificVersion() {
        WorkflowDefinitionHolder holder = new WorkflowDefinitionHolder();
        holder.addWorkflow(new WorkflowDefinition("process1", 99, "最新プロセス", "20140101"));
        holder.addWorkflow(new WorkflowDefinition("process1", 50, "プロセス名", "20130101"));
        holder.addWorkflow(new WorkflowDefinition("process2", 50, "異なるワークフロー定義", "20130203"));
        holder.addWorkflow(new WorkflowDefinition("process1", 1, "プロセス名", "20140202"));

        WorkflowDefinition process = holder.getWorkflowDefinition("process1", 50);
        assertThat(process.getWorkflowId(), is("process1"));
        assertThat(process.getVersion(), is(50));
    }

    /**
     * 指定したプロセスIDに対応するプロセス定義が存在しない場合は例外が発生すること。
     */
    @Test
    public void testProcessIdNotFound() {
        WorkflowDefinitionHolder holder = new WorkflowDefinitionHolder();
        holder.addWorkflow(new WorkflowDefinition("process1", 99, "最新プロセス", "20140302"));
        holder.addWorkflow(new WorkflowDefinition("process3", 50, "プロセス名", "20130502"));

        try {
            holder.getWorkflowDefinition("process2");
            fail("ここは通らない");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("Workflow definition was not found."));
            assertThat(e.getMessage(), containsString("workflow id = [process2]"));
        }
    }

    /**
     * 指定したプロセスIDに対応するプロセス定義が存在するが、有効期間外の場合は例外が発生すること。
     */
    @Test
    public void testNotEffectiveDate() {
        WorkflowDefinitionHolder holder = new WorkflowDefinitionHolder();
        holder.setSystemTimeProvider(new SystemTimeProviderImpl());
        SystemTimeProviderImpl.now = "20140708";
        holder.addWorkflow(new WorkflowDefinition("process1", 2, "最新プロセス", "20140710"));
        holder.addWorkflow(new WorkflowDefinition("process1", 1, "プロセス名", "20140709"));

        try {
            holder.getWorkflowDefinition("process1");
            fail("ここは通らない");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("Workflow definition was not found."));
            assertThat(e.getMessage(), containsString("workflow id = [process1]"));
        }
    }

    /**
     * 指定したバージョン番号に対応するプロセス定義が存在しない場合は例外が発生すること。
     */
    @Test
    public void testVersionNotFound() {
        WorkflowDefinitionHolder holder = new WorkflowDefinitionHolder();
        holder.addWorkflow(new WorkflowDefinition("process1", 10, "最新プロセス", "20130202"));
        holder.addWorkflow(new WorkflowDefinition("process1", 12, "プロセス名", "20140302"));

        try {
            holder.getWorkflowDefinition("process1", 11);
            fail("ここは通らない");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("Workflow definition was not found."));
            assertThat(e.getMessage(), containsString("workflow id = [process1], version = [11]"));
        }
    }
}

