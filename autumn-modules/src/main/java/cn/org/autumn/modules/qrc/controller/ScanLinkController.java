package cn.org.autumn.modules.qrc.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.model.Response;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.service.ScanTicketService;
import cn.org.autumn.modules.spm.interceptor.SpmInterceptor;
import cn.org.autumn.modules.usr.interceptor.AuthorizationInterceptor;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * QR 内容 {@code /qrc/v1/t/{uuid}} 的轻量解析端点。
 * APP 通常本地解析 URL；本接口用于校验票据存在并返回公开字段，不暴露 OAuth 密钥或 token。
 */
@Slf4j
@RestController
@RequestMapping("/qrc/v1/t")
@SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
public class ScanLinkController {

    @Autowired
    private ScanTicketService scanTicketService;

    @GetMapping("/{uuid}")
    public Response<Map<String, String>> resolve(@PathVariable("uuid") String uuid) {
        try {
            TicketSnapshot ticket = scanTicketService.getRequired(uuid);
            Map<String, String> body = new HashMap<>();
            body.put("uuid", ticket.getUuid());
            body.put("intent", ticket.getIntent());
            body.put("status", ticket.getStatus());
            return Response.ok(body);
        } catch (Exception e) {
            return Response.error(e);
        }
    }
}
