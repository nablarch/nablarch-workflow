package nablarch.integration.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ワークフローインスタンスをあらわすインタフェース。
 *
 * @author Ryo Tanaka
 * @since 1.4.2
 */
public interface WorkflowInstance {

    /**
     * アクティブユーザタスクを完了させた後、ワークフロー定義に従ってワークフローを進行させ、ワークフローインスタンスのアクティブフローノードを次のタスク
     * もしくは停止イベントに進行させる。
     *
     * @throws IllegalStateException 終了させる対象のアクティブユーザタスクが見つからない場合。
     */
    void completeUserTask() throws IllegalStateException;

    /**
     * アクティブユーザタスクを完了させた後、ワークフロー定義に従ってワークフローを進行させ、ワークフローインスタンスのアクティブフローノードを次のタスク
     * もしくは停止イベントに進行させる。
     *
     * @param assigned タスクを完了させるユーザ
     * @throws IllegalStateException {@code assigned} に対してアクティブユーザタスクが見つからない場合。
     */
    void completeUserTask(String assigned) throws IllegalStateException;

    /**
     * アクティブユーザタスクを完了させた後、ワークフロー定義に従ってワークフローを進行させ、ワークフローインスタンスのアクティブフローノードを次のタスク
     * もしくは停止イベントに進行させる。
     *
     * @param parameter ワークフローを進行させる際に、各フローノードで使用するパラメータ
     * @throws IllegalStateException 終了させる対象のアクティブユーザタスクが見つからない場合。
     */
    void completeUserTask(Map<String, ?> parameter) throws IllegalStateException;

    /**
     * アクティブユーザタスクを完了させた後、ワークフロー定義に従ってワークフローを進行させ、ワークフローインスタンスのアクティブフローノードを次のタスク
     * もしくは停止イベントに進行させる。
     *
     * @param parameter ワークフローを進行させる際に、各フローノードで使用するパラメータ
     * @param assigned タスクを完了させるユーザ
     * @throws IllegalStateException {@code assigned} に対してアクティブユーザタスクが見つからない場合、またはワークフローが既に完了している場合。
     */
    void completeUserTask(Map<String, ?> parameter, String assigned) throws IllegalStateException;

    /**
     * アクティブグループタスクを完了させた後、ワークフロー定義に従ってワークフローを進行させ、ワークフローインスタンスのアクティブフローノードを次のタスク
     * もしくは停止イベントに進行させる。
     *
     * @param assigned タスクを完了させるグループ
     * @throws IllegalStateException {@code assigned} に対してアクティブグループタスクが見つからない場合、またはワークフローが既に完了している場合。
     */
    void completeGroupTask(String assigned) throws IllegalStateException;

    /**
     * アクティブグループタスクを完了させた後、ワークフロー定義に従ってワークフローを進行させ、ワークフローインスタンスのアクティブフローノードを次のタスク
     * もしくは停止イベントに進行させる。
     *
     * @param parameter ワークフローを進行させる際に、各フローノードで使用するパラメータ
     * @param assigned タスクを完了させるグループ
     * @throws IllegalStateException {@code assigned} に対してアクティブグループタスクが見つからない場合、またはワークフローが既に完了している場合。
     */
    void completeGroupTask(Map<String, ?> parameter, String assigned) throws IllegalStateException;

    /**
     * アクティブフローノードから、境界イベントトリガーIDに対応する境界イベントを取得し、現在のタスクを中断して、境界イベントから取得される進行先フローノードに
     * ワークフローを進行させる。
     *
     * @param eventTriggerId 境界イベントトリガーID
     * @throws IllegalStateException アクティブフローノードから、境界イベントトリガーIDに対応する境界イベントを取得できなかった場合。
     */
    void triggerEvent(String eventTriggerId) throws IllegalStateException;

