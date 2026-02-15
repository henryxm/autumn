package cn.org.autumn.modules.sys.service;

import cn.org.autumn.site.InitFactory;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.org.autumn.utils.Constant;
import cn.org.autumn.annotation.DataFilter;
import cn.org.autumn.modules.sys.dao.SysDeptDao;
import cn.org.autumn.modules.sys.entity.SysDeptEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SysDeptService extends ServiceImpl<SysDeptDao, SysDeptEntity> implements InitFactory.Init {

    @Autowired
    private SysDeptDao sysDeptDao;

    private static final String NULL = null;

    public static final String Department_System_Administrator = "Department:System:Administrator";

    public static final String Department_System_User = "Department:System:User";

    @Order(-1)
    public void init() {
        String[][] mapping = new String[][]{
                //{部门标识,                   父级标识,  部门名称,   排序号,删除标记,备注说明}
                {Department_System_Administrator, "", "系统超级管理", "0", "0", "超级管理员部门，系统默认创建"},
                {Department_System_User, Department_System_Administrator, "系统后台管理", "0", "0", "系统管理普通用户部门"},
        };
        for (String[] map : mapping) {
            SysDeptEntity sysDeptEntity = new SysDeptEntity();
            String temp = map[0];
            if (null != baseMapper.getByDeptKey(temp))
                continue;
            if (NULL != temp)
                sysDeptEntity.setDeptKey(temp);
            temp = map[1];
            if (StringUtils.isNotEmpty(temp)) {
                sysDeptEntity.setParentKey(temp);
            } else {
                sysDeptEntity.setParentKey("");
            }
            temp = map[2];
            if (NULL != temp)
                sysDeptEntity.setName(temp);
            temp = map[3];
            if (NULL != temp)
                sysDeptEntity.setOrderNum(Integer.valueOf(temp));
            temp = map[4];
            if (NULL != temp)
                sysDeptEntity.setDelFlag(Integer.valueOf(temp));
            temp = map[5];
            if (NULL != temp)
                sysDeptEntity.setRemark(temp);
            save(sysDeptEntity);
        }
    }

    public boolean save(SysDeptEntity dept) {
        if (null != dept.getParentKey()) {
            SysDeptEntity parent = getByDeptKey(dept.getParentKey());
            if (null != parent)
                dept.setParentKey(parent.getDeptKey());
            else
                dept.setParentKey("");
        }
        if (null == dept.getDelFlag()) {
            dept.setDelFlag(0);
        }
        return saveOrUpdate(dept);
    }

    public SysDeptEntity getByDeptKey(String deptKey) {
        return baseMapper.getByDeptKey(deptKey);
    }

    @DataFilter(subDept = true, user = false)
    public List<SysDeptEntity> queryList(Map<String, Object> params) {
        List<SysDeptEntity> deptList =
                this.list(new QueryWrapper<SysDeptEntity>()
                        .apply(params.get(Constant.SQL_FILTER) != null, (String) params.get(Constant.SQL_FILTER)));

        for (SysDeptEntity sysDeptEntity : deptList) {
            SysDeptEntity parentDeptEntity = getByDeptKey(sysDeptEntity.getParentKey());
            if (parentDeptEntity != null) {
                sysDeptEntity.setParentName(parentDeptEntity.getName());
            }
        }
        return deptList;
    }

    public List<String> getByParentKey(String parentKey) {
        return baseMapper.getByParentKey(parentKey);
    }

    public List<String> getSubDeptKeys(String deptKey) {
        //部门及子部门ID列表
        List<String> deptIdList = new ArrayList<>();
        //获取子部门ID
        List<String> subIdList = getByParentKey(deptKey);
        getDeptTreeList(subIdList, deptIdList);
        return deptIdList;
    }

    /**
     * 递归
     */
    private void getDeptTreeList(List<String> subKeys, List<String> deptKeys) {
        for (String deptKey : subKeys) {
            List<String> list = getByParentKey(deptKey);
            if (list.size() > 0) {
                getDeptTreeList(list, deptKeys);
            }
            deptKeys.add(deptKey);
        }
    }
}
