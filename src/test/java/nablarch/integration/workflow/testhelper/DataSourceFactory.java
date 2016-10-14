package nablarch.integration.workflow.testhelper;

import java.sql.SQLException;

import javax.sql.DataSource;

import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

import nablarch.core.repository.di.ComponentFactory;

/**
 * プールデータソースをキャッシュするデータソースファクトリクラス。
 *
 * 自動テスト全実行などで、リポジトリが初期化されるたびに{@link PoolDataSource}が生成されると、
 * そのたびに接続プールが作成されデータベースへの物理接続が行われる。
 *
 * この問題を回避するために、本クラスでは生成した{@link PoolDataSource}をクラス内に保持し、
 * {@link #createObject()}が複数回呼び出された場合には初回に生成した{@link PoolDataSource}を返却する。
 */
public class DataSourceFactory implements ComponentFactory<DataSource> {

    private static final String POOL_NAME = "workflow-test";

    private static DataSource dataSource = null;

    private String user;

    private String password;

    private String url;

    @Override
    public synchronized DataSource createObject() {

        if (dataSource != null) {
            return dataSource;
        }

        try {
            PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();
            pds.setConnectionPoolName(POOL_NAME);
            pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
            pds.setURL(url);
            pds.setUser(user);
            pds.setPassword(password);
            pds.setMaxPoolSize(5);
            pds.setInitialPoolSize(5);
            pds.setMaxStatements(100);
            dataSource = pds;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dataSource;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
