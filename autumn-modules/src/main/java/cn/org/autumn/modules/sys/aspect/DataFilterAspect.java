package cn.org.autumn.modules.sys.aspect;

import cn.org.autumn.annotation.DataFilter;
import cn.org.autumn.utils.Constant;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysDeptService;
import cn.org.autumn.modules.sys.service.SysRoleDeptService;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.exception.AException;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 数据过滤，切面处理类
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

    @Pointcut("@annotation(cn.org.autumn.annotation.DataFilter)")
    public void dataFilterCut() {

    }

    @Before("dataFilterCut()")
    public void dataFilter(JoinPoint point) throws Throwable {
        Object params = point.getArgs()[0];
        if (params != null && params instanceof Map) {
            SysUserEntity user = ShiroUtils.getUserEntity();

            //如果不是超级管理员，则进行数据过滤
            if (sysUserRoleService.isSystemAdministrator(user)) {
                Map map = (Map) params;
//                map.put(Constant.SQL_FILTER, getSQLFilter(user, point));
            }

            return;
        }

        throw new AException("数据权限接口，只能是Map类型参数，且不能为NULL");
    }

    /**
     * 获取数据过滤的SQL
     */
    private String getSQLFilter(SysUserEntity user, JoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        DataFilter dataFilter = signature.getMethod().getAnnotation(DataFilter.class);
        //获取表的别名
        String tableAlias = dataFilter.tableAlias();
        if (StringUtils.isNotBlank(tableAlias)) {
            tableAlias += ".";
        }

        //部门ID列表
        Set<String> deptIdList = new HashSet<>();

        //用户角色对应的部门ID列表
        List<String> roleIdList = sysUserRoleService.getRoleKeys(user.getUuid());
        if (roleIdList.size() > 0) {
            List<String> userDeptIdList = sysRoleDeptService.getDeptKeys(roleIdList.toArray(new String[roleIdList.size()]));
            deptIdList.addAll(userDeptIdList);
        }

        //用户子部门ID列表
        if (dataFilter.subDept()) {
            List<String> subDeptIdList = sysDeptService.getSubDeptKeys(user.getDeptKey());
            deptIdList.addAll(subDeptIdList);
        }

        StringBuilder sqlFilter = new StringBuilder();
        sqlFilter.append(" (");

        if (deptIdList.size() > 0) {
            sqlFilter.append(tableAlias).append(" find_in_set(").append(dataFilter.deptId()).append(", '").append(StringUtils.join(deptIdList, ",")).append("')");
        }

        //没有本部门数据权限，也能查询本人数据
        if (dataFilter.user()) {
            if (deptIdList.size() > 0) {
                sqlFilter.append(" or ");
            }
            sqlFilter.append(tableAlias).append(dataFilter.userUuid()).append("=").append(user.getUuid());
        }

        sqlFilter.append(")");

        return sqlFilter.toString();
    }
}