    /**
     * アクティブフローノードから、境界イベントトリガーIDに対応する境界イベントを取得し、現在のタスクを中断して、境界イベントから取得される進行先フローノードに
     * ワークフローを進行させる。
     *
     * @param eventTriggerId 境界イベントトリガーID
     * @param parameter ワークフローを進行させる際に利用するパラメータ
     * @throws IllegalStateException アクティブフローノードから、境界イベントトリガーIDに対応する境界イベントを取得できなかった場合。
     */
    void triggerEvent(String eventTriggerId, Map<String, ?> parameter) throws IllegalStateException;

    /**
     * タスクに担当ユーザを割り当てる。
     *
     * @param taskId 担当ユーザを割り当てる対象のタスク
     * @param user 担当ユーザ
     * @throws IllegalStateException ワークフローがすでに完了している場合
     * @throws IllegalArgumentException 指定されたタスクが存在しない場合
     */
    void assignUser(String taskId, String user) throws IllegalStateException, IllegalArgumentException;

    /**
     * タスクに担当ユーザを割り当てる。マルチインスタンスでないタスクに対しては、複数ユーザを割り当てることは出来ない。
     * <p/>
     * マルチインスタンスタスクで、シーケンシャルタイプの場合は、指定された担当ユーザの順序がタスクの実行順となる。
     * すでにタスクに担当ユーザや担当グループが割り当てられている場合、それらの情報はクリアされ、今回設定した担当ユーザのみが有効となる。
     *
     * @param taskId タスクID
     * @param users 担当ユーザリスト
     * @throws IllegalStateException ワークフローがすでに完了している場合
     * @throws IllegalArgumentException 指定されたタスクが存在しない場合、もしくはマルチインスタンスでないタスクに複数ユーザを割り当てようとした場合。
     */
    void assignUsers(String taskId, List<String> users) throws IllegalStateException, IllegalArgumentException;

    /**
     * タスクに担当グループを割り当てる。
     * <p/>
     * すでにタスクに担当ユーザや担当グループが割り当てられている場合、それらの情報はクリアされ、今回設定した担当グループ情報のみが有効となる。
     *
     * @param taskId タスクのフローノードID
     * @param group 担当グループ
     * @throws IllegalStateException ワークフローがすでに完了している場合
     * @throws IllegalArgumentException 指定されたタスクが存在しない場合
     */
    void assignGroup(String taskId, String group) throws IllegalStateException, IllegalArgumentException;

    /**
     * タスクに担当グループを割り当てる。マルチインスタンスでないタスクに対しては、複数グループを割り当てることは出来ない。
     * <p/>
     * マルチインスタンスタスクで、シーケンシャルタイプの場合は、指定された担当グループの順序がタスクの実行順となる。
     * すでにタスクに担当ユーザや担当グループが割り当てられている場合、それらの情報はクリアされ、今回設定した担当ユーザのみが有効となる。
     *
     * @param taskId タスクのフローノードID
     * @param groups 担当グループ
     * @throws IllegalStateException ワークフローがすでに完了している場合
     * @throws IllegalArgumentException 指定されたタスクが存在しない場合、もしくはマルチインスタンスでないタスクに複数グループを割り当てようとした場合。
     */
    void assignGroups(String taskId, List<String> groups) throws IllegalStateException, IllegalArgumentException;

    /**
     * レーンIDで指定されたレーンに属するすべてのタスクに、指定された担当ユーザを割り当てる。
     *
     * @param laneId 担当ユーザを割り当てるタスクが属するレーンのレーンID
     * @param user 担当ユーザ
     * @throws IllegalStateException ワークフローがすでに完了している場合
     * @throws IllegalArgumentException マルチインスタンスでないタスクに複数ユーザを割り当てようとした場合。
     */
    void assignUserToLane(String laneId, String user) throws IllegalStateException, IllegalArgumentException;

    /**
     * レーンIDで指定されたレーンに属するすべてのタスクに、指定された担当ユーザを割り当てる。
     *
     * @param laneId 担当ユーザを割り当てるタスクが属するレーンのレーンID
     * @param users 担当ユーザリスト
     * @throws IllegalStateException ワークフローがすでに完了している場合
     * @throws IllegalArgumentException マルチインスタンスでないタスクに複数ユーザを割り当てようとした場合。
     */
    void assignUsersToLane(String laneId, List<String> users) throws IllegalStateException, IllegalArgumentException;

