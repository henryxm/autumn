package cn.org.autumn.modules.install;

import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.modules.install.dto.InstallConnectionForm;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/install/api")
@ConditionalOnProperty(prefix = "autumn.install", name = "mode", havingValue = "true")
public class InstallWizardApiController {

    private final InstallWizardService installWizardService;

    public InstallWizardApiController(InstallWizardService installWizardService) {
        this.installWizardService = installWizardService;
    }

    @GetMapping("/types")
    public Map<String, Object> types() {
        List<Map<String, String>> list = new ArrayList<>();
        for (DatabaseType t : DatabaseType.values()) {
            if (t == DatabaseType.OTHER) {
                continue;
            }
            Map<String, String> row = new LinkedHashMap<>();
            row.put("id", t.name());
            row.put("label", label(t));
            row.put("defaultDriver", InstallJdbcHelper.defaultDriver(t));
            row.put("defaultPort", String.valueOf(InstallJdbcHelper.defaultPort(t)));
            row.put("supportsEmbeddedFile", String.valueOf(InstallJdbcHelper.supportsEmbeddedFile(t)));
            row.put("supportsEmbeddedMemory", String.valueOf(InstallJdbcHelper.supportsEmbeddedMemory(t)));
            row.put("databaseFieldHintRemote", InstallJdbcHelper.databaseFieldHint(t, InstallJdbcHelper.ConnectionMode.REMOTE));
            row.put("databaseFieldHintFile", InstallJdbcHelper.databaseFieldHint(t, InstallJdbcHelper.ConnectionMode.EMBEDDED_FILE));
            row.put("databaseFieldHintMemory", InstallJdbcHelper.databaseFieldHint(t, InstallJdbcHelper.ConnectionMode.EMBEDDED_MEMORY));
            list.add(row);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("types", list);
        return out;
    }

    private static String label(DatabaseType t) {
        return t.name().charAt(0) + t.name().substring(1).toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    @PostMapping("/agree")
    public Map<String, Object> agree(HttpSession session) {
        installWizardService.setAgreed(session);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        return out;
    }

    @PostMapping("/form")
    public Map<String, Object> saveForm(HttpSession session, @RequestBody InstallConnectionForm form) {
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            InstallJdbcHelper.resolve(form);
            installWizardService.saveForm(session, form);
            out.put("ok", true);
        } catch (Exception e) {
            out.put("ok", false);
            out.put("error", e.getMessage());
        }
        return out;
    }

    @PostMapping("/check")
    public Map<String, Object> check(HttpSession session) {
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            Map<String, Object> detail = installWizardService.testAndDescribePrivileges(session);
            out.put("ok", true);
            out.putAll(detail);
        } catch (Throwable e) {
            out.put("ok", false);
            out.put("error", rootMessage(e));
        }
        return out;
    }

    @GetMapping("/meta")
    public Map<String, Object> meta(HttpSession session) {
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            out.put("ok", true);
            out.putAll(installWizardService.readMetadata(session));
        } catch (Throwable e) {
            out.put("ok", false);
            out.put("error", rootMessage(e));
        }
        return out;
    }

    @PostMapping("/finish")
    public Map<String, Object> finish(HttpSession session) {
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            return installWizardService.finalizeInstall(session);
        } catch (Throwable e) {
            out.put("ok", false);
            out.put("error", rootMessage(e));
            return out;
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable c = e;
        while (c.getCause() != null && c.getCause() != c) {
            c = c.getCause();
        }
        String m = c.getMessage();
        return m != null ? m : c.getClass().getSimpleName();
    }
}
