package cn.org.autumn.modules.sys.aspect;

import cn.org.autumn.annotation.DataFilter;
import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.exception.AException;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysDeptService;
import cn.org.autumn.modules.sys.service.SysRoleDeptService;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.utils.Constant;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 数据过滤切面：非超管写入 {@link Constant#SQL_FILTER}；超管跳过。
 */
@Aspect
@Component
public class DataFilterAspect {
    @Autowired
    private SysDeptService sysDeptService;
    @Autowired
    private SysUserRoleService sysUserRoleService;
    @Autowired
    private SysRoleDeptService sysRoleDeptService;

    @Autowired
    private RuntimeSqlDialect runtimeSqlDialect;

    @Pointcut("@annotation(cn.org.autumn.annotation.DataFilter)")
    public void dataFilterCut() {
    }

    @Before("dataFilterCut()")
    public void dataFilter(JoinPoint point) throws Throwable {
        Object params = point.getArgs()[0];
        if (params != null && params instanceof Map) {
            SysUserEntity user = ShiroUtils.getUserEntity();
            if (user == null) {
                throw new AException("数据权限：未登录");
            }
            // 超级管理员不过滤
            if (sysUserRoleService.isSystemAdministrator(user)) {
                return;
            }
            @SuppressWarnings("rawtypes")
            Map map = (Map) params;
            map.put(Constant.SQL_FILTER, getSQLFilter(user, point));
            return;
        }
        throw new AException("数据权限接口，只能是Map类型参数，且不能为NULL");
    }

    /**
     * 获取数据过滤的SQL（部门 IN + 可选本人 uuid，uuid 经方言字面量转义）
     */
    private String getSQLFilter(SysUserEntity user, JoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        DataFilter dataFilter = signature.getMethod().getAnnotation(DataFilter.class);
        String tableAlias = dataFilter.tableAlias();
        if (StringUtils.isNotBlank(tableAlias)) {
            tableAlias += ".";
        }

        Set<String> deptIdList = new HashSet<>();
        List<String> roleIdList = sysUserRoleService.getRoleKeys(user.getUuid());
        if (!roleIdList.isEmpty()) {
            List<String> userDeptIdList = sysRoleDeptService.getDeptKeys(roleIdList.toArray(new String[0]));
            deptIdList.addAll(userDeptIdList);
        }
        if (dataFilter.subDept()) {
            List<String> subDeptIdList = sysDeptService.getSubDeptKeys(user.getDeptKey());
            deptIdList.addAll(subDeptIdList);
        }

        StringBuilder sqlFilter = new StringBuilder();
        sqlFilter.append(" (");

        boolean hasPart = false;
        if (!deptIdList.isEmpty()) {
            String deptCol = tableAlias + dataFilter.deptId();
            sqlFilter.append(runtimeSqlDialect.columnValueInCommaSeparatedList(deptCol, StringUtils.join(deptIdList, ",")));
            hasPart = true;
        }

        if (dataFilter.user()) {
            if (hasPart) {
                sqlFilter.append(" or ");
            }
            String uuidLit = quoteSqlStringLiteral(user.getUuid());
            sqlFilter.append(tableAlias).append(dataFilter.userUuid()).append("=").append(uuidLit);
            hasPart = true;
        }

        if (!hasPart) {
            // 无部门且不查本人：恒假，避免无过滤泄露全表
            sqlFilter.append("1=0");
        }

        sqlFilter.append(")");
        return sqlFilter.toString();
    }

    /** 将 uuid 写成安全的 SQL 字符串字面量（单引号转义） */
    static String quoteSqlStringLiteral(String value) {
        if (value == null) {
            return "''";
        }
        return "'" + value.replace("'", "''") + "'";
    }
}
