/**
 * Copyright 2018 Autumn.org.cn http://www.autumn.org.cn
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package cn.org.autumn.modules.sys.service;

import cn.org.autumn.table.TableInit;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import cn.org.autumn.utils.Constant;
import cn.org.autumn.annotation.DataFilter;
import cn.org.autumn.modules.sys.dao.SysDeptDao;
import cn.org.autumn.modules.sys.entity.SysDeptEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
public class SysDeptService extends ServiceImpl<SysDeptDao, SysDeptEntity> {

    @Autowired
    private TableInit tableInit;

    @Autowired
    private SysDeptDao sysDeptDao;

    private static final String NULL = null;

    @PostConstruct
    public void init() {
        if (!tableInit.init)
            return;
        String[][] mapping = new String[][]{
                {"1", "0", "集团总公司", "0", "0"},
                {"2", "1", "四川分公司", "1", "0"},
                {"3", "1", "北京分公司", "2", "0"},
                {"4", "1", "广州分公司", "2", "0"},
                {"5", "1", "技术部", "0", "0"},
                {"6", "2", "销售部", "1", "0"},
                {"7", "3", "产品部", "1", "0"},
                {"8", "4", "渠道部", "1", "0"},
                {"9", "1", "后勤部", "1", "0"},
                {"10", "1", "总经办", "1", "0"},
        };

        for (String[] map : mapping) {
            SysDeptEntity sysMenu = new SysDeptEntity();
            String temp = map[0];
            if (NULL != temp)
                sysMenu.setDeptId(Long.valueOf(temp));
            temp = map[1];
            if (NULL != temp)
                sysMenu.setParentId(Long.valueOf(temp));
            temp = map[2];
            if (NULL != temp)
                sysMenu.setName(temp);
            temp = map[3];
            if (NULL != temp)
                sysMenu.setOrderNum(Integer.valueOf(temp));
            temp = map[4];
            if (NULL != temp)
                sysMenu.setDelFlag(Integer.valueOf(temp));
            SysDeptEntity entity = sysDeptDao.selectOne(sysMenu);
            if (null == entity)
                sysDeptDao.insert(sysMenu);
        }

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
