package cn.org.autumn.mybatis;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

/**
 * @author Shaohua Xu
 * @date 2018/10
 * 自定义Select in 注解,用于动态生成Select in 语句
 */
public class SelectInLangDriver extends XMLLanguageDriver implements LanguageDriver {

    /**
     * Pattern静态申明
     */
    private final Pattern inPattern = Pattern.compile("\\(#\\{(\\w+)\\}\\)");

    /**
     * 实现自定义Select in 注解
     *
     * @param configuration 配置参数
     * @param script        入参
     * @param parameterType 参数类型
     * @return 转换后的SqlSource
     */
    @Override
    public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType) {
        Matcher matcher = inPattern.matcher(script);
        if (matcher.find()) {
            script = matcher.replaceAll("<foreach collection=\"$1\" item=\"_item\" open=\"(\" "
                    + "separator=\",\" close=\")\" >#{_item}</foreach>");
        }
        script = "<script>" + script + "</script>";

        return super.createSqlSource(configuration, script, parameterType);
    }
}
