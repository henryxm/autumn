package cn.org.autumn.modules.bot.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.modules.bot.dao.RobotTokenDao;
import cn.org.autumn.modules.bot.dto.RobotTokenItemView;
import cn.org.autumn.modules.bot.dto.RobotTokenListResult;
import cn.org.autumn.modules.bot.entity.RobotEntity;
import cn.org.autumn.modules.bot.entity.RobotTokenEntity;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.utils.Uuid;
import lombok.Getter;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class RobotTokenService extends ModuleService<RobotTokenDao, RobotTokenEntity> {

    public static final String TOKEN_PREFIX = "rbt_";
    public static final int TOKEN_PREFIX_LEN = 12;
    private static final String TOKEN_HASH_SALT = "autumn-robot-token";
    private static final int DEFAULT_EXPIRE_DAYS = 365;

    @Autowired
    @Lazy
    private RobotService robotService;

    @Autowired
    @Lazy
    private RobotQuotaService robotQuotaService;

    @Getter
    public static class ValidateResult {
        private final RobotTokenEntity token;
        private final RobotEntity robot;

        public ValidateResult(RobotTokenEntity token, RobotEntity robot) {
            this.token = token;
            this.robot = robot;
        }
    }

    @Override
    public String ico() {
        return "fa-key";
    }

    public String hashToken(String plainToken) {
        return ShiroUtils.sha256(plainToken, TOKEN_HASH_SALT);
    }

    public String generatePlainToken() {
        return TOKEN_PREFIX + RandomStringUtils.randomAlphanumeric(40);
    }

    public String tokenPrefixOf(String plainToken) {
        if (StringUtils.isBlank(plainToken))
            return "";
        return plainToken.length() <= TOKEN_PREFIX_LEN ? plainToken : plainToken.substring(0, TOKEN_PREFIX_LEN);
    }

    public int countByRobot(String robotUuid) {
        if (StringUtils.isBlank(robotUuid))
            return 0;
        return baseMapper.countByRobot(robotUuid);
    }

    public int countActiveByRobot(String robotUuid) {
        if (StringUtils.isBlank(robotUuid))
            return 0;
        return baseMapper.countActiveByRobot(robotUuid);
    }

    public RobotTokenListResult listActiveResult(String robotUuid, String loginUuid) throws Exception {
        RobotEntity robot = robotService.getByUuid(robotUuid);
        robotService.assertOwner(robot, loginUuid);
        List<RobotTokenEntity> tokens = baseMapper.listActiveByRobot(robotUuid);
        List<RobotTokenItemView> views = new ArrayList<>();
        if (tokens != null) {
            for (RobotTokenEntity token : tokens)
                views.add(RobotTokenItemView.of(token));
        }
        RobotTokenListResult result = RobotTokenListResult.of(views);
        result.setUsedRows(countActiveByRobot(robotUuid));
        result.setMaxRows(robotQuotaService.effectiveMaxTokens(robotUuid));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public String insertToken(String robotUuid, Integer expireDays) {
        String plain = generatePlainToken();
        Date now = new Date();
        RobotTokenEntity entity = new RobotTokenEntity();
        entity.setUuid(Uuid.uuid());
        entity.setRobot(robotUuid);
        entity.setToken(hashToken(plain));
        entity.setTokenPrefix(tokenPrefixOf(plain));
        entity.setStatus(RobotTokenEntity.STATUS_ACTIVE);
        entity.setUpdateTime(now);
        entity.setLastUsedTime(now);
        int days = expireDays == null ? DEFAULT_EXPIRE_DAYS : expireDays;
        if (days > 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(now);
            calendar.add(Calendar.DAY_OF_YEAR, days);
            entity.setExpireTime(calendar.getTime());
        }
        insert(entity);
        return plain;
    }

    public void assertTokenQuotaAvailable(String robotUuid) throws CodeException {
        int max = robotQuotaService.effectiveMaxTokens(robotUuid);
        int count = countActiveByRobot(robotUuid);
        if (count >= max)
            throw new CodeException("已达令牌数量上限:" + max);
    }

    @Transactional(rollbackFor = Exception.class)
    public String createToken(String robotUuid, Integer expireDays) throws CodeException {
        assertTokenQuotaAvailable(robotUuid);
        return insertToken(robotUuid, expireDays);
    }

    /**
     * 删除该机器人最旧的一条已作废（status=0）令牌行，释放配额槽位。
     * 使用 SELECT + 按主键 DELETE，避免 H2 等库不支持 {@code DELETE ... ORDER BY ... LIMIT}。
     */
    private int deleteOldestRevokedByRobot(String robotUuid) {
        RobotTokenEntity oldest = baseMapper.getOldestRevokedByRobot(robotUuid);
        if (oldest == null || oldest.getId() == null)
            return 0;
        return deleteById(oldest.getId()) ? 1 : 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public String rotateIssue(String robotUuid, Integer expireDays) throws CodeException {
        int max = robotQuotaService.effectiveMaxTokens(robotUuid);
        while (countActiveByRobot(robotUuid) >= max) {
            if (deleteOldestRevokedByRobot(robotUuid) > 0)
                continue;
            throw new CodeException("已达令牌数量上限:" + max + "，请先作废无用令牌");
        }
        return insertToken(robotUuid, expireDays);
    }

    @Transactional(rollbackFor = Exception.class)
    public void revoke(String tokenUuid, String loginUuid) throws Exception {
        RobotTokenEntity token = baseMapper.getByUuid(tokenUuid);
        if (token == null)
            throw new CodeException("令牌不存在");
        RobotEntity robot = robotService.getByUuid(token.getRobot());
        robotService.assertOwner(robot, loginUuid);
        deleteById(token.getId());
    }

    public ValidateResult validate(String plainToken) {
        if (StringUtils.isBlank(plainToken) || !plainToken.startsWith(TOKEN_PREFIX))
            return null;
        String prefix = tokenPrefixOf(plainToken);
        List<RobotTokenEntity> candidates = baseMapper.listByTokenPrefix(prefix);
        if (candidates == null || candidates.isEmpty())
            return null;
        String hash = hashToken(plainToken);
        RobotTokenEntity matched = null;
        for (RobotTokenEntity candidate : candidates) {
            if (hash.equals(candidate.getToken()) && candidate.isActive()) {
                matched = candidate;
                break;
            }
        }
        if (matched == null)
            return null;
        if (matched.getExpireTime() != null && matched.getExpireTime().before(new Date()))
            return null;
        RobotEntity robot = robotService.getByUuid(matched.getRobot());
        if (robot == null)
            return null;
        return new ValidateResult(matched, robot);
    }

    public void touchLastUsed(RobotTokenEntity tokenEntity) {
        if (tokenEntity == null)
            return;
        Date now = new Date();
        tokenEntity.setLastUsedTime(now);
        tokenEntity.setUpdateTime(now);
        updateById(tokenEntity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void revokeByRobot(String robotUuid) {
        if (StringUtils.isBlank(robotUuid))
            return;
        baseMapper.revokeByRobot(robotUuid, new Date());
    }
}
