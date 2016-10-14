package nablarch.integration.workflow.definition;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import nablarch.core.util.DateUtil;
import nablarch.core.date.SystemTimeProvider;
import nablarch.core.repository.initialization.Initializable;

import nablarch.integration.workflow.definition.loader.WorkflowDefinitionLoader;
import nablarch.integration.workflow.util.WorkflowUtil;

/**
 * ワークフローの定義情報を保持するクラス。
 * <p/>
 * ワークフロー定義情報は、ワークフローIDを指定して取得できる。
 * <p/>
 * ワークフローのバージョン番号が特定できている場合には、
 * ワークフローIDとバージョン番号を共に指定してワークフロー定義情報を取得することができる。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class WorkflowDefinitionHolder implements Initializable {

    /** ワークフロー定義情報 */
    private List<WorkflowDefinition> workflowDefinitions = new ArrayList<WorkflowDefinition>();

    /** ワークフロー定義のロードクラス */
    private WorkflowDefinitionLoader workflowDefinitionLoader;

    /** 基準日(適用期間の判断に使用する基準日) */
    private SystemTimeProvider systemTimeProvider;

    /**
     * ワークフローIDに紐づく適用期間内のワークフロー定義を取得する。
     * <p/>
     * 適用期間内に複数のワークフロー定義情報が存在していた場合には、
     * バージョン番号が最大のワークフロー定義情報を返却する。
     *
     * @param workflowId ワークフローID
     * @return ワークフロー定義
     * @throws IllegalArgumentException ワークフロー定義が存在しない場合
     */
    public WorkflowDefinition getWorkflowDefinition(final String workflowId) throws IllegalArgumentException {

        List<WorkflowDefinition> filtered = WorkflowUtil.filterList(workflowDefinitions,
                new WorkflowUtil.ListFilter<WorkflowDefinition>() {
                    @Override
                    public boolean isMatch(WorkflowDefinition other) {
                        return other.getWorkflowId().equals(workflowId)
                                && isEffectiveWorkflow(other.getEffectiveDate());
                    }

                    /**
                     * 適用期間内のワークフローか否かを判定する。
                     * @param effectiveDate 適用日
                     * @return 有効な場合はtrue
                     */
                    private boolean isEffectiveWorkflow(String effectiveDate) {
                        return DateUtil.getDays(getReferenceDate(), effectiveDate) <= 0;
                    }
                });

        if (filtered.isEmpty()) {
            throw new IllegalArgumentException("Workflow definition was not found. workflow id = [" + workflowId + "]");
        }

        // 最大のバージョン番号を持つワークフロー定義を返却
        return Collections.max(filtered, new Comparator<WorkflowDefinition>() {
            @Override
            public int compare(WorkflowDefinition o1, WorkflowDefinition o2) {
                return o1.getVersion() - o2.getVersion();
            }
        });
    }

    /**
     * ワークフローID及びバージョン番号に紐づくワークフロー定義を取得する。
     *
     * @param workflowId ワークフローID
     * @param version バージョン番号
     * @return ワークフロー定義
     * @throws IllegalArgumentException ワークフロー定義が存在しない場合
     */
    public WorkflowDefinition getWorkflowDefinition(final String workflowId, final long version)
            throws IllegalArgumentException {
        List<WorkflowDefinition> filtered = WorkflowUtil.filterList(workflowDefinitions,
                new WorkflowUtil.ListFilter<WorkflowDefinition>() {
                    @Override
                    public boolean isMatch(WorkflowDefinition other) {
                        return (other.getWorkflowId().equals(workflowId))
                                && (other.getVersion() == version);
                    }
                });

        if (filtered.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Workflow definition was not found. workflow id = [%s], version = [%d]",
                            workflowId, version));
        }
        return filtered.get(0);
    }

    /**
     * ワークフローの定義情報をロードするクラスを設定する。
     *
     * @param workflowDefinitionLoader ワークフローの定義情報をロードするクラス。
     */
    public void setWorkflowDefinitionLoader(WorkflowDefinitionLoader workflowDefinitionLoader) {
        this.workflowDefinitionLoader = workflowDefinitionLoader;
    }

    /**
     * 有効期間ないかを判断するための基準日を取得する{@link nablarch.core.date.SystemTimeProvider}を設定する。
     *
     * @param systemTimeProvider システムタイム
     */
    public void setSystemTimeProvider(SystemTimeProvider systemTimeProvider) {
        this.systemTimeProvider = systemTimeProvider;
    }

    /**
     * 初期化処理を行う。
     */
    @Override
    public void initialize() {
        workflowDefinitions = workflowDefinitionLoader.load();
    }

    /**
     * ワークフロー定義を追加する。
     *
     * @param workflowDefinition ワークフロー定義
     */
    void addWorkflow(WorkflowDefinition workflowDefinition) {
        workflowDefinitions.add(workflowDefinition);
    }

    /**
     * 適用期間判断のための基準日を取得する。
     *
     * @return 基準日
     */
    protected String getReferenceDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        return dateFormat.format(systemTimeProvider.getDate());
    }
}