    /**
     * レーンIDで指定されたレーンに属するすべてのタスクに、指定された担当グループを割り当てる。
     *
     * @param laneId 担当グループを割り当てるタスクが属するレーンのレーンID
     * @param group 担当グループ
     * @throws IllegalStateException ワークフローがすでに完了している場合
     * @throws IllegalArgumentException マルチインスタンスでないタスクに複数グループを割り当てようとした場合。
     */
    void assignGroupToLane(String laneId, String group) throws IllegalStateException, IllegalArgumentException;

    /**
     * レーンIDで指定されたレーンに属するすべてのタスクに、指定された担当グループを割り当てる。
     *
     * @param laneId 担当ユーザを割り当てるタスクが属するレーンのレーンID
     * @param groups 担当ユーザ
     * @throws IllegalStateException ワークフローがすでに完了している場合
     * @throws IllegalArgumentException マルチインスタンスでないタスクに複数グループを割り当てようとした場合。
     */
    void assignGroupsToLane(String laneId, List<String> groups) throws IllegalStateException, IllegalArgumentException;

    /**
     * タスクに現在アサインされている担当ユーザを、別の担当ユーザに振り替える。
     * <p/>
     * 指定されたタスクがアクティブタスクの場合は、アクティブユーザタスクについても振り替えを行う。
     *
     * @param taskId 担当ユーザを振り替えるタスク
     * @param oldUser 振替元の担当ユーザ
     * @param newUser 振替先の担当ユーザ
     * @throws IllegalArgumentException 指定されたタスクがワークフロー定義に存在しない場合
     * @throws IllegalStateException 指定されたタスクに、振替元の担当ユーザがアサインされていない場合
     */
    void changeAssignedUser(String taskId, String oldUser, String newUser) throws IllegalArgumentException, IllegalStateException;

    /**
     * タスクに現在アサインされている担当グループを、別の担当グループに振り替える。
     * <p/>
     * 指定されたタスクがアクティブである場合は、アクティブグループタスクについても振り替えを行う。
     *
     * @param taskId 担当ユーザを振り替えるタスク
     * @param oldGroup 振替元の担当グループ
     * @param newGroup 振替先の担当グループ
     * @throws IllegalArgumentException 指定されたタスクがワークフロー定義に存在しない場合
     * @throws IllegalStateException 指定されたタスクに、振替元の担当グループがアサインされていない場合
     */
    void changeAssignedGroup(String taskId, String oldGroup, String newGroup) throws IllegalArgumentException, IllegalStateException;

    /**
     * タスクに割り当てられた担当ユーザを取得する。
     * <p/>
     * 担当ユーザは、実行順でソートされて返却される。
     *
     * @param taskId タスクID
     * @return 担当ユーザ
     * @throws IllegalArgumentException 指定されたタスクがワークフロー定義に存在しない場合
     */
    List<String> getAssignedUsers(String taskId) throws IllegalArgumentException;

    /**
     * タスクに割り当てられた担当グループを取得する。
     * <p/>
     * 担当グループは、実行順でソートされて返却される。
     *
     * @param taskId タスクID
     * @return 担当グループ
     * @throws IllegalArgumentException 指定されたタスクがワークフロー定義に存在しない場合
     */
    List<String> getAssignedGroups(String taskId) throws IllegalArgumentException;

    /**
     * 指定されたユーザのアクティブユーザタスクが存在するかどうかを確認する。
     *
     * @param user ユーザ
     * @return ユーザのアクティブユーザタスクが存在する場合は {@code true}
     */
    boolean hasActiveUserTask(String user);

    /**
     * 指定されたグループのアクティブグループタスクが存在するかどうかを確認する。
     *
     * @param group グループ
     * @return グループのアクティブグループタスクが存在する場合は {@code true}
     */
    boolean hasActiveGroupTask(String group);

    /**
     * 現在のワークフローインスタンスのインスタンスIDを取得する。
     *
     * @return ワークフローインスタンスのインスタンスID
     */
    String getInstanceId();

