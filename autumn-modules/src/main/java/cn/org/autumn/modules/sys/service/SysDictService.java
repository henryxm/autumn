package cn.org.autumn.modules.sys.service;

import cn.org.autumn.site.InitFactory;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import cn.org.autumn.modules.sys.dao.SysDictDao;
import cn.org.autumn.modules.sys.entity.SysDictEntity;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;


@Service
public class SysDictService extends ServiceImpl<SysDictDao, SysDictEntity> implements InitFactory.Init {

    protected static final String NULL = null;

    @Autowired
    private SysDictDao sysDictDao;

    public void init() {
        String[][] mapping = new String[][]{
                {"1", "性别", "sex", "0", "女", "0", NULL, "0"},
                {"2", "性别", "sex", "1", "男", "1", NULL, "0"},
                {"3", "性别", "sex", "2", "未知", "3", NULL, "0"},
        };

        for (String[] map : mapping) {
            SysDictEntity entity = new SysDictEntity();
            String temp = map[0];
            if (NULL != temp)
                entity.setId(Long.valueOf(temp));
            temp = map[1];
            if (NULL != temp)
                entity.setName(temp);
            temp = map[2];
            if (NULL != temp)
                entity.setType(temp);
            temp = map[3];
            if (NULL != temp)
                entity.setCode(temp);
            temp = map[4];
            if (NULL != temp)
                entity.setValue(temp);
            temp = map[5];
            if (NULL != temp)
                entity.setOrderNum(Integer.valueOf(temp));
            temp = map[6];
            if (NULL != temp)
                entity.setRemark(temp);
            temp = map[7];
            if (NULL != temp)
                entity.setDelFlag(Integer.valueOf(temp));
            SysDictEntity et = sysDictDao.selectOne(entity);
            if (null == et)
                sysDictDao.insert(entity);
        }

    }

    public PageUtils queryPage(Map<String, Object> params) {
        String name = (String) params.get("name");
        EntityWrapper<SysDictEntity> entityEntityWrapper = new EntityWrapper<>();
        Page<SysDictEntity> page = this.selectPage(
                new Query<SysDictEntity>(params).getPage(),
                new EntityWrapper<SysDictEntity>()
                        .like(StringUtils.isNotBlank(name), "name", name)
        );
        page.setTotal(baseMapper.selectCount(entityEntityWrapper));
        return new PageUtils(page);
    }

    public List<SysDictEntity> getByType(String type) {
        return baseMapper.getByType(type);
    }
}
