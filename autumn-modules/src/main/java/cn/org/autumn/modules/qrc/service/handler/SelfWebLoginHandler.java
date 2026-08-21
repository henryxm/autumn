package cn.org.autumn.modules.qrc.service.handler;

import cn.org.autumn.modules.qrc.dto.ConfirmResult;
import cn.org.autumn.modules.qrc.dto.CreateContext;
import cn.org.autumn.modules.qrc.model.Intent;
import cn.org.autumn.modules.qrc.model.TicketPayloads;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.service.ScanTicketService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.usr.entity.UserTokenEntity;
import cn.org.autumn.modules.usr.service.UserTokenService;
import cn.org.autumn.model.UserContext;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class SelfWebLoginHandler implements IntentHandler {

    @Autowired
    @Lazy
    private ScanTicketService scanTicketService;

    @Autowired
    @Lazy
    private UserTokenService userTokenService;

    @Override
    public String intent() {
        return Intent.SELF_WEB_LOGIN;
    }

    @Override
    public void onCreate(TicketSnapshot ticket, CreateContext ctx) {
    }

    @Override
    public void onScan(TicketSnapshot ticket, UserContext scanner) {
    }

    /**
     * Web：exchange → Shiro Cookie；PC/桌面：result.accessToken → usr_user_token（对齐 /login/uniform）。
     */
    @Override
    public ConfirmResult onConfirm(TicketSnapshot ticket, UserContext scanner) throws Exception {
        SysUserEntity user = scanTicketService.requireActiveUser(scanner == null ? null : scanner.getUuid());
        String exchangeToken = scanTicketService.createExchangeToken(user.getUuid(), ticket.getUuid());
        ConfirmResult result = ConfirmResult.ofExchange(exchangeToken);
        String deviceUuid = TicketPayloads.get(ticket, "loginDeviceUuid");
        UserTokenEntity tokenEntity = userTokenService.createApiToken(user.getUuid(), deviceUuid);
        if (tokenEntity != null && StringUtils.isNotBlank(tokenEntity.getToken())) {
            Map<String, String> map = result.getResult();
            if (map == null) {
                map = new HashMap<>();
                result.setResult(map);
            }
            map.put("accessToken", tokenEntity.getToken());
            map.put("userUuid", user.getUuid());
        }
        return result;
    }
}
