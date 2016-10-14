package nablarch.integration.workflow.dao;

import nablarch.core.db.connection.AppDbConnection;
import nablarch.core.db.connection.DbConnectionContext;
import nablarch.core.db.statement.SqlPStatement;

/**
 * データベースアクセスクラスのサポートクラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public abstract class DaoSupport {

    /**
     * SQL実行用のスタートメントを生成する。
     *
     * @param sql SQL
     * @return 生成したステートメント
     */
    protected SqlPStatement createStatement(String sql) {
        AppDbConnection connection = DbConnectionContext.getConnection();
        return connection.prepareStatement(sql);
    }
}

