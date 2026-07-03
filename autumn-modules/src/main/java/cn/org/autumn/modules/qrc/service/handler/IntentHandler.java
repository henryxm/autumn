package cn.org.autumn.modules.qrc.service.handler;

import cn.org.autumn.modules.qrc.dto.ConfirmResult;
import cn.org.autumn.modules.qrc.dto.CreateContext;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.model.UserContext;

public interface IntentHandler {
    String intent();

    void onCreate(TicketSnapshot ticket, CreateContext ctx) throws Exception;

    void onScan(TicketSnapshot ticket, UserContext scanner) throws Exception;

    ConfirmResult onConfirm(TicketSnapshot ticket, UserContext scanner) throws Exception;
}
