package cn.org.autumn.cluster;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(ServiceHandler.class)
public interface ServiceHandler {
    default URI uri() {
        return null;
    }
}
