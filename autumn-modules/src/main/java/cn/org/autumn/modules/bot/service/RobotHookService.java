package cn.org.autumn.modules.bot.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.config.QueueConfig;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.modules.bot.dao.RobotHookDao;
import cn.org.autumn.modules.bot.dto.RobotHookView;
import cn.org.autumn.modules.bot.entity.RobotEntity;
import cn.org.autumn.modules.bot.entity.RobotHookEntity;
import cn.org.autumn.modules.bot.model.RobotHookDispatchTask;
import cn.org.autumn.modules.bot.support.RobotHookHttp;
import cn.org.autumn.modules.bot.support.RobotHookSupport;
import cn.org.autumn.utils.Uuid;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@DependsOn("tableInit")
public class RobotHookService extends ModuleService<RobotHookDao, RobotHookEntity> {

    private static final String HOOK_DISPATCH_SUFFIX = "dispatch";
    private static final int HOOK_HTTP_TIMEOUT_MS = 8000;

    @Autowired
    @Lazy
    private RobotService robotService;

    @Autowired
    @Lazy
    private RobotQuotaService robotQuotaService;

    @Autowired
    private Gson gson;

    @Override
    public String ico() {
        return "fa-link";
    }

    @Override
    public <X> boolean isAutoQueue(String suffix, Class<X> clazz) {
        if (HOOK_DISPATCH_SUFFIX.equals(suffix) && RobotHookDispatchTask.class.equals(clazz))
            return true;
        return super.isAutoQueue(suffix, clazz);
    }

    @Override
    public <X> long getIdleTime(String suffix, Class<X> clazz) {
        if (HOOK_DISPATCH_SUFFIX.equals(suffix) && RobotHookDispatchTask.class.equals(clazz))
            return 120L;
        return super.getIdleTime(suffix, clazz);
    }

    @Override
    public <X> TimeUnit getIdleUnit(String suffix, Class<X> clazz) {
        if (HOOK_DISPATCH_SUFFIX.equals(suffix) && RobotHookDispatchTask.class.equals(clazz))
            return TimeUnit.SECONDS;
        return super.getIdleUnit(suffix, clazz);
    }

    @Override
    public <X> QueueConfig getQueueConfig(String suffix, Class<X> clazz) {
        if (HOOK_DISPATCH_SUFFIX.equals(suffix) && RobotHookDispatchTask.class.equals(clazz)) {
            QueueConfig base = super.getQueueConfig(suffix, clazz);
            int retries = robotQuotaService.getGlobal().getHookDispatchRetries();
            return QueueConfig.builder()
                    .name(base.getName())
                    .type(base.getType())
                    .auto(base.isAuto())
                    .idleTime(getIdleTime(suffix, clazz))
                    .idleUnit(getIdleUnit(suffix, clazz))
                    .retries(retries > 0 ? retries : 5)
                    .deadLetter(true)
                    .build();
        }
        return super.getQueueConfig(suffix, clazz);
    }

    @PostConstruct
    public void initHookDispatchQueue() {
        register(HOOK_DISPATCH_SUFFIX, RobotHookDispatchTask.class, this::consume);
    }

    public boolean dispatch(RobotHookDispatchTask task) {
        if (task == null || StringUtils.isBlank(task.getHook()))
            return false;
        return sendQueue(HOOK_DISPATCH_SUFFIX, RobotHookDispatchTask.class, task);
    }

    public RobotHookEntity getByUuid(String uuid) {
        if (StringUtils.isBlank(uuid))
            return null;
        return baseMapper.getByUuid(uuid);
    }

    public List<RobotHookEntity> listByRobot(String robot) {
        if (StringUtils.isBlank(robot))
            return null;
        return baseMapper.listByRobot(robot);
    }

    public List<RobotHookEntity> listActiveByRobot(String robot) {
        if (StringUtils.isBlank(robot))
            return null;
        return baseMapper.listActiveByRobot(robot);
    }

    public int countByRobot(String robot) {
        if (StringUtils.isBlank(robot))
            return 0;
        return baseMapper.countByRobot(robot);
    }

    public List<RobotHookView> listViewsForOwner(String robotUuid, String loginUuid) throws Exception {
        RobotEntity robot = robotService.getByUuid(robotUuid);
        robotService.assertOwner(robot, loginUuid);
        List<RobotHookEntity> hooks = listByRobot(robotUuid);
        List<RobotHookView> views = new ArrayList<>();
        if (hooks != null) {
            for (RobotHookEntity hook : hooks)
                views.add(RobotHookView.of(hook));
        }
        return views;
    }

    @Transactional(rollbackFor = Exception.class)
    public RobotHookView create(String robotUuid, String loginUuid, String name, String callbackUrl, String secret, String events, String description) throws Exception {
        RobotEntity robot = robotService.getByUuid(robotUuid);
        robotService.assertOwner(robot, loginUuid);
        if (!robot.isActive())
            throw new CodeException("机器人未启用");
        if (StringUtils.isBlank(name))
            throw new CodeException("名称不能为空");
        RobotHookSupport.validateCallbackUrl(callbackUrl);
        robotQuotaService.assertHookQuota(robotUuid);
        Date now = new Date();
        RobotHookEntity hook = new RobotHookEntity();
        hook.setUuid(Uuid.uuid());
        hook.setRobot(robotUuid);
        hook.setOwner(robot.getOwner());
        hook.setName(name);
        hook.setCallback(callbackUrl);
        hook.setSecret(StringUtils.isBlank(secret) ? generateHookSecret() : secret);
        hook.setEvents(StringUtils.isBlank(events) ? "*" : events);
        hook.setDescription(description);
        hook.setStatus(RobotHookEntity.STATUS_ACTIVE);
        hook.setCreateTime(now);
        hook.setUpdateTime(now);
        insert(hook);
        return RobotHookView.of(hook);
    }

