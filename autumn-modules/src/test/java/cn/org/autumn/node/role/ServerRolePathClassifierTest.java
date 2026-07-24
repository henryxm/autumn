package cn.org.autumn.node.role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ServerRolePathClassifierTest {

    @Test
    void classifiesApiWebDownloadAndOpen() {
        ServerRolePathClassifier c = new ServerRolePathClassifier();
        assertEquals(ServerRole.CAP_API_HTTP, c.requiredCapability("/v1/chat/completions"));
        assertEquals(ServerRole.CAP_FILE_DOWNLOAD, c.requiredCapability("/statics/a.js"));
        assertEquals(ServerRole.CAP_WEB_UI, c.requiredCapability("/modules/bigmodel/pages/x"));
        assertNull(c.requiredCapability("/client/health"));
        assertNull(c.requiredCapability("/install"));
    }
}
