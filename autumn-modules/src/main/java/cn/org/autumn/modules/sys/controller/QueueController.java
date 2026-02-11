package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.annotation.Endpoint;
import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.config.QueueConfig;
import cn.org.autumn.model.QueueMessage;
import cn.org.autumn.service.QueueService;
import cn.org.autumn.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Controller
@RequestMapping("/sys/queue")
@Endpoint(hidden = true)
@SkipInterceptor
public class QueueController {

    @Autowired
    private QueueService queueService;

    /**
     * 队列管理页面
     */
    @GetMapping("/")
    public String index() {
        return "queue";
    }

    /**
     * 获取所有队列列表
     */
    @GetMapping("/list")
    @ResponseBody
    public R list() {
        try {
            List<Map<String, Object>> queueList = queueService.getAllQueueInfo();
            return R.ok().put("list", queueList).put("total", queueList.size());
        } catch (Exception e) {
            log.error("获取队列列表失败", e);
            return R.error("获取队列列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取队列详情
     */
    @GetMapping("/info/{name}")
    @ResponseBody
    public R info(@PathVariable("name") String name) {
        try {
            Map<String, Object> info = queueService.getQueueInfo(name);
            if (info == null) {
                return R.error("队列不存在");
            }
            return R.ok().put("info", info);
        } catch (Exception e) {
            log.error("获取队列详情失败", e);
            return R.error("获取队列详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取队列消息列表（预览，不消费）
     */
    @GetMapping("/messages/{name}")
    @ResponseBody
    public R messages(@PathVariable("name") String name,
                      @RequestParam(value = "page", defaultValue = "1") int page,
                      @RequestParam(value = "limit", defaultValue = "20") int limit) {
        try {
            Map<String, Object> result = queueService.peekMessages(name, page, limit);
            return R.ok().put("list", result.get("list"))
                    .put("total", result.get("total"))
                    .put("page", page)
                    .put("limit", limit);
        } catch (Exception e) {
            log.error("获取队列消息失败", e);
            return R.error("获取队列消息失败: " + e.getMessage());
        }
    }

    /**
     * 发送消息到队列
     */
    @PostMapping("/send")
    @ResponseBody
    public R send(@RequestBody Map<String, Object> params) {
        try {
            String queueName = (String) params.get("queueName");
            Object message = params.get("message");
            Integer priority = (Integer) params.get("priority");
            Long delay = params.get("delay") != null ? ((Number) params.get("delay")).longValue() : null;

            if (queueName == null || queueName.isEmpty()) {
                return R.error("队列名称不能为空");
            }
            if (message == null) {
                return R.error("消息内容不能为空");
            }

            boolean success;
            if (delay != null && delay > 0) {
                success = queueService.sendDelay(queueName, message, delay);
            } else if (priority != null) {
                success = queueService.sendPriority(queueName, message, priority);
            } else {
                success = queueService.send(queueName, message);
            }

            if (success) {
                return R.ok("消息发送成功");
            } else {
                return R.error("消息发送失败");
            }
        } catch (Exception e) {
            log.error("发送消息失败", e);
            return R.error("发送消息失败: " + e.getMessage());
        }
    }

    /**
     * 批量发送消息
     */
    @PostMapping("/sendBatch")
    @ResponseBody
    public R sendBatch(@RequestBody Map<String, Object> params) {
        try {
            String queueName = (String) params.get("queueName");
            @SuppressWarnings("unchecked")
            List<Object> messages = (List<Object>) params.get("messages");

            if (queueName == null || queueName.isEmpty()) {
                return R.error("队列名称不能为空");
            }
            if (messages == null || messages.isEmpty()) {
                return R.error("消息列表不能为空");
            }

            int successCount = queueService.sendBatch(queueName, messages);
            return R.ok("批量发送完成").put("total", messages.size()).put("success", successCount);
        } catch (Exception e) {
            log.error("批量发送消息失败", e);
            return R.error("批量发送消息失败: " + e.getMessage());
        }
    }

    /**
     * 消费消息
     */
    @PostMapping("/consume/{name}")
    @ResponseBody
    public R consume(@PathVariable("name") String name,
                     @RequestParam(value = "count", defaultValue = "1") int count) {
        try {
            List<Object> messages = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                QueueMessage<?> message = queueService.poll(name, 100, TimeUnit.MILLISECONDS);
                if (message != null) {
                    messages.add(message);
                } else {
                    break;
                }
            }
            return R.ok().put("list", messages).put("count", messages.size());
        } catch (Exception e) {
            log.error("消费消息失败", e);
            return R.error("消费消息失败: " + e.getMessage());
        }
    }

    /**
     * 清空队列
     */
    @PostMapping("/clear/{name}")
    @ResponseBody
    public R clear(@PathVariable("name") String name) {
        try {
            long sizeBefore = queueService.size(name);
            queueService.clear(name);
            return R.ok("队列已清空").put("cleared", sizeBefore);
        } catch (Exception e) {
            log.error("清空队列失败", e);
            return R.error("清空队列失败: " + e.getMessage());
        }
    }

    /**
     * 删除队列
     */
    @PostMapping("/delete/{name}")
    @ResponseBody
    public R delete(@PathVariable("name") String name) {
        try {
            queueService.delete(name);
            return R.ok("队列已删除");
        } catch (Exception e) {
            log.error("删除队列失败", e);
            return R.error("删除队列失败: " + e.getMessage());
        }
    }

    /**
     * 创建队列
     */
    @PostMapping("/create")
    @ResponseBody
    public R create(@RequestBody Map<String, Object> params) {
        try {
            String name = (String) params.get("name");
            String type = (String) params.getOrDefault("type", "MEMORY");
            Integer capacity = (Integer) params.getOrDefault("capacity", 10000);
            Integer maxRetries = (Integer) params.getOrDefault("maxRetries", 3);
            Boolean deadLetterEnabled = (Boolean) params.getOrDefault("deadLetterEnabled", false);

            if (name == null || name.isEmpty()) {
                return R.error("队列名称不能为空");
            }

            QueueConfig.QueueType queueType;
            try {
                queueType = QueueConfig.QueueType.valueOf(type);
            } catch (Exception e) {
                return R.error("无效的队列类型: " + type);
            }

            QueueConfig config = QueueConfig.builder()
                    .name(name)
                    .type(Object.class)
                    .queueType(queueType)
                    .capacity(capacity)
                    .retries(maxRetries)
                    .deadLetter(deadLetterEnabled)
                    .build();

            queueService.register(config);
            return R.ok("队列创建成功");
        } catch (Exception e) {
            log.error("创建队列失败", e);
            return R.error("创建队列失败: " + e.getMessage());
        }
    }

    /**
     * 获取消费者状态
     */
    @GetMapping("/consumers")
    @ResponseBody
    public R consumers() {
        try {
            List<Map<String, Object>> consumers = queueService.getConsumerStatus();
            return R.ok().put("list", consumers);
        } catch (Exception e) {
            log.error("获取消费者状态失败", e);
            return R.error("获取消费者状态失败: " + e.getMessage());
        }
    }

    /**
     * 启动消费者
     */
    @PostMapping("/consumer/start/{name}")
    @ResponseBody
    public R startConsumer(@PathVariable("name") String name,
                           @RequestParam(value = "concurrency", defaultValue = "1") int concurrency) {
        try {
            queueService.start(name, concurrency);
            return R.ok("消费者已启动");
        } catch (Exception e) {
            log.error("启动消费者失败", e);
            return R.error("启动消费者失败: " + e.getMessage());
        }
    }

    /**
     * 停止消费者
     */
    @PostMapping("/consumer/stop/{name}")
    @ResponseBody
    public R stopConsumer(@PathVariable("name") String name) {
        try {
            queueService.stop(name);
            return R.ok("消费者已停止");
        } catch (Exception e) {
            log.error("停止消费者失败", e);
            return R.error("停止消费者失败: " + e.getMessage());
        }
    }

    /**
     * 获取队列统计信息
     */
    @GetMapping("/stats")
    @ResponseBody
    public R stats() {
        try {
            Map<String, Object> stats = queueService.getStatistics();
            return R.ok().put("stats", stats);
        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            return R.error("获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取队列类型列表
     */
    @GetMapping("/types")
    @ResponseBody
    public R types() {
        List<Map<String, Object>> types = new ArrayList<>();
        for (QueueConfig.QueueType type : QueueConfig.QueueType.values()) {
            Map<String, Object> item = new HashMap<>();
            item.put("value", type.name());
            item.put("label", getTypeLabel(type));
            item.put("description", getTypeDescription(type));
            types.add(item);
        }
        return R.ok().put("list", types);
    }

    /**
     * 更新队列配置
     */
    @PostMapping("/config/update/{name}")
    @ResponseBody
    public R updateConfig(@PathVariable("name") String name, @RequestBody Map<String, Object> params) {
        try {
            QueueConfig config = queueService.getConfig(name);
            if (config == null) {
                return R.error("队列不存在: " + name);
            }
            boolean success = queueService.updateConfig(name, params);
            if (success) {
                return R.ok("配置更新成功");
            } else {
                return R.error("配置更新失败");
            }
        } catch (Exception e) {
            log.error("更新队列配置失败", e);
            return R.error("更新队列配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取队列完整配置详情
     */
    @GetMapping("/config/{name}")
    @ResponseBody
    public R getConfig(@PathVariable("name") String name) {
        try {
            QueueConfig config = queueService.getConfig(name);
            if (config == null) {
                return R.error("队列不存在: " + name);
            }
            Map<String, Object> configInfo = new LinkedHashMap<>();
            configInfo.put("name", config.getName());
            configInfo.put("queueType", config.getQueueType().name());
            configInfo.put("capacity", config.getCapacity());
            configInfo.put("timeout", config.getTimeout());
            configInfo.put("timeoutUnit", config.getUnit().name());
            configInfo.put("expire", config.getExpire());
            configInfo.put("retries", config.getRetries());
            configInfo.put("concurrency", config.getConcurrency());
            configInfo.put("idleTime", config.getIdleTime());
            configInfo.put("idleUnit", config.getIdleUnit().name());
            configInfo.put("auto", config.isAuto());
            configInfo.put("deadLetter", config.isDeadLetter());
            configInfo.put("deadLetterQueue", config.getDeadName());
            configInfo.put("persistent", config.isPersistent());
            configInfo.put("history", config.getHistory());
            configInfo.put("messageType", config.getType() != null ? config.getType().getSimpleName() : "Object");
            configInfo.put("processedCount", queueService.getProcessedCount(name));
            return R.ok().put("config", configInfo);
        } catch (Exception e) {
            log.error("获取队列配置失败", e);
            return R.error("获取队列配置失败: " + e.getMessage());
        }
    }

    // ==================== 历史消息接口 ====================

    /**
     * 获取历史消息列表
     */
    @GetMapping("/history/{name}")
    @ResponseBody
    public R history(@PathVariable("name") String name,
                     @RequestParam(value = "page", defaultValue = "1") int page,
                     @RequestParam(value = "limit", defaultValue = "20") int limit) {
        try {
            Map<String, Object> result = queueService.getHistoryMessages(name, page, limit);
            return R.ok().put("list", result.get("list"))
                    .put("total", result.get("total"))
                    .put("page", page)
                    .put("limit", limit);
        } catch (Exception e) {
            log.error("获取历史消息失败", e);
            return R.error("获取历史消息失败: " + e.getMessage());
        }
    }

    /**
     * 删除单条历史消息
     */
    @PostMapping("/history/delete/{name}/{messageId}")
    @ResponseBody
    public R deleteHistory(@PathVariable("name") String name,
                           @PathVariable("messageId") String messageId) {
        try {
            boolean success = queueService.deleteHistoryMessage(name, messageId);
            if (success) {
                return R.ok("历史消息已删除");
            } else {
                return R.error("消息不存在或已被删除");
            }
        } catch (Exception e) {
            log.error("删除历史消息失败", e);
            return R.error("删除历史消息失败: " + e.getMessage());
        }
    }

    /**
     * 清空历史消息
     */
    @PostMapping("/history/clear/{name}")
    @ResponseBody
    public R clearHistory(@PathVariable("name") String name) {
        try {
            int count = queueService.clearHistory(name);
            return R.ok("已清空 " + count + " 条历史消息").put("cleared", count);
        } catch (Exception e) {
            log.error("清空历史消息失败", e);
            return R.error("清空历史消息失败: " + e.getMessage());
        }
    }

    /**
     * 重试死信队列消息
     */
    @PostMapping("/deadletter/retry/{name}")
    @ResponseBody
    public R retryDeadLetter(@PathVariable("name") String name) {
        try {
            int count = queueService.retryDeadLetterMessages(name);
            return R.ok("已重试 " + count + " 条消息").put("count", count);
        } catch (Exception e) {
            log.error("重试死信消息失败", e);
            return R.error("重试死信消息失败: " + e.getMessage());
        }
    }

    /**
     * 移动消息到另一个队列
     */
    @PostMapping("/move")
    @ResponseBody
    public R moveMessages(@RequestBody Map<String, Object> params) {
        try {
            String sourceQueue = (String) params.get("sourceQueue");
            String targetQueue = (String) params.get("targetQueue");
            Integer count = (Integer) params.getOrDefault("count", 100);

            if (sourceQueue == null || targetQueue == null) {
                return R.error("源队列和目标队列不能为空");
            }

            int moved = queueService.moveMessages(sourceQueue, targetQueue, count);
            return R.ok("已移动 " + moved + " 条消息").put("moved", moved);
        } catch (Exception e) {
            log.error("移动消息失败", e);
            return R.error("移动消息失败: " + e.getMessage());
        }
    }

    private String getTypeLabel(QueueConfig.QueueType type) {
        switch (type) {
            case MEMORY:
                return "内存队列";
            case REDIS_LIST:
                return "Redis List队列";
            case REDIS_STREAM:
                return "Redis Stream队列";
            case DELAY:
                return "延迟队列";
            case PRIORITY:
                return "优先级队列";
            default:
                return type.name();
        }
    }

    private String getTypeDescription(QueueConfig.QueueType type) {
        switch (type) {
            case MEMORY:
                return "基于BlockingQueue，高性能，单机使用，重启后数据丢失";
            case REDIS_LIST:
                return "基于Redis List，支持分布式，持久化，简单队列";
            case REDIS_STREAM:
                return "基于Redis Stream，支持消费者组、消息确认、持久化";
            case DELAY:
                return "基于Redis Sorted Set，支持延迟执行、定时任务";
            case PRIORITY:
                return "基于PriorityBlockingQueue，支持按优先级消费";
            default:
                return "";
        }
    }
}
