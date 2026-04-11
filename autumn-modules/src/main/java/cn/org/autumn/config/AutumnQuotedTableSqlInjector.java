package cn.org.autumn.config;

import com.baomidou.mybatisplus.entity.GlobalConfiguration;
import com.baomidou.mybatisplus.entity.TableInfo;
import com.baomidou.mybatisplus.mapper.LogicSqlInjector;
import com.baomidou.mybatisplus.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.toolkit.StringUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;

/**
 * MP 2.x 仅在列命中 {@link com.baomidou.mybatisplus.toolkit.SqlReservedWords} 时用全局 {@code identifier-quote} 转义，
 * 表名始终原样拼进 {@code INSERT/UPDATE/DELETE/SELECT}；Derby/DB2 等会将未加引号的标识符折成大写，与注解 DDL 中
 * {@code "sys_config"} 等小写双引号表不一致。在注入 MappedStatement 前按全局引号模式包装物理表名（与
 * {@link cn.org.autumn.database.DatabaseType#mybatisPlusIdentifierQuotePattern()} / JDBC 推断一致）。
 */
public class AutumnQuotedTableSqlInjector extends LogicSqlInjector {

    @Override
    protected void injectSql(MapperBuilderAssistant builderAssistant, Class<?> mapperClass, Class<?> modelClass,
                             TableInfo table) {
        if (builderAssistant != null && table != null) {
            GlobalConfiguration gc = GlobalConfigUtils.getGlobalConfig(builderAssistant.getConfiguration());
            applyQuotedTableName(gc, table);
        }
        super.injectSql(builderAssistant, mapperClass, modelClass, table);
    }

    static void applyQuotedTableName(GlobalConfiguration gc, TableInfo table) {
        if (gc == null) {
            return;
        }
        String pattern = gc.getIdentifierQuote();
        if (StringUtils.isEmpty(pattern) || !pattern.contains("%s")) {
            return;
        }
        String name = table.getTableName();
        if (StringUtils.isEmpty(name)) {
            return;
        }
        char c = name.charAt(0);
        if (c == '"' || c == '`' || c == '[') {
            return;
        }
        table.setTableName(String.format(pattern, name));
    }
}
