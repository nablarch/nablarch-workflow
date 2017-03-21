package nablarch.integration.workflow.testhelper;

import java.util.Properties;

import javax.sql.DataSource;

import nablarch.core.repository.di.ComponentFactory;
import org.apache.commons.dbcp.BasicDataSourceFactory;

/**
 * プールデータソースをキャッシュするデータソースファクトリクラス。
 */
public class DataSourceFactory implements ComponentFactory<DataSource> {

    private static DataSource dataSource;

    private String driverClassName;

    private String user;

    private String password;

    private String url;

    @Override
    public synchronized DataSource createObject() {

        if (dataSource != null) {
            return dataSource;
        }

        try {
            Properties properties = new Properties();
            properties.setProperty("driverClassName", driverClassName);
            properties.setProperty("url", url);
            properties.setProperty("user", user);
            properties.setProperty("password", password);
            properties.setProperty("maxPoolSize", "5");
            properties.setProperty("initialPoolSize", "5");
            properties.setProperty("maxStatements", "100");
            dataSource = BasicDataSourceFactory.createDataSource(properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dataSource;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
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
