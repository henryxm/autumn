package cn.org.autumn.modules.opc.service;

import cn.org.autumn.modules.opc.dto.OpcBindAdminView;
import cn.org.autumn.modules.opc.dto.OpcBindManageView;
import cn.org.autumn.utils.PageUtils;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

public class ConnectBindManageServiceTest {

    private ConnectBindManageService connectBindManageService;

    @Before
    public void setUp() {
        connectBindManageService = new ConnectBindManageService();
    }

    @Test
    public void pageBindsManageViews_stripsTechnicalFieldsForUser() {
        ConnectBindManageService spy = Mockito.spy(connectBindManageService);
        OpcBindAdminView adminRow = new OpcBindAdminView();
        adminRow.setId(1L);
        adminRow.setAppName("Demo App");
        adminRow.setNickname("Nick");
        adminRow.setUser("uuid-user");
        adminRow.setOpenId("o_test");
        PageUtils page = new PageUtils(Collections.singletonList(adminRow), 1, 10, 1);
        Mockito.doReturn(page).when(spy).pageBindsForViewer(Mockito.eq("viewer-1"), Mockito.eq(false), Mockito.anyMap());

        PageUtils result = spy.pageBindsManageViews("viewer-1", false, new LinkedHashMap<String, Object>());

        Assert.assertEquals(1, result.getList().size());
        OpcBindManageView view = (OpcBindManageView) result.getList().get(0);
        Assert.assertEquals("Demo App", view.getAppName());
        Assert.assertEquals("Nick", view.getAccountLabel());
        Assert.assertNull(view.getTechnical());
    }

    @Test
    public void pageBindsManageViews_includesTechnicalForAdmin() {
        ConnectBindManageService spy = Mockito.spy(connectBindManageService);
        OpcBindAdminView adminRow = new OpcBindAdminView();
        adminRow.setId(2L);
        adminRow.setAppName("Admin App");
        adminRow.setUsername("admin");
        adminRow.setOpenId("o_admin");
        adminRow.setUser("uuid-admin");
        PageUtils page = new PageUtils(Collections.singletonList(adminRow), 1, 10, 1);
        Mockito.doReturn(page).when(spy).pageBindsForViewer(Mockito.eq("admin-1"), Mockito.eq(true), Mockito.anyMap());

        PageUtils result = spy.pageBindsManageViews("admin-1", true, new LinkedHashMap<String, Object>());

        OpcBindManageView view = (OpcBindManageView) result.getList().get(0);
        Assert.assertNotNull(view.getTechnical());
        Assert.assertEquals("o_admin", view.getTechnical().getOpenId());
    }
}
