package cn.org.autumn.modules.qrc.service.handler;

import cn.org.autumn.modules.qrc.dto.ConfirmResult;
import cn.org.autumn.modules.qrc.dto.CreateContext;
import cn.org.autumn.modules.qrc.model.Intent;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.service.ScanTicketService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.model.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class SelfWebLoginHandler implements IntentHandler {

    @Autowired
    @Lazy
    private ScanTicketService scanTicketService;

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
        SysUserEntity user = scanTicketService.requireActiveUser(scanner == null ? null : scanner.getUuid());
        String exchangeToken = scanTicketService.createExchangeToken(user.getUuid(), ticket.getUuid());
        return ConfirmResult.ofExchange(exchangeToken);
    }
}
