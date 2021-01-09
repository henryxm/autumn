package cn.org.autumn.modules.sys.service;

import cn.org.autumn.site.InitFactory;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import cn.org.autumn.utils.Constant;
import cn.org.autumn.annotation.DataFilter;
import cn.org.autumn.modules.sys.dao.SysDeptDao;
import cn.org.autumn.modules.sys.entity.SysDeptEntity;
import org.apache.commons.lang.StringUtils;
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

    Map<String, SysDeptEntity> cache = new HashMap<>();

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
                SysDeptEntity parent = cache.get(temp);
                Long dID = 0L;
                if (null != parent)
                    dID = parent.getDeptId();
                sysDeptEntity.setParentId(dID);
            } else {
                sysDeptEntity.setParentKey("");
                sysDeptEntity.setParentId(0L);
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
            sysDeptDao.insert(sysDeptEntity);
            cache.put(sysDeptEntity.getDeptKey(), sysDeptEntity);
        }
    }

    public void save(SysDeptEntity dept){
        if (null != dept.getParentId()) {
            SysDeptEntity parent = selectById(dept.getParentId());
            if (null != parent)
                dept.setParentKey(parent.getDeptKey());
            else
                dept.setParentKey("");
        }
        if (null == dept.getDelFlag()) {
            dept.setDelFlag(0);
        }
        insertOrUpdate(dept);
    }

    public SysDeptEntity getByDeptKey(String deptKey) {
        SysDeptEntity sysDeptEntity = cache.get(deptKey);
        if (null == sysDeptEntity) {
            sysDeptEntity = baseMapper.getByDeptKey(deptKey);
            cache.put(deptKey, sysDeptEntity);
        }
        return sysDeptEntity;
    }

    @DataFilter(subDept = true, user = false)
    public List<SysDeptEntity> queryList(Map<String, Object> params) {
        List<SysDeptEntity> deptList =
                this.selectList(new EntityWrapper<SysDeptEntity>()
                        .addFilterIfNeed(params.get(Constant.SQL_FILTER) != null, (String) params.get(Constant.SQL_FILTER)));

        for (SysDeptEntity sysDeptEntity : deptList) {
            SysDeptEntity parentDeptEntity = this.selectById(sysDeptEntity.getParentId());
            if (parentDeptEntity != null) {
                sysDeptEntity.setParentName(parentDeptEntity.getName());
            }
        }
        return deptList;
    }

    public List<Long> queryDetpIdList(Long parentId) {
        return baseMapper.queryDetpIdList(parentId);
    }

    public List<Long> getSubDeptIdList(Long deptId) {
        //部门及子部门ID列表
        List<Long> deptIdList = new ArrayList<>();

        //获取子部门ID
        List<Long> subIdList = queryDetpIdList(deptId);
        getDeptTreeList(subIdList, deptIdList);

        return deptIdList;
    }

    /**
     * 递归
     */
    private void getDeptTreeList(List<Long> subIdList, List<Long> deptIdList) {
        for (Long deptId : subIdList) {
            List<Long> list = queryDetpIdList(deptId);
            if (list.size() > 0) {
                getDeptTreeList(list, deptIdList);
            }

            deptIdList.add(deptId);
        }
    }
}