    @Transactional(rollbackFor = Exception.class)
    public RobotHookView update(String hookUuid, String loginUuid, String name, String callbackUrl, String secret, String events, String description) throws Exception {
        RobotHookEntity hook = getByUuid(hookUuid);
        assertOwner(hook, loginUuid);
        if (StringUtils.isNotBlank(name))
            hook.setName(name);
        if (StringUtils.isNotBlank(callbackUrl)) {
            RobotHookSupport.validateCallbackUrl(callbackUrl);
            hook.setCallback(callbackUrl);
        }
        if (secret != null)
            hook.setSecret(secret);
        if (events != null)
            hook.setEvents(events);
        if (description != null)
            hook.setDescription(description);
        hook.setUpdateTime(new Date());
        updateById(hook);
        return RobotHookView.of(hook);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String hookUuid, String loginUuid) throws Exception {
        RobotHookEntity hook = getByUuid(hookUuid);
        assertOwner(hook, loginUuid);
        deleteById(hook.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteByRobot(String robotUuid) {
        if (StringUtils.isBlank(robotUuid))
            return;
        List<RobotHookEntity> hooks = listByRobot(robotUuid);
        if (hooks == null || hooks.isEmpty())
            return;
        for (RobotHookEntity hook : hooks)
            deleteById(hook.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void disable(String hookUuid, String loginUuid) throws Exception {
        RobotHookEntity hook = getByUuid(hookUuid);
        assertOwner(hook, loginUuid);
        hook.setStatus(RobotHookEntity.STATUS_DISABLED);
        hook.setUpdateTime(new Date());
        updateById(hook);
    }

    @Transactional(rollbackFor = Exception.class)
    public void enable(String hookUuid, String loginUuid) throws Exception {
        RobotHookEntity hook = getByUuid(hookUuid);
        assertOwner(hook, loginUuid);
        RobotEntity robot = robotService.getByUuid(hook.getRobot());
        if (robot == null || !robot.isActive())
            throw new CodeException("机器人未启用");
        hook.setStatus(RobotHookEntity.STATUS_ACTIVE);
        hook.setUpdateTime(new Date());
        updateById(hook);
    }

    private void assertOwner(RobotHookEntity hook, String loginUuid) throws Exception {
        if (hook == null)
            throw new CodeException("Hook不存在");
        if (StringUtils.isBlank(loginUuid) || !loginUuid.equals(hook.getOwner()))
            throw new CodeException("无权操作该Hook");
    }

    @Override
    protected <X> void onDeadMessage(String suffix, Class<X> type, X message) {
        if (HOOK_DISPATCH_SUFFIX.equals(suffix) && message instanceof RobotHookDispatchTask) {
            RobotHookDispatchTask task = (RobotHookDispatchTask) message;
            log.error("Hook callback moved to dead letter: hook={}, robot={}, event={}", task.getHook(), task.getRobot(), task.getEvent());
        } else {
            super.onDeadMessage(suffix, type, message);
        }
    }

    private String generateHookSecret() {
        return RandomStringUtils.randomAlphanumeric(32);
    }

    private boolean consume(RobotHookDispatchTask task) {
        if (task == null || StringUtils.isBlank(task.getHook()))
            return true;
        RobotHookEntity hook = getByUuid(task.getHook());
        if (hook == null || !hook.isActive() || StringUtils.isBlank(hook.getCallback()))
            return true;
        if (!RobotHookSupport.matchesEvent(hook.getEvents(), task.getEvent()))
            return true;
        try {
            Object data = RobotHookSupport.parseDataJson(gson, task.getData());
            String body = RobotHookSupport.formatCallbackBody(gson, task.getRobot(), task.getEvent(), task.getTimestamp(), data);
            String signature = RobotHookSupport.sign(hook.getSecret(), task.getTimestamp(), body);
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json;charset=UTF-8");
            headers.put(RobotHookSupport.headerEvent(), task.getEvent());
            headers.put(RobotHookSupport.headerTimestamp(), task.getTimestamp());
            headers.put(RobotHookSupport.headerSignature(), signature);
            boolean ok = RobotHookHttp.postJson(hook.getCallback(), body, headers, HOOK_HTTP_TIMEOUT_MS);
            if (!ok) {
                log.warn("Hook callback non-2xx: hook={}, event={}", hook.getUuid(), task.getEvent());
                return false;
            }
            Date now = new Date();
            hook.setLastInvokeTime(now);
            hook.setUpdateTime(now);
            updateById(hook);
            return true;
        } catch (Exception e) {
            log.warn("Hook callback failed: hook={}, event={}, {}", hook.getUuid(), task.getEvent(), e.getMessage());
            return false;
        }
    }
}
