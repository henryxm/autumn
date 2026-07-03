package cn.org.autumn.modules.qrc.service;

import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.spi.ConsentProvider;
import java.util.ArrayList;
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

    public List<String> describeScopes(TicketSnapshot ticket) {
        if (ticket == null || ticket.getPayload() == null) {
            return Collections.emptyList();
        }
        Map<String, String> payload = ticket.getPayload();
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
        List<String> labels = new ArrayList<>();
        for (String part : scope.split("\\s+")) {
            if (StringUtils.isNotBlank(part)) {
                labels.add(part.trim());
            }
        }
        return labels;
    }
}
