package nablarch.integration.workflow.util;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nablarch.core.util.StringUtil;

/**
 * ワークフロー機能で使用するユティリティ群。
 *
 * @author hisaaki sioiri
 * @since 1.4.2
 */
public final class WorkflowUtil {

    /** クラス名とコンストラクタ引数を抽出する正規表現 */
    private static final Pattern CLASS_NAME_AND_PARAM_PATTERN = Pattern.compile("^([^\\(]+)(?:\\(([^\\)]+)\\))?$");

    /**
     * 隠蔽コンストラクタ。
     */
    private WorkflowUtil() {
    }

    /**
     * 指定されたListを指定されたフィルタ条件で絞り込む。
     *
     * @param list フィルタ対象のリスト
     * @param listFilter 条件
     * @param <T> フィルタ対象オブジェクトの型
     * @return 絞込結果
     */
    public static <T> List<T> filterList(List<? extends T> list, ListFilter<T> listFilter) {
        List<T> result = new ArrayList<T>();
        for (T t : list) {
            if (listFilter.isMatch(t)) {
                result.add(t);
            }
        }
        return result;
    }

    /**
     * 指定されたListから条件に一致する最初の要素を取得する。
     *
     * @param list リスト
     * @param condition 条件
     * @param <T> オブジェクトの型
     * @return 最初に条件に一致した要素
     */
    public static <T> T find(List<? extends T> list, ListFilter<T> condition) {
        for (T t : list) {
            if (condition.isMatch(t)) {
                return t;
            }
        }
        return null;
    }

    /**
     * リスト内に指定された条件に一致するオブジェクトが存在しているかチェックする。
     *
     * @param list チェック対象のリスト
     * @param listFilter 条件
     * @param <T> オブジェクトの型
     * @return 条件に一致するオブジェクトが存在している場合はtrue
     */
    public static <T> boolean contains(List<? extends T> list, ListFilter<T> listFilter) {
        for (T t : list) {
            if (listFilter.isMatch(t)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 与えられた文字列からインスタンスを生成する。
     * <p/>
     * 文字列がnullや空文字列の場合には、nullを返却する。
     * <p/>
     * クラス名を表す文字列は以下の形式であること。
     * <ul>
     * <li>クラス名部は完全修飾名であること</li>
     * <li>コンストラクタ引数は、クラス名の後に括弧で囲って指定すること。</li>
     * </ul>
     * <p/>
     * 引数無しコンストラクタを持つクラスの場合: {@code package.subpackage.ClassName}
     * 引数有りコンストラクタを持つクラスの場合: {@code package.subpackage.ClassName2(arg1, args)}
     * <p/>
     * 制約<br/>
     * コンストラクタの引数の方は全てStringであること。
     * クラスの責務としてString意外を要求する場合には、そのクラス内にて適切な型に変換すること。
     *
     * @param classNamePattern クラス名を表す文字列
     * @param <T> 生成するインスタンスの型
     * @return 生成したインスタンス
     */
    public static <T> T createInstance(String classNamePattern) {
        if (StringUtil.isNullOrEmpty(classNamePattern)) {
            return null;
        }
        Matcher matcher = CLASS_NAME_AND_PARAM_PATTERN.matcher(classNamePattern);
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    String.format("invalid class name pattern. param string = [%s]", classNamePattern));
        }

        String className = matcher.group(1);
        List<String> params = new ArrayList<String>();
        if (matcher.group(2) != null) {
            String[] paramString = matcher.group(2).split(",");
            for (String param : paramString) {
                params.add(param.trim());
            }
        }
        try {
            @SuppressWarnings("unchecked")
            Class<T> clazz = (Class<T>) Class.forName(className);
            Constructor<T> constructor = clazz.getConstructor(createStringClassArray(params.size()));
            return constructor.newInstance(params.toArray(new String[params.size()]));
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("failed to create instance. class name pattern = [%s]", classNamePattern), e);
        }
    }

    /**
     * 指定されたサイズのString.classを持つClass配列を生成する。
     *
     * @param size サイズ
     * @return 生成した配列
     */
    private static Class[] createStringClassArray(int size) {
        Class[] result = new Class[size];
        for (int i = 0; i < size; i++) {
            result[i] = String.class;
        }
        return result;
    }

    /**
     * Listの要素をフィルタする際の条件。
     *
     * @param <T> 比較対象の型
     */
    public interface ListFilter<T> {

        /**
         * 比較処理を行う。
         *
         * @param other 比較対象
         * @return フィルタ条件に合致した場合はtrue
         */
        boolean isMatch(T other);
    }
}