    /**
     * ワークフローIDを取得する。
     *
     * @return ワークフローID
     */
    String getWorkflowId();

    /**
     * バージョン番号を取得する。
     *
     * @return バージョン番号
     */
    long getVersion();

    /**
     * 指定されたフローノードIDがアクティブな状態かどうかを判定する。
     *
     * @param flowNodeId フローノードID
     * @return アクティブな場合はtrue
     */
    boolean isActive(String flowNodeId);

    /**
     * ワークフローが完了状態かどうかを判定する。
     *
     * @return ワークフローが完了している場合は {@code true}
     */
    boolean isCompleted();

    /**
     * 完了状態のワークフローインスタンス。
     * <p/>
     * 本クラスでは、 {@link WorkflowInstance} が、 {@link WorkflowInstance#completeUserTask(Map, String)} や
     * {@link WorkflowInstance#completeGroupTask(Map, String)} で完了状態になった場合と同様に振舞うワークフローインスタンスを実装している。
     * <p/>
     * すなわち、 {@link #isCompleted()} は常に {@code true} を返却し、 {@link #isActive(String)} は常に {@code false} を返却する。
     * また、完了状態のワークフローインスタンスに対して許されない操作については、 {@link IllegalStateException} を送出する。
     *
     * ただし、 {@link #getWorkflowId()}, {@link #getVersion()} については、取得することができないため、 {@link UnsupportedOperationException} を送出する。
     *
     * @author Ryo Tanaka
     * @since 1.4.2
     */
    final class CompletedWorkflowInstance extends WorkflowInstanceSupport {

        /**
         * ワークフローインスタンスID
         */
        private final String instanceId;

        /**
         * {@inheritDoc}
         * <p/>
         * 完了状態のワークフローインスタンスには、アクティブフローノードは存在しないため、常に {@code false} を返却する。
         */
        @Override
        public boolean isActive(String flowNodeId) {
            return false;
        }

        /**
         * {@inheritDoc}
         * <p/>
         * 完了状態のワークフローインスタンスでは、常に {@code true} を返却する。
         */
        @Override
        public boolean isCompleted() {
            return true;
        }

        /**
         * {@inheritDoc}
         * <p/>
         * 完了状態のワークフローインスタンスには、アクティブユーザタスクは存在しないため、常に {@code false} を返却する。
         */
        @Override
        public boolean hasActiveUserTask(String user) {
            return false;
        }

        /**
         * {@inheritDoc}
         * <p/>
         * 完了状態のワークフローインスタンスには、アクティブグループタスクは存在しないため、常に {@code false} を返却する。
         */
        @Override
        public boolean hasActiveGroupTask(String group) {
            return false;
        }

        /**
         * {@inheritDoc}
         * <p/>
         * 完了状態のワークフローインスタンスでは、対象タスクに割り当てられていた担当ユーザを取得できないため、常に空のリストを返却する。
         */
        @Override
        public List<String> getAssignedUsers(String taskId) throws IllegalArgumentException {
            return new ArrayList<String>(0);
        }

        /**
         * {@inheritDoc}
         * <p/>
         * 完了状態のワークフローインスタンスでは、対象タスクに割り当てられていた担当グループを取得できないため、常に空のリストを返却する。
         */
        @Override
        public List<String> getAssignedGroups(String taskId) throws IllegalArgumentException {
            return new ArrayList<String>(0);
        }

        /**
         * インスタンスIDを指定して、完了状態のワークフローインスタンスを生成する。
         *
         * @param instanceId インスタンスID
         */
        public CompletedWorkflowInstance(String instanceId) {
            this.instanceId = instanceId;
        }

        @Override
        public String getInstanceId() {
            return instanceId;
        }

        /**
         * {@inheritDoc}
         * <p/>
         * 完了状態のワークフローインスタンスでは、ワークフロー定義のワークフローIDは取得することができないため、 {@link UnsupportedOperationException} を送出する。
         */
        @Override
        public String getWorkflowId() {
            throw new UnsupportedOperationException("Workflow is already completed. " + this);
        }

