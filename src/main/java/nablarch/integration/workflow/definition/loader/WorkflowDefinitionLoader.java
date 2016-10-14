package nablarch.integration.workflow.definition.loader;

import java.util.List;

import nablarch.integration.workflow.definition.WorkflowDefinition;

/**
 * ワークフロー定義情報のロード処理を行うインタフェース。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public interface WorkflowDefinitionLoader {

    /**
     * ワークフロー定義をロードする。
     *
     * @return ワークフロー定義
     */
    List<WorkflowDefinition> load();
}
