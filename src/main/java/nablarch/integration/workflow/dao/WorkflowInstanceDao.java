package nablarch.integration.workflow.dao;

import java.util.List;

import nablarch.common.idgenerator.IdGenerator;
import nablarch.core.repository.initialization.Initializable;
import nablarch.core.util.StringUtil;

import nablarch.integration.workflow.definition.FlowNode;
import nablarch.integration.workflow.definition.Task;

/**
 * ワークフローのインスタンス状態保持DBへアクセスするクラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class WorkflowInstanceDao implements Initializable {

    /** インスタンステーブル定義 */
    private WorkflowInstanceSchema workflowInstanceSchema;

    /** インスタンスIDを採番するジェネレーター */
    private IdGenerator instanceIdGenerator;

    /** インスタンスIDの桁数(デフォルト10桁) */
    private int instanceIdLength = 10;

    /** インスタンスIDを採番するために使用する採番対象ID */
    private String instanceIdGenerateId;

    /** インスタンステーブルアクセス */
    private InstanceDao instanceDao;

    /** インスタンスフローノードテーブルアクセス */
    private InstanceFlowNodeDao instanceFlowNodeDao;

    /** 担当ユーザテーブルアクセス */
    private TaskAssignedUserDao taskAssignedUserDao;

    /** 担当グループテーブルアクセス */
    private TaskAssignedGroupDao taskAssignedGroupDao;

    /** アクティブフローノードテーブルアクセス */
    private ActiveFlowNodeDao activeFlowNodeDao;

    /** アクティブユーザタスク */
    private ActiveUserTaskDao activeUserTaskDao;

    /** アクティブグループタスク */
    private ActiveGroupTaskDao activeGroupTaskDao;

    /**
     * ワークフローインスタンの進行状態を登録する。
     *
     * @param workflowId ワークフローID
     * @param version バージョン
     * @param tasks タスクリスト
     * @return ワークフローインスタンスID
     */
    public String createWorkflowInstance(String workflowId, int version, List<Task> tasks) {
        String instanceId = StringUtil.lpad(
                instanceIdGenerator.generateId(instanceIdGenerateId), instanceIdLength, '0');
        instanceDao.insert(instanceId, workflowId, version);
        instanceFlowNodeDao.insert(instanceId, workflowId, version, tasks);
        return instanceId;
    }

    /**
     * 担当者を登録する。
     * <p/>
     * 担当者情報がすでに登録されていた場合は、洗い替えを行う。
     * また、同一フローノードに割り当てられたグループ情報の削除処理も行う。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param users ユーザ情報
     */
    public void saveAssignedUser(String instanceId, String flowNodeId, List<String> users) {
        taskAssignedGroupDao.delete(instanceId, flowNodeId);
        taskAssignedUserDao.delete(instanceId, flowNodeId);
        taskAssignedUserDao.insert(instanceId, flowNodeId, users);
    }

    /**
     * 指定された順を実行順として担当者を登録する。
     * <p/>
     * 担当者情報がすでに登録されていた場合は、洗い替えを行う。
     * また、同一フローノードに割り当てられたグループ情報の削除処理も行う。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param users ユーザ情報
     */
    public void saveAssignedSequentialUser(String instanceId, String flowNodeId, List<String> users) {
        taskAssignedUserDao.delete(instanceId, flowNodeId);
        taskAssignedUserDao.insertSequential(instanceId, flowNodeId, users);
    }

    /**
     * 担当グループを登録する。
     * <p/>
     * グループ情報がすでに登録されていた場合は、洗い替えを行う。
     * また、同一フローノードに割り当てられた担当者情報の削除処理も行う。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param group グループ
     */
    public void saveAssignedGroup(String instanceId, String flowNodeId, List<String> group) {
        taskAssignedUserDao.delete(instanceId, flowNodeId);
        taskAssignedGroupDao.delete(instanceId, flowNodeId);
        taskAssignedGroupDao.insert(instanceId, flowNodeId, group);
    }

    /**
     * 指定された順を実行順として担当グループを登録する。
     * <p/>
     * グループ情報がすでに登録されていた場合は、洗い替えを行う。
     * また、同一フローノードに割り当てられた担当者情報の削除処理も行う。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param groups グループ情報
     */
    public void saveAssignedSequentialGroup(String instanceId, String flowNodeId, List<String> groups) {
        taskAssignedUserDao.delete(instanceId, flowNodeId);
        taskAssignedGroupDao.delete(instanceId, flowNodeId);
        taskAssignedGroupDao.insertSequential(instanceId, flowNodeId, groups);
    }

    /**
     * アクティブなフローノードの状態を登録する。
     * <p/>
     * すでにインスタンスIDに対応するアクティブなノードが登録されていた場合には、
     * その情報を削除後に登録を行う。
     *
     * @param instanceId インスタンスID
     * @param flowNode アクティブなフローノード
     */
    public void saveActiveFlowNode(String instanceId, FlowNode flowNode) {
        activeUserTaskDao.delete(instanceId);
        activeGroupTaskDao.delete(instanceId);
        activeFlowNodeDao.delete(instanceId);
        activeFlowNodeDao.insert(instanceId, flowNode);
    }

    /**
     * アクティブユーザタスクテーブルにユーザ情報を登録する。
     * <p/>
     * インスタンスIDに紐づくデータが既に登録されていた場合には、そのデータを削除後に登録処理を行う。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param users 登録対象のユーザ情報
     */
    public void saveActiveUserTask(String instanceId, String flowNodeId, List<String> users) {
        activeUserTaskDao.delete(instanceId);
        activeUserTaskDao.insert(instanceId, flowNodeId, users);
    }

    /**
     * アクティブユーザタスクテーブルからデータを削除する。
     * <p/>
     * 削除条件は以下のとおり
     * <ul>
     * <li>インスタンスID</li>
     * <li>フローノードID</li>
     * <li>ユーザ</li>
     * </ul>
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param user ユーザ
     */
    public void deleteActiveUserTaskByUserId(String instanceId, String flowNodeId, String user) {
        activeUserTaskDao.delete(instanceId, flowNodeId, user);
    }

    /**
     * アクティブユーザタスクテーブルにユーザ情報を登録する。
     * <p/>
     * インスタンスIDに紐づくデータが既に登録されていた場合には、そのデータを削除後に登録処理を行う。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param user 登録対象のユーザ情報
     * @param executionOrder 実行順
     */
    public void saveActiveUserTask(String instanceId, String flowNodeId, String user, int executionOrder) {
        activeUserTaskDao.delete(instanceId);
        activeUserTaskDao.insert(instanceId, flowNodeId, user, executionOrder);
    }

    /**
     * アクティブグループタスクテーブルにグループ情報を登録する。
     * <p/>
     * インスタンスIDに紐づくデータが既に登録されていた場合には、そのデータを削除後に登録処理を行う。
     *
     * @param instanceId インスタンスId
     * @param flowNodeId フローノードID
     * @param groups グループ情報
     */
    public void saveActiveGroupTask(String instanceId, String flowNodeId, List<String> groups) {
        activeGroupTaskDao.delete(instanceId);
        activeGroupTaskDao.insert(instanceId, flowNodeId, groups);
    }

    /**
     * アクティブグループタスクテーブルにグループ情報を登録する。
     * <p/>
     * インスタンスIDに紐づくデータが既に登録されていた場合には、そのデータを削除後に登録処理を行う。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param group 登録対象のグループ情報
     * @param executionOrder 実行順
     */
    public void saveActiveGroupTask(String instanceId, String flowNodeId, String group, int executionOrder) {
        activeGroupTaskDao.delete(instanceId);
        activeGroupTaskDao.insert(instanceId, flowNodeId, group, executionOrder);
    }

    /**
     * アクティブグループタスクテーブルから引数で指定された条件に紐づくグループ情報を削除する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param groupId グループID
     */
    public void deleteActiveGroupTaskByGroupId(String instanceId, String flowNodeId, String groupId) {
        activeGroupTaskDao.delete(instanceId, flowNodeId, groupId);
    }

    /**
     * タスク担当ユーザを別のユーザに変更する。
     * <p/>
     * 変更対象のタスク担当ユーザが存在しない場合は、本処理は何もしない。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノード(タスク)ID
     * @param oldUser 元グループ
     * @param newUser 新しいグループ
     */
    public void changeAssignedUser(String instanceId, String flowNodeId, String oldUser, String newUser) {
        TaskAssignedUserEntity oldUserInfo = taskAssignedUserDao.find(instanceId, flowNodeId, oldUser);
        if (oldUserInfo == null) {
            return;
        }
        taskAssignedUserDao.delete(instanceId, flowNodeId, oldUser);
        taskAssignedUserDao.insert(instanceId, flowNodeId, newUser, oldUserInfo.getExecutionOrder());
    }

    /**
     * アクティブユーザタスクの情報を別のユーザに変更する。
     * <p/>
     * 変更対象のアクティブユーザタスクが存在しない場合は、本処理は何もしない。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param oldUser 元ユーザ
     * @param newUser 新しいユーザ
     */
    public void changeActiveUser(String instanceId, String flowNodeId, String oldUser, String newUser) {
        ActiveUserTaskEntity oldUserInfo = activeUserTaskDao.find(instanceId, flowNodeId, oldUser);
        if (oldUserInfo == null) {
            return;
        }
        activeUserTaskDao.delete(instanceId, flowNodeId, oldUser);
        activeUserTaskDao.insert(instanceId, flowNodeId, newUser, oldUserInfo.getExecutionOrder());
    }

    /**
     * タスク担当グループを別のグループに変更する。
     * <p/>
     * 変更対象のタスク担当グループが存在しない場合は、本処理は何もしない。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノード(タスク)ID
     * @param oldGroup 元グループ
     * @param newGroup 新しいグループ
     */
    public void changeAssignedGroup(String instanceId, String flowNodeId, String oldGroup, String newGroup) {
        TaskAssignedGroupEntity oldGroupInfo = taskAssignedGroupDao.find(instanceId, flowNodeId, oldGroup);
        if (oldGroupInfo == null) {
            return;
        }
        taskAssignedGroupDao.delete(instanceId, flowNodeId, oldGroup);
        taskAssignedGroupDao.insert(instanceId, flowNodeId, newGroup, oldGroupInfo.getExecutionOrder());
    }

    /**
     * アクティブグループタスクを別のグループに変更する。
     * <p/>
     * 変更対象のアクティブグループタスクが存在しない場合は、本処理は何もしない。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノード(タスク)ID
     * @param oldGroup 元グループ
     * @param newGroup 新しいグループ
     */
    public void changeActiveGroup(String instanceId, String flowNodeId, String oldGroup, String newGroup) {
        ActiveGroupTaskEntity oldGroupInfo = activeGroupTaskDao.find(instanceId, flowNodeId, oldGroup);
        if (oldGroupInfo == null) {
            return;
        }
        // アクティブグループが存在する場合のみ更新
        activeGroupTaskDao.delete(instanceId, flowNodeId, oldGroup);
        activeGroupTaskDao.insert(instanceId, flowNodeId, newGroup, oldGroupInfo.getExecutionOrder());
    }


    /**
     * インスタンスIDに紐づくデータを全て削除する。
     *
     * @param instanceId インスタンスID
     */
    public void deleteInstance(String instanceId) {
        activeUserTaskDao.delete(instanceId);
        activeGroupTaskDao.delete(instanceId);
        activeFlowNodeDao.delete(instanceId);
        taskAssignedUserDao.delete(instanceId);
        taskAssignedGroupDao.delete(instanceId);
        instanceFlowNodeDao.delete(instanceId);
        instanceDao.delete(instanceId);
    }

    /**
     * インスタンスIDに紐づくインスタンス情報を取得する。
     *
     * @param instanceId インスタンスID
     * @return インスタンス情報
     */
    public WorkflowInstanceEntity findInstance(String instanceId) {
        return instanceDao.find(instanceId);
    }

    /**
     * インスタンスIDとフローノードIDに紐づく担当ユーザ情報を取得する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @return 担当ユーザ情報
     */
    public List<TaskAssignedUserEntity> findTaskAssignedUser(String instanceId, String flowNodeId) {
        return taskAssignedUserDao.find(instanceId, flowNodeId);
    }

    /**
     * インスタンスIDとフローノードIDに紐づく担当グループ情報を取得する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @return 担当グループ情報
     */
    public List<TaskAssignedGroupEntity> findTaskAssignedGroup(String instanceId, String flowNodeId) {
        return taskAssignedGroupDao.find(instanceId, flowNodeId);
    }

    /**
     * アクティブフローノードを取得する。
     *
     * @param instanceId インスタンスID
     * @return アクティブフローノード
     */
    public ActiveFlowNodeEntity findActiveFlowNode(String instanceId) {
        return activeFlowNodeDao.find(instanceId);
    }

    /**
     * アクティブユーザタスクを取得する。
     *
     * @param instanceId インスタンスID
     * @return アクティブユーザタスク情報
     */
    public List<ActiveUserTaskEntity> findActiveUserTask(String instanceId) {
        return activeUserTaskDao.find(instanceId);
    }

    /**
     * ユーザID指定でアクティブユーザタスクを取得する。
     * <p/>
     * 指定したユーザに対応するアクティブユーザタスクが存在しない場合は、nullを返却する。
     *
     * @param user ユーザ
     * @param flowNodeId フローノードID
     * @param instanceId インスタンスID
     * @return 取得結果(存在しない場合はnull)
     */
    public ActiveUserTaskEntity findActiveUserTaskByPk(String user, String flowNodeId, String instanceId) {
        return activeUserTaskDao.find(instanceId, flowNodeId, user);
    }

    /**
     * インスタンスIDに紐づくアクティブグループタスクを取得する。
     *
     * @param instanceId インスタンスID
     * @return アクティブグループタスク情報
     */
    public List<ActiveGroupTaskEntity> findActiveGroupTask(String instanceId) {
        return activeGroupTaskDao.find(instanceId);
    }

    /**
     * グループID指定でアクティブグループタスクを取得する。
     * <p/>
     * 指定したグループに対応するアクティブグループタスクが存在しない場合は、nullを返却する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @param group グループ
     * @return 取得結果(存在しない場合はnull)
     */
    public ActiveGroupTaskEntity findActiveGroupTaskByPk(String instanceId, String flowNodeId, String group) {
        return activeGroupTaskDao.find(instanceId, flowNodeId, group);
    }

    /**
     * アクティブユーザタスク数を取得する。
     *
     * @param instanceId インスタンスID
     * @return アクティブなユーザタスク数
     */
    public int getActiveUserTaskCount(String instanceId) {
        return activeUserTaskDao.count(instanceId);
    }

    /**
     * 担当ユーザを指定して、アクティブユーザタスク数を取得する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノード
     * @param user 担当ユーザ
     * @return アクティブなユーザタスク数
     */
    public int getActiveUserTaskCountByPk(String instanceId, String flowNodeId, String user) {
        return activeUserTaskDao.countByPk(instanceId, flowNodeId, user);
    }

    /**
     * タスク担当ユーザ数を取得する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @return タスクに割り当てされたユーザ数
     */
    public int getTaskAssignedUserCount(String instanceId, String flowNodeId) {
        return taskAssignedUserDao.count(instanceId, flowNodeId);
    }

    /**
     * アクティブグループタスク数を取得する。
     *
     * @param instanceId インスタンスID
     * @return アクティブなグループタスク数
     */
    public int getActiveGroupTaskCount(String instanceId) {
        return activeGroupTaskDao.count(instanceId);
    }

    /**
     * 担当グループを指定して、アクティブグループタスク数を取得する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノード
     * @param group 担当グループ
     * @return アクティブなグループタスク数
     */
    public int getActiveGroupTaskCountByPk(String instanceId, String flowNodeId, String group) {
        return activeGroupTaskDao.countByPk(instanceId, flowNodeId, group);
    }
    /**
     * タスク担当グループ数を取得する。
     *
     * @param instanceId インスタンスID
     * @param flowNodeId フローノードID
     * @return タスクに割り当てられたグループ数
     */
    public int getTaskAssignedGroupCount(String instanceId, String flowNodeId) {
        return taskAssignedGroupDao.count(instanceId, flowNodeId);
    }

    /**
     * ワークフローインスタンステーブルの定義情報を設定する。
     *
     * @param workflowInstanceSchema ワークフローインスタンステーブルの定義情報
     */
    public void setWorkflowInstanceSchema(WorkflowInstanceSchema workflowInstanceSchema) {
        this.workflowInstanceSchema = workflowInstanceSchema;
    }

    /**
     * インスタンスIDを採番するジェネレーターを設定する。
     *
     * @param instanceIdGenerator インスタンスIDを採番するジェネレーター
     */
    public void setInstanceIdGenerator(IdGenerator instanceIdGenerator) {
        this.instanceIdGenerator = instanceIdGenerator;
    }

    /**
     * インスタンスIDを採番する際に使用する採番対象IDを設定する。
     *
     * @param instanceIdGenerateId インスタンスIDを採番する際に使用する採番対象ID
     * @see {@link IdGenerator}
     */
    public void setInstanceIdGenerateId(String instanceIdGenerateId) {
        this.instanceIdGenerateId = instanceIdGenerateId;
    }

    /**
     * インスタンスIDの桁数を設定する。
     * <p/>
     * インスタンスIDは、指定された桁数となるよう先頭に"0"を付加する。
     * 設定を省略した場合は、10桁のインスタンスIDが採番される。
     *
     * @param instanceIdLength インスタンスIDの桁数
     */
    public void setInstanceIdLength(int instanceIdLength) {
        this.instanceIdLength = instanceIdLength;
    }

    /**
     * 初期化処理を行う。
     */
    @Override
    public void initialize() {
        instanceDao = new InstanceDao(workflowInstanceSchema);
        instanceFlowNodeDao = new InstanceFlowNodeDao(workflowInstanceSchema);
        taskAssignedUserDao = new TaskAssignedUserDao(workflowInstanceSchema);
        taskAssignedGroupDao = new TaskAssignedGroupDao(workflowInstanceSchema);
        activeFlowNodeDao = new ActiveFlowNodeDao(workflowInstanceSchema);
        activeUserTaskDao = new ActiveUserTaskDao(workflowInstanceSchema);
        activeGroupTaskDao = new ActiveGroupTaskDao(workflowInstanceSchema);
    }

}