        /**
         * {@inheritDoc}
         * <p/>
         * 完了状態のワークフローインスタンスでは、ワークフロー定義のバージョンは取得することができないため、 {@link UnsupportedOperationException} を送出する。
         */
        @Override
        public long getVersion() {
            throw new UnsupportedOperationException("Workflow is already completed. " + this);
        }

        /**
         * {@inheritDoc}
         * <p/>
         * 完了状態のワークフローインスタンスに対して、タスクを進行することは出来ないため、 {@link IllegalStateException} を送出する。
         */
        @Override
        public void completeUserTask(Map<String, ?> parameter, String assigned) throws IllegalStateException {
            throw new IllegalStateException("Workflow is already completed. " + this);
        }

        /**
         * {@inheritDoc}
         * <p/>
         * 完了状態のワークフローインスタンスに対して、タスクを進行することは出来ないため、 {@link IllegalStateException} を送出する。
         */
        @Override
        public void completeGroupTask(Map<String, ?> parameter, String assigned) throws IllegalStateException {
            throw new IllegalStateException("Workflow is already completed. " + this);
        }

        /**
         * {@inheritDoc}
         * <p/>
         * 完了状態のワークフローインスタンスに対して、境界イベントを発生させることは出来ないため、 {@link IllegalStateException} を送出する。
         */
        @Override
        public void triggerEvent(String eventTriggerId, Map<String, ?> parameter) throws IllegalStateException {
            throw new IllegalStateException("Workflow is already completed. " + this);
        }

        /**
         * {@inheritDoc}
         * <p/>
         * 完了状態のワークフローインスタンスに対して、担当ユーザを割り当てることは出来ないため、 {@link IllegalStateException} を送出する。
         */
        @Override
        public void assignUsers(String taskId, List<String> users) throws IllegalStateException, IllegalArgumentException {
            throw new IllegalStateException("Workflow is already completed. " + this);
        }

        /**
         * {@inheritDoc}
         * <p/>
         * 完了状態のワークフローインスタンスに対して、担当グループを割り当てることは出来ないため、 {@link IllegalStateException} を送出する。
         */
        @Override
        public void assignGroups(String taskId, List<String> groups) throws IllegalStateException, IllegalArgumentException {
            throw new IllegalStateException("Workflow is already completed. " + this);
        }

        /**
         * {@inheritDoc}
         * <p/>
         * 完了状態のワークフローインスタンスに対して、担当ユーザを割り当てることは出来ないため、 {@link IllegalStateException} を送出する。
         */
        @Override
        public void assignUsersToLane(String laneId, List<String> users) throws IllegalStateException, IllegalArgumentException {
            throw new IllegalStateException("Workflow is already completed. " + this);
        }

        /**
         * {@inheritDoc}
         * <p/>
         * 完了状態のワークフローインスタンスに対して、担当グループを割り当てることは出来ないため、 {@link IllegalStateException} を送出する。
         */
        @Override
        public void assignGroupsToLane(String laneId, List<String> groups) throws IllegalStateException, IllegalArgumentException {
            throw new IllegalStateException("Workflow is already completed. " + this);
        }

        /**
         * {@inheritDoc}
         * <p/>
         * 完了状態のワークフローインスタンスに対して、担当ユーザを振替えることは出来ないため、 {@link IllegalStateException} を送出する。
         */
        @Override
        public void changeAssignedUser(String taskId, String oldUser, String newUser) throws IllegalArgumentException, IllegalStateException {
            throw new IllegalStateException("Workflow is already completed. " + this);
        }

        /**
         * {@inheritDoc}
         * <p/>
         * 完了状態のワークフローインスタンスに対して、担当グループを振替えることは出来ないため、 {@link IllegalStateException} を送出する。
         */
        @Override
        public void changeAssignedGroup(String taskId, String oldGroup, String newGroup) throws IllegalArgumentException, IllegalStateException {
            throw new IllegalStateException("Workflow is already completed. " + this);
        }

        @Override
        public String toString() {
            return "instance id = [" + getInstanceId() + "]";
        }
    }
}
