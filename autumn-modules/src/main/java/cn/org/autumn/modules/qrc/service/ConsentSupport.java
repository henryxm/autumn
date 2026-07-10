package cn.org.autumn.modules.qrc.service;

import cn.org.autumn.auth.scope.AuthScopeSet;
import cn.org.autumn.auth.scope.AuthTrack;
import cn.org.autumn.modules.auth.support.AuthScopeSupport;
import cn.org.autumn.modules.qrc.model.TicketPayloads;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.spi.ConsentProvider;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConsentSupport {

    @Autowired(required = false)
    private List<ConsentProvider> consentProviders = Collections.emptyList();

    @Autowired
    private AuthScopeSupport authScopeSupport;

    public List<String> describeScopes(TicketSnapshot ticket) {
        if (ticket == null) {
            return Collections.emptyList();
        }
        Map<String, String> payload = TicketPayloads.map(ticket);
        if (payload.isEmpty()) {
            return Collections.emptyList();
        }
        String clientId = payload.get("clientId");
        if (consentProviders != null) {
            for (ConsentProvider provider : consentProviders) {
                if (provider != null && provider.supports(clientId, ticket.getIntent())) {
                    List<String> labels = provider.describeScopes(ticket, payload);
                    if (labels != null && !labels.isEmpty()) {
                        return labels;
                    }
                }
            }
        }
        String scope = payload.get("scope");
        if (StringUtils.isBlank(scope)) {
            return Collections.emptyList();
        }
        AuthTrack track = resolveTrack(clientId, payload);
        return authScopeSupport.labels(track, scope);
    }

    private AuthTrack resolveTrack(String clientId, Map<String, String> payload) {
        if (payload != null && payload.containsKey("appId")) {
            return AuthTrack.OPL;
        }
        return AuthTrack.OAUTH;
    }
}
