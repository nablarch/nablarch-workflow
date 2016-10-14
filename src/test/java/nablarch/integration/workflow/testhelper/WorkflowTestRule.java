package nablarch.integration.workflow.testhelper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.rules.ExternalResource;

import nablarch.core.db.connection.AppDbConnection;
import nablarch.core.db.connection.ConnectionFactory;
import nablarch.core.db.connection.DbConnectionContext;
import nablarch.core.db.connection.TransactionManagerConnection;
import nablarch.core.db.statement.SqlPStatement;
import nablarch.core.db.statement.exception.SqlStatementException;
import nablarch.core.db.transaction.SimpleDbTransactionExecutor;
import nablarch.core.db.transaction.SimpleDbTransactionManager;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.transaction.TransactionContext;
import nablarch.core.util.FileUtil;

import nablarch.integration.workflow.definition.WorkflowDefinitionHolder;

/**
 * ワークフロー関連のテストを行う際に必要となる提携セットアップ処理をまとめたルール実装クラス。
 * <p/>
 * 本クラスを利用することで、リポジトリの初期化などの提携ロジックを各テストクラスで実装する必要がなくなる。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class WorkflowTestRule extends ExternalResource {

    /** DDLパス */
    private static final String DDL_PATH = "nablarch/integration/workflow/forward_with_drop.sql";

    /** DDL実行済みか否か */
    private static Boolean ddlExecuted = null;

    /** 外部キーの使用有無 */
    private final Boolean useFk;

    public WorkflowTestRule() {
        this(false);
    }

    public WorkflowTestRule(boolean useFk) {
        this.useFk = useFk;
    }

    @Override
    protected void before() throws Throwable {
        // 念のためリポジトリ情報は事前にクリアする。
        SystemRepository.clear();

        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/integration/workflow/default-definition.xml");
        SystemRepository.load(new DiContainer(loader));

        // 必要なテーブルのセットアップ処理
        if (!useFk.equals(ddlExecuted)) {
            createWorkflowProcessDefinitionTable(loadSql());
            ddlExecuted = useFk;
        }

        // 業務トランザクション用コネクションの登録
        ConnectionFactory connectionFactory = SystemRepository.get("databaseConnectionFactory");
        TransactionManagerConnection connection = connectionFactory.getConnection(TransactionContext.DEFAULT_TRANSACTION_CONTEXT_KEY);
        DbConnectionContext.setConnection(connection);

    }

    @Override
    protected void after() {
        try {
            TransactionManagerConnection connection = (TransactionManagerConnection) DbConnectionContext.getConnection();
            connection.rollback();
            connection.terminate();
        } catch (Exception e) {
            e.printStackTrace();
        }

        SystemRepository.clear();
        DbConnectionContext.removeConnection();
    }

    public void commit() {
        TransactionManagerConnection connection = (TransactionManagerConnection) DbConnectionContext.getConnection();
        connection.commit();
    }

    /**
     * プロセス定義情報を現在の定義情報を元に最新化する。
     */
    public void reloadProcessDefinitions() {
        WorkflowDefinitionHolder workflowDefinitionHolder = SystemRepository.get("workflowDefinitionHolder");
        workflowDefinitionHolder.initialize();
    }

    /**
     * ワークフロープロセス定義テーブルを構築する。
     *
     * @param sqlHolder 実行対象のSQL文
     */
    public void createWorkflowProcessDefinitionTable(final SqlHolder sqlHolder) {

        SimpleDbTransactionManager transactionManager = getTransactionManager();
        new SimpleDbTransactionExecutor<Void>(transactionManager) {
            @Override
            public Void execute(AppDbConnection connection) {
                for (String sql : sqlHolder.dropList) {
                    SqlPStatement statement = connection.prepareStatement(sql);
                    try {
                        statement.execute();
                    } catch (SqlStatementException ignored) {
                        ignored.printStackTrace();
                    }
                }
                for (String sql : sqlHolder.createList) {
                    SqlPStatement statement = connection.prepareStatement(sql);
                    statement.execute();
                }
                for (String sql : sqlHolder.alterList) {
                    if (!useFk && sql.contains("FOREIGN KEY")) {
                        continue;
                    }
                    SqlPStatement statement = connection.prepareStatement(sql);
                    statement.execute();
                }
                return null;
            }

        }.doTransaction();
    }

    /**
     * データベーストランザクションを取得する。
     *
     * @return トランザクションオブジェクト
     */
    public SimpleDbTransactionManager getTransactionManager() {
        return SystemRepository.get("tran");
    }

    /**
     * ワークフローDAOを取得する。
     */
    public WorkflowDbAccessSupport getWorkflowDao() {
        return new WorkflowDbAccessSupport(getTransactionManager());
    }

    /**
     * SQLファイルからSQL文をロードする。
     */
    private SqlHolder loadSql() throws Exception {
        InputStream resource = FileUtil.getClasspathResource(
                DDL_PATH);
        if (resource == null) {
            throw new IllegalStateException("forward_with_drop.sql was not found.");
        }

        SqlHolder result = new SqlHolder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(resource, "UTF-8"));
        try {
            StringBuilder sql = new StringBuilder();
            SqlHolder.SqlType type = null;
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.equals("/") && sql.length() != 0) {
                    switch (type) {
                        case drop:
                            result.dropList.add(sql.toString());
                            break;
                        case create:
                            result.createList.add(sql.toString());
                            break;
                        case alter:
                            result.alterList.add(sql.toString());
                            break;
                    }
                    sql.setLength(0);
                    continue;
                }
                if (sql.length() == 0) {
                    if (line.contains("DROP ")) {
                        line = line.substring(line.indexOf("DROP "));
                        type = SqlHolder.SqlType.drop;
                    } else if (line.contains("CREATE ")) {
                        line = line.substring(line.indexOf("CREATE "));
                        type = SqlHolder.SqlType.create;
                    } else if (line.contains("ALTER ")) {
                        line = line.substring(line.indexOf("ALTER "));
                        type = SqlHolder.SqlType.alter;
                    }
                }
                sql.append(line);
                sql.append(' ');
            }
        } finally {
            FileUtil.closeQuietly(reader);
        }
        return result;
    }

    private static class SqlHolder {

        enum SqlType {
            drop,
            create,
            alter
        }

        private List<String> dropList = new ArrayList<String>();

        private List<String> createList = new ArrayList<String>();

        private List<String> alterList = new ArrayList<String>();
    }
}


