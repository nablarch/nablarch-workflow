package nablarch.integration.workflow.testhelper;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import nablarch.core.date.SystemTimeProvider;

/**
 * テストで使用する{@link nablarch.core.date.SystemTimeProvider}実装クラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class SystemTimeProviderImpl implements SystemTimeProvider {

    public static String now;

    private static final DateFormat FORMAT = new SimpleDateFormat("yyyyMMdd");

    /**
     * システム日時を取得する。
     *
     * @return システム日時
     */
    @Override
    public Date getDate() {
        if (now == null) {
            return new Date();
        }
        try {
            return FORMAT.parse(now);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * システム日時を取得する。
     *
     * @return システム日時
     */
    @Override
    public Timestamp getTimestamp() {
        if (now == null) {
            return new Timestamp(new Date().getTime());
        }
        try {
            return new Timestamp(FORMAT.parse(now).getTime());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

    }
}
