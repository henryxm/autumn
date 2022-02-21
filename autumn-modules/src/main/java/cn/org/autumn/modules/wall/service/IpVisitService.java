package cn.org.autumn.modules.wall.service;

import cn.org.autumn.modules.wall.entity.RData;
import org.springframework.stereotype.Service;
import cn.org.autumn.modules.wall.dao.IpVisitDao;
import cn.org.autumn.modules.wall.entity.IpVisitEntity;

import java.util.Date;

@Service
public class IpVisitService extends WallCounter<IpVisitDao, IpVisitEntity> {

    public IpVisitEntity getByIp(String ip) {
        return baseMapper.getByIp(ip);
    }

    @Override
    public String ico() {
        return "fa-eye";
    }

    public boolean hasIp(String ip) {
        Integer integer = baseMapper.hasIp(ip);
        if (null != integer && integer > 0)
            return true;
        return false;
    }

    public IpVisitEntity create(String ip, String tag, String description) {
        IpVisitEntity visitEntity = null;
        try {
            visitEntity = getByIp(ip);
            if (null == visitEntity) {
                visitEntity = new IpVisitEntity();
                visitEntity.setIp(ip);
                visitEntity.setTag(tag);
                visitEntity.setDescription(description);
                visitEntity.setCreateTime(new Date());
                visitEntity.setCount(0L);
                visitEntity.setToday(0L);
                insert(visitEntity);
            }
        } catch (Exception e) {
            //do nothing
        }
        return visitEntity;
    }

    @Override
    protected void save(String key, RData rData) {
        baseMapper.count(key, rData.getUserAgent(), rData.getHost(), rData.getUri(), rData.getRefer(), rData.getCount());
    }

    protected void clear() {
        baseMapper.clear();
    }

    @Override
    protected boolean has(String key) {
        return hasIp(key);
    }

    @Override
    protected boolean create() {
        return true;
    }

    @Override
    protected void create(String key) {
        create(key, "", "");
    }
}
