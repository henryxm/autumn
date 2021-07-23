package cn.org.autumn.cluster;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@ConditionalOnMissingBean(ServiceHandler.class)
public interface ServiceHandler {
    default URI uri() {
        return null;
    }
}
