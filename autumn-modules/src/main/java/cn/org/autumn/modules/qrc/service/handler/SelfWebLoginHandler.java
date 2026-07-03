package cn.org.autumn.modules.qrc.service.handler;

import cn.org.autumn.modules.qrc.dto.ConfirmResult;
import cn.org.autumn.modules.qrc.dto.CreateContext;
import cn.org.autumn.modules.qrc.model.Intent;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.service.ScanTicketService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.model.UserContext;
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
    private SysUserService sysUserService;

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

    @Override
    public ConfirmResult onConfirm(TicketSnapshot ticket, UserContext scanner) throws Exception {
        SysUserEntity user = sysUserService.getByUuid(scanner.getUuid());
        if (user == null || user.getStatus() < 1) {
            throw new cn.org.autumn.exception.CodeException("用户不可用", 8623);
        }
        String exchangeToken = scanTicketService.createExchangeToken(user.getUuid(), ticket.getUuid());
        return ConfirmResult.ofExchange(exchangeToken);
    }
}
