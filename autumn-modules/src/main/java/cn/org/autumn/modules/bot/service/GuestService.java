package cn.org.autumn.modules.bot.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.bot.dao.GuestDao;
import cn.org.autumn.modules.bot.entity.GuestEntity;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.sys.service.UuidNamespaceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

/**
 * 临时用户：归属机器人模块，独立于 {@code sys_user}，不创建资料、角色或登录口令。
 */
@Slf4j
@Service
public class GuestService extends ModuleService<GuestDao, GuestEntity> implements LoopJob.OneHour {

    @Autowired
    @Lazy
    private UuidNamespaceService uuidNamespaceService;

    public GuestEntity getByUuid(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return null;
        }
        return baseMapper.getByUuid(uuid);
    }

    /**
     * 未过期且状态正常的临时用户；供统一资料查询与访客判定。
     */
    public GuestEntity getActive(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return null;
        }
        GuestEntity guest = getCache(uuid);
        if (guest == null || !guest.isActive()) {
            return null;
        }
        return guest;
    }

    /**
     * 同一访客标识在未过期时复用；过期则删旧行并分配新的全局 uuid。
     * 不调用账号工厂，避免写入正式用户资料。
     * 不加外层事务：并发插入撞唯一键时要能继续查已有行，外层事务会把冲突标成必须回滚。
     */
    public GuestEntity getOrCreate(String guestKey, String nickname, long ttlHours) {
        if (StringUtils.isBlank(guestKey)) {
            throw new IllegalArgumentException("访客标识不能为空");
        }
        guestKey = guestKey.trim();
        if (guestKey.length() < 8 || guestKey.length() > 64) {
            throw new IllegalArgumentException("访客标识无效");
        }
        long hours = Math.max(1, ttlHours);
        GuestEntity existing = baseMapper.getByGuestKey(guestKey);
        if (existing != null) {
            if (existing.isActive()) {
                return touch(existing, hours);
            }
            if (existing.getStatus() == GuestEntity.STATUS_DISABLED && existing.getExpireTime() != null && existing.getExpireTime().after(new Date())) {
                return existing;
            }
            removeById(existing.getId());
            removeCache(existing.getUuid());
        }
        return insertNew(guestKey, nickname, hours);
    }

    /**
     * 按小时清掉已过期行，只删除本表。
     */
    @Override
    public void onOneHour() {
        int removed = purgeExpired();
        if (removed > 0) {
            log.info("清理过期临时用户: count={}", removed);
        }
    }

    public int purgeExpired() {
        return baseMapper.deleteExpired(new Date());
    }

    private GuestEntity touch(GuestEntity guest, long ttlHours) {
        Date now = new Date();
        guest.setExpireTime(plusHours(now, ttlHours));
        guest.setLastSeenTime(now);
        guest.setUpdateTime(now);
        updateById(guest);
        removeCache(guest.getUuid());
        return guest;
    }

    private GuestEntity insertNew(String guestKey, String nickname, long ttlHours) {
        Date now = new Date();
        GuestEntity guest = new GuestEntity();
        guest.setUuid(uuidNamespaceService.allocate());
        guest.setGuestKey(guestKey);
        guest.setNickname(trimNickname(nickname));
        guest.setStatus(GuestEntity.STATUS_ACTIVE);
        guest.setExpireTime(plusHours(now, ttlHours));
        guest.setCreateTime(now);
        guest.setUpdateTime(now);
        guest.setLastSeenTime(now);
        try {
            insert(guest);
        } catch (DuplicateKeyException e) {
            GuestEntity again = baseMapper.getByGuestKey(guestKey);
            if (again != null && again.isActive()) {
                return touch(again, ttlHours);
            }
            log.warn("创建临时用户冲突: guestKey={}", guestKey);
            throw e;
        }
        return guest;
    }

    private static String trimNickname(String nickname) {
        if (StringUtils.isBlank(nickname)) {
            return "";
        }
        String name = nickname.trim();
        if (name.length() > 100) {
            return name.substring(0, 100);
        }
        return name;
    }

    private static Date plusHours(Date from, long hours) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(from);
        calendar.add(Calendar.HOUR_OF_DAY, (int) Math.min(hours, Integer.MAX_VALUE));
        return calendar.getTime();
    }
}
