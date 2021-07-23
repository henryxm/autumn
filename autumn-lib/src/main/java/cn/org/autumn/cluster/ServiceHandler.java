package cn.org.autumn.cluster;

import java.net.URI;

public interface ServiceHandler {
    default URI uri() {
        return null;
    }
}
