package cn.org.autumn.modules.bot.service;

import cn.org.autumn.exception.CodeException;
import cn.org.autumn.site.BotHandlerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 机器人头像：对象存储 uuid → 展示 URL（经 {@link cn.org.autumn.config.BotIconUrlHandler} SPI）。
 * <p>
 * 基础框架不内嵌对象存储；业务引入 {@code minclouds-file-starter} 后由
 * {@code BotIconUrlObjectStorageHandler} 提供实现。无 Handler 时无法解析 iconId。
 */
@Component
public class RobotIconUrlHelper {

    /** 与历史文案一致：有 Handler 但对象不可访问时抛出 */
    public static final String FAIL_MSG =
            "图片地址解析失败，请确认已上传且为可访问对象（bizType=im_group_info，accessLevel=PUBLIC）";

    public static final String NO_OSS_MSG =
            "对象存储未接入，无法解析 iconId；请引入 minclouds-file-starter";

    @Autowired
    private BotHandlerFactory botHandlerFactory;

    public boolean hasResolver() {
        return botHandlerFactory.hasIconUrlHandler();
    }

    public String resolveAccessUrl(String objectUuid) {
        if (StringUtils.isBlank(objectUuid)) {
            return "";
        }
        return botHandlerFactory.resolveIconUrl(objectUuid);
    }

    public String requireAccessUrl(String objectUuid) throws CodeException {
        if (StringUtils.isBlank(objectUuid)) {
            throw new CodeException(FAIL_MSG);
        }
        if (!hasResolver()) {
            throw new CodeException(NO_OSS_MSG);
        }
        String url = resolveAccessUrl(objectUuid);
        if (StringUtils.isBlank(url)) {
            throw new CodeException(FAIL_MSG);
        }
        return url;
    }
}
