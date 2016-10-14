package nablarch.integration.workflow.testhelper;

import nablarch.core.db.connection.AppDbConnection;
import nablarch.core.db.statement.SqlPStatement;
import nablarch.core.db.statement.SqlResultSet;
import nablarch.core.db.transaction.SimpleDbTransactionExecutor;
import nablarch.core.db.transaction.SimpleDbTransactionManager;

import nablarch.integration.workflow.testhelper.entity.BoundaryEventEntity;
import nablarch.integration.workflow.testhelper.entity.BoundaryEventTriggerEntity;
import nablarch.integration.workflow.testhelper.entity.FlowNodeEntity;
import nablarch.integration.workflow.testhelper.entity.GatewayEntity;
import nablarch.integration.workflow.testhelper.entity.LaneEntity;
import nablarch.integration.workflow.testhelper.entity.WorkflowEntity;
import nablarch.integration.workflow.testhelper.entity.EventEntity;
import nablarch.integration.workflow.testhelper.entity.SequenceFlowEntity;
import nablarch.integration.workflow.testhelper.entity.TaskEntity;

/**
 * ワークフロー関連のテーブルにアクセスするクラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class WorkflowDbAccessSupport {

    /** トランザクションマネージャ */
    private final SimpleDbTransactionManager transactionManager;

    /**
     * ワークフローDAOを生成する。
     *
     * @param transactionManager トランザクションマネージャ
     */
    public WorkflowDbAccessSupport(SimpleDbTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * ワークフロー関連の全テーブルをクリーニングする。
     */
    public void cleanupAll() {
        cleanup(
                "WF_ACTIVE_GROUP_TASK",
                "WF_ACTIVE_USER_TASK",
                "WF_ACTIVE_FLOW_NODE",
                "WF_TASK_ASSIGNED_GROUP",
                "WF_TASK_ASSIGNED_USER",
                "WF_INSTANCE_FLOW_NODE",
                "WF_INSTANCE",
                "WF_SEQUENCE_FLOW",
                "WF_BOUNDARY_EVENT",
                "WF_BOUNDARY_EVENT_TRIGGER",
                "WF_GATEWAY",
                "WF_EVENT",
                "WF_TASK",
                "WF_FLOW_NODE",
                "WF_LANE",
                "WF_WORKFLOW_DEFINITION"
        );
    }

    /**
     * 指定されたテーブルのデータを削除する。
     *
     * @param tableName テーブル名
     */
    public void cleanup(final String... tableName) {
        new SimpleDbTransactionExecutor<Void>(transactionManager) {
            @Override
            public Void execute(AppDbConnection connection) {
                for (String name : tableName) {
                    SqlPStatement statement = connection.prepareStatement("delete from " + name);
                    statement.executeUpdate();
                }
                return null;
            }
        }.doTransaction();
    }

    /**
     * ワークフローエンティティへデータを登録する。
     */
    public void insertWorkflowEntity(final WorkflowEntity... entities) {
        new SimpleDbTransactionExecutor<Void>(transactionManager) {
            @Override
            public Void execute(AppDbConnection connection) {
                SqlPStatement statement = connection.prepareStatement("INSERT INTO WF_WORKFLOW_DEFINITION "
                        + " (WORKFLOW_ID, DEF_VERSION, WORKFLOW_NAME, EFFECTIVE_DATE)"
                        + "VALUES (?, ?, ?, ?)");
                for (WorkflowEntity entity : entities) {
                    statement.setString(1, entity.processId);
                    statement.setLong(2, entity.version);
                    statement.setString(3, entity.processName);
                    statement.setString(4, entity.effectiveDate);
                    statement.addBatch();
                }
                statement.executeBatch();
                return null;
            }
        }.doTransaction();
    }

    /**
     * レーンエンティティへデータを登録する。
     */
    public void insertLaneEntity(final LaneEntity... entities) {
        new SimpleDbTransactionExecutor<Void>(transactionManager) {
            @Override
            public Void execute(AppDbConnection connection) {
                SqlPStatement statement = connection.prepareStatement("INSERT INTO WF_LANE "
                        + "(WORKFLOW_ID, DEF_VERSION, LANE_ID, LANE_NAME) VALUES (?, ?, ?, ?)");
                for (LaneEntity entity : entities) {
                    statement.setString(1, entity.workflowEntity.processId);
                    statement.setLong(2, entity.workflowEntity.version);
                    statement.setString(3, entity.laneId);
                    statement.setString(4, entity.name);
                    statement.addBatch();
                }
                statement.executeBatch();
                return null;
            }
        }.doTransaction();
    }

    /**
     * シーケンスエンティティへデータを登録する。
     */
    public void insertSequenceEntity(final SequenceFlowEntity... entities) {
        new SimpleDbTransactionExecutor<Void>(transactionManager) {
            @Override
            public Void execute(AppDbConnection connection) {
                SqlPStatement statement = connection.prepareStatement("INSERT INTO WF_SEQUENCE_FLOW "
                        + "(WORKFLOW_ID, DEF_VERSION, SEQUENCE_FLOW_ID, SOURCE_FLOW_NODE_ID, TARGET_FLOW_NODE_ID, SEQUENCE_FLOW_NAME, FLOW_PROCEED_CONDITION) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)");
                for (SequenceFlowEntity entity : entities) {
                    statement.setString(1, entity.workflowEntity.processId);
                    statement.setLong(2, entity.workflowEntity.version);
                    statement.setString(3, entity.sequenceId);
                    statement.setString(4, entity.sourceFlowNodeId);
                    statement.setString(5, entity.targetFlowNodeId);
                    statement.setString(6, entity.name);
                    statement.setString(7, entity.flowCondition);
                    statement.addBatch();
                }
                statement.executeBatch();
                return null;
            }
        }.doTransaction();
    }

    /**
     * フローノードエンティティへデータを登録する。
     */
    private void insertFlowNodeEntity(final FlowNodeEntity... entities) {
        new SimpleDbTransactionExecutor<Void>(transactionManager) {
            @Override
            public Void execute(AppDbConnection connection) {
                SqlPStatement statement = connection.prepareStatement("INSERT INTO WF_FLOW_NODE "
                        + "(WORKFLOW_ID, DEF_VERSION, FLOW_NODE_ID, LANE_ID, FLOW_NODE_NAME) "
                        + "VALUES (?, ?, ?, ?, ?)");
                for (FlowNodeEntity entity : entities) {
                    statement.setString(1, entity.laneEntity.workflowEntity.processId);
                    statement.setLong(2, entity.laneEntity.workflowEntity.version);
                    statement.setString(3, entity.flowNodeId);
                    statement.setString(4, entity.laneEntity.laneId);
                    statement.setString(5, entity.name);
                    statement.addBatch();
                }
                statement.executeBatch();
                return null;
            }
        }.doTransaction();
    }

    /**
     * イベントノードエンティティへデータを登録する。
     */
    public void insertEventEntity(final EventEntity... entities) {
        insertFlowNodeEntity(entities);
        new SimpleDbTransactionExecutor<Void>(transactionManager) {
            @Override
            public Void execute(AppDbConnection connection) {
                SqlPStatement statement = connection.prepareStatement("INSERT INTO WF_EVENT"
                        + "(WORKFLOW_ID, DEF_VERSION, FLOW_NODE_ID, EVENT_TYPE)"
                        + "VALUES (?, ?, ?, ?)");
                for (EventEntity entity : entities) {
                    statement.setString(1, entity.laneEntity.workflowEntity.processId);
                    statement.setLong(2, entity.laneEntity.workflowEntity.version);
                    statement.setString(3, entity.flowNodeId);
                    statement.setString(4, entity.eventType);
                    statement.addBatch();
                }
                statement.executeBatch();
                return null;
            }
        }.doTransaction();
    }

    /**
     * ゲートウェイエンティティへデータを登録する。
     */
    public void insertGatewayEntity(final GatewayEntity... entities) {
        insertFlowNodeEntity(entities);
        new SimpleDbTransactionExecutor<Void>(transactionManager) {
            @Override
            public Void execute(AppDbConnection connection) {
                SqlPStatement statement = connection.prepareStatement("INSERT INTO WF_GATEWAY "
                        + "(WORKFLOW_ID, DEF_VERSION, FLOW_NODE_ID, GATEWAY_TYPE)"
                        + "VALUES (?, ?, ?, ?)");
                for (GatewayEntity entity : entities) {
                    statement.setString(1, entity.laneEntity.workflowEntity.processId);
                    statement.setLong(2, entity.laneEntity.workflowEntity.version);
                    statement.setString(3, entity.flowNodeId);
                    statement.setString(4, entity.gatewayType);
                    statement.addBatch();
                }
                statement.executeBatch();
                return null;
            }
        }.doTransaction();
    }

    /**
     * アクティビティエンティティへデータを登録する。
     */
    public void insertTaskEntity(final TaskEntity... entities) {
        insertFlowNodeEntity(entities);
        new SimpleDbTransactionExecutor<Void>(transactionManager) {
            @Override
            public Void execute(AppDbConnection connection) {
                SqlPStatement statement = connection.prepareStatement("INSERT INTO WF_TASK "
                        + "(WORKFLOW_ID, DEF_VERSION, FLOW_NODE_ID, MULTI_INSTANCE_TYPE, COMPLETION_CONDITION)"
                        + "VALUES (?, ?, ?, ?, ?)");
                for (TaskEntity entity : entities) {
                    statement.setString(1, entity.laneEntity.workflowEntity.processId);
                    statement.setLong(2, entity.laneEntity.workflowEntity.version);
                    statement.setString(3, entity.flowNodeId);
                    statement.setString(4, entity.multiInstanceType);
                    statement.setString(5, entity.completionCondition);
                    statement.addBatch();
                }
                statement.executeBatch();
                return null;
            }
        }.doTransaction();
    }

    /**
     * イベントエンティティへデータを登録する。
     */
    public void insertBoundaryEventTriggerEntity(final BoundaryEventTriggerEntity... entities) {
        new SimpleDbTransactionExecutor<Void>(transactionManager) {
            @Override
            public Void execute(AppDbConnection connection) {
                SqlPStatement statement = connection.prepareStatement(
                        "INSERT INTO WF_BOUNDARY_EVENT_TRIGGER ("
                                + "WORKFLOW_ID, DEF_VERSION, BOUNDARY_EVENT_TRIGGER_ID, BOUNDARY_EVENT_TRIGGER_NAME)"
                                + " VALUES ( ?, ?, ?, ?)");
                for (BoundaryEventTriggerEntity entity : entities) {
                    statement.setString(1, entity.workflowEntity.processId);
                    statement.setLong(2, entity.workflowEntity.version);
                    statement.setString(3, entity.eventTriggerId);
                    statement.setString(4, entity.eventTriggerName);
                    statement.addBatch();
                }
                statement.executeBatch();
                return null;
            }
        }.doTransaction();
    }

    /**
     * バウンダリイベントエンティティへデータを登録する。
     */
    public void insertBoundaryEventEntity(final BoundaryEventEntity... entities) {
        insertFlowNodeEntity(entities);
        new SimpleDbTransactionExecutor<Void>(transactionManager) {
            @Override
            public Void execute(AppDbConnection connection) {
                SqlPStatement statement = connection.prepareStatement("INSERT INTO WF_BOUNDARY_EVENT ("
                        + " WORKFLOW_ID, DEF_VERSION, FLOW_NODE_ID, BOUNDARY_EVENT_TRIGGER_ID, ATTACHED_TASK_ID )"
                        + "VALUES ( ?, ?, ?, ?, ?)");

                for (BoundaryEventEntity entity : entities) {
                    statement.setString(1, entity.boundaryEventTriggerEntity.workflowEntity.processId);
                    statement.setLong(2, entity.boundaryEventTriggerEntity.workflowEntity.version);
                    statement.setString(3, entity.flowNodeId);
                    statement.setString(4, entity.boundaryEventTriggerEntity.eventTriggerId);
                    statement.setString(5, entity.taskEntity.flowNodeId);
                    statement.addBatch();
                }
                statement.executeBatch();
                return null;
            }
        }.doTransaction();
    }

    /**
     * ワークフローインスタンステーブルのデータを全件取得する。
     *
     * @return ワークフローインスタンステーブルのデータ
     */
    public SqlResultSet findWorkflowInstance() {
        return new SimpleDbTransactionExecutor<SqlResultSet>(transactionManager) {
            @Override
            public SqlResultSet execute(AppDbConnection connection) {
                SqlPStatement statement = connection.prepareStatement("SELECT * FROM WF_INSTANCE");
                return statement.retrieve();
            }
        }.doTransaction();
    }

    /**
     * インスタンスフローノードテーブルのデータを全件取得する。
     *
     * @return インスタンスフローノードテーブルのデータ
     */
    public SqlResultSet findInstanceFlowNode() {
        return new SimpleDbTransactionExecutor<SqlResultSet>(transactionManager) {
            @Override
            public SqlResultSet execute(AppDbConnection connection) {
                SqlPStatement statement = connection.prepareStatement(
                        "SELECT * FROM WF_INSTANCE_FLOW_NODE ORDER BY INSTANCE_ID, FLOW_NODE_ID");
                return statement.retrieve();
            }
        }.doTransaction();
    }

    /**
     * 担当者テーブルのデータを全件取得する。
     *
     * @return 担当者テーブルのデータ
     */
    public SqlResultSet findAssignedUser() {
        return new SimpleDbTransactionExecutor<SqlResultSet>(transactionManager) {
            @Override
            public SqlResultSet execute(AppDbConnection connection) {
                SqlPStatement statement = connection.prepareStatement(
                        "SELECT * FROM WF_TASK_ASSIGNED_USER ORDER BY INSTANCE_ID, FLOW_NODE_ID, ASSIGNED_USER_ID");
                return statement.retrieve();
            }
        }.doTransaction();
    }

    /**
     * 担当グループのデータを全件取得する。
     *
     * @return 担当グループのデータ
     */
    public SqlResultSet findAssignedGroup() {
        return new SimpleDbTransactionExecutor<SqlResultSet>(transactionManager) {
            @Override
            public SqlResultSet execute(AppDbConnection connection) {
                SqlPStatement statement = connection.prepareStatement(
                        "SELECT * FROM WF_TASK_ASSIGNED_GROUP ORDER BY INSTANCE_ID, FLOW_NODE_ID, ASSIGNED_GROUP_ID");
                return statement.retrieve();
            }
        }.doTransaction();
    }

    /**
     * アクティブフローノードのデータを全件取得する。
     *
     * @return アクティブフローノードのデータ
     */
    public SqlResultSet findActiveFlowNode() {
        return new SimpleDbTransactionExecutor<SqlResultSet>(transactionManager) {
            @Override
            public SqlResultSet execute(AppDbConnection connection) {
                SqlPStatement statement = connection.prepareStatement(
                        "SELECT * FROM WF_ACTIVE_FLOW_NODE ORDER BY INSTANCE_ID, FLOW_NODE_ID");
                return statement.retrieve();
            }
        }.doTransaction();
    }


    /**
     * アクティブなタスクのアサインユーザデータを取得する。
     *
     * @return アクティブタスクユーザデータ
     */
    public SqlResultSet findActiveUserTask() {
        return new SimpleDbTransactionExecutor<SqlResultSet>(transactionManager) {
            @Override
            public SqlResultSet execute(AppDbConnection connection) {
                SqlPStatement statement = connection.prepareStatement(
                        "SELECT * FROM WF_ACTIVE_USER_TASK ORDER BY INSTANCE_ID, FLOW_NODE_ID, ASSIGNED_USER_ID");
                return statement.retrieve();
            }
        }.doTransaction();
    }

    public SqlResultSet findActiveGroupTask() {
        return new SimpleDbTransactionExecutor<SqlResultSet>(transactionManager) {
            @Override
            public SqlResultSet execute(AppDbConnection connection) {
                SqlPStatement statement = connection.prepareStatement(
                        "SELECT * FROM WF_ACTIVE_GROUP_TASK ORDER BY INSTANCE_ID, FLOW_NODE_ID, ASSIGNED_GROUP_ID");
                return statement.retrieve();
            }
        }.doTransaction();
    }

    /**
     * 単純なプロセス定義(プロセスID:12345)を登録する。
     */
    public void createSimpleProcess() throws Exception {
        WorkflowEntity process = new WorkflowEntity("12345", (long) 10, "ホゲプロセス", "20140101");
        insertWorkflowEntity(process);

        LaneEntity lane = new LaneEntity(process, "l01", "レーン");
        insertLaneEntity(lane);

        insertEventEntity(new EventEntity("e01", lane, "開始イベント", "START"));
        insertEventEntity(new EventEntity("e02", lane, "終了イベント", "TERMINATE"));

        insertTaskEntity(new TaskEntity("a01", lane, "承認", "NONE", null));
        insertTaskEntity(new TaskEntity("a02", lane, "再申請", "NONE", null));

        insertGatewayEntity(new GatewayEntity("g01", lane, "承認・差し戻し", "EXCLUSIVE"));

        insertSequenceEntity(new SequenceFlowEntity(process, "000000001", "申請処理", "e01", "a01", null));
        insertSequenceEntity(new SequenceFlowEntity(process, "000000002", "承認・差し戻し処理", "a01", "g01", null));
        insertSequenceEntity(new SequenceFlowEntity(process, "000000003", "承認処理", "g01", "e02", null));
        insertSequenceEntity(new SequenceFlowEntity(process, "000000004", "差し戻し処理", "e02", "a02", null));
        insertSequenceEntity(new SequenceFlowEntity(process, "000000005", "再申請処理", "a02", "g01", null));
    }
}


