package cn.org.autumn.table.config;

import cn.org.autumn.table.data.InitType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 注解建表扫描与自动同步，前缀 {@code autumn.table.*}。
 * <p>
 * {@link #KEY_AUTO}、{@link #KEY_PACK} 及默认值为本项目唯一维护点；
 * 业务代码请注入本类或经 {@link cn.org.autumn.table.service.MysqlTableService} 读取，勿重复 {@code @Value}。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = TableProperties.PREFIX)
public class TableProperties {

    public static final String PREFIX = "autumn.table";

    public static final String KEY_AUTO = PREFIX + ".auto";

    public static final String KEY_PACK = PREFIX + ".pack";

    public static final String DEFAULT_PACK = "cn.org.autumn.modules";

    public static final InitType DEFAULT_AUTO = InitType.update;

    /**
     * 要扫描的实体包名；多个包可用逗号、分号、空格等分隔。
     */
    private String pack = DEFAULT_PACK;

    /**
     * 自动建表模式：{@code update} 增量同步，{@code create} 删表重建，{@code none} 不执行。
     */
    private InitType auto = DEFAULT_AUTO;

    /**
     * {@code update} 模式下是否根据 {@code @Table} 同步表字符集。
     */
    private boolean syncCharset = true;
}
