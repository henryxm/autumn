package cn.org.autumn.modules.ops.controller;

import cn.org.autumn.model.SecurityOpsCertConfig;
import cn.org.autumn.modules.ops.service.OpsCertService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * T0 发现落地页：浏览器打开 {@code GET /ops/discover} 时提示用超然信；
 * App 拦截同 path 后本地 probe，不依赖本页内容。
 */
@RestController
@RequestMapping("/ops")
public class OpsDiscoverController {

    @Autowired
    private OpsCertService opsCertService;

    @GetMapping(value = "/discover", produces = MediaType.TEXT_HTML_VALUE)
    public String discover() {
        SecurityOpsCertConfig cfg = opsCertService.getConfig();
        String siteId = cfg == null ? "" : StringUtils.defaultString(cfg.getSiteId());
        String fp = cfg == null ? "" : StringUtils.defaultString(cfg.getFingerprint());
        String siteLine = StringUtils.isBlank(siteId)
                ? ""
                : "<p class=\"meta\">站点标识：" + escape(siteId) + "</p>";
        String fpLine = StringUtils.isBlank(fp)
                ? ""
                : "<p class=\"meta\">指纹摘要：" + escape(shorten(fp, 16)) + "…</p>";
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                  <meta charset="utf-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1"/>
                  <title>Autumn 运维发现</title>
                  <style>
                    body{font-family:system-ui,-apple-system,sans-serif;margin:0;padding:48px 24px;
                      background:#f6f8fb;color:#1a1a1a;text-align:center}
                    .card{max-width:420px;margin:0 auto;background:#fff;border-radius:12px;
                      padding:28px 24px;box-shadow:0 1px 4px rgba(0,0,0,.08)}
                    h1{font-size:1.25rem;margin:0 0 12px}
                    p{line-height:1.55;margin:0 0 10px}
                    .meta{color:#666;font-size:.85rem}
                  </style>
                </head>
                <body>
                  <div class="card">
                    <h1>Autumn 运维发现</h1>
                    <p>请使用<strong>超然信</strong>扫码或打开此链接，以自动探测并管理本站运维证书。</p>
                    <p class="meta">浏览器无法完成证书入驻；本页不含任何密钥。</p>
                    %s
                    %s
                  </div>
                </body>
                </html>
                """.formatted(siteLine, fpLine);
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String shorten(String s, int n) {
        if (s.length() <= n) {
            return s;
        }
        return s.substring(0, n);
    }
}
