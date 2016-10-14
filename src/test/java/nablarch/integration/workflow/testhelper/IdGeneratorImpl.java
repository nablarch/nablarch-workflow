package nablarch.integration.workflow.testhelper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import nablarch.common.idgenerator.IdFormatter;
import nablarch.common.idgenerator.IdGenerator;
import nablarch.core.util.Base64Util;

/**
 * インスタンスIDの採番で使用する{@link IdGenerator}実装クラス。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public class IdGeneratorImpl implements IdGenerator{

    /**
     * 引数で指定された採番対象ID内でユニークなIDを採番する。
     *
     * @param id 採番対象を識別するID
     * @return 採番対象ID内でユニークな採番結果のID
     */
    @Override
    public String generateId(String id) {
        if (id.length() != 2) {
            throw new RuntimeException("IDは二桁固定でお願いします。");
        }

        String value = id + new SecureRandom().nextLong();
        MessageDigest messageDigest = createMessageDigest();
        String encoded = Base64Util.encode(messageDigest.digest(value.getBytes()));
        return id + encoded.substring(0, 8);
    }

    private MessageDigest createMessageDigest() {
        try {
            return MessageDigest.getInstance("sha-512");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 引数で指定された採番対象ID内でユニークなIDを採番し、指定された{@link nablarch.common.idgenerator.IdFormatter}でフォーマットし返却する。
     *
     * @param id 採番対象を識別するID
     * @param formatter 採番したIDをフォーマットするIdFormatter
     * @return 採番対象ID内でユニークな採番結果のID
     */
    @Override
    public String generateId(String id, IdFormatter formatter) {
        throw new UnsupportedOperationException("実装していないですよ。");
    }
}

