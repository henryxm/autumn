# 站点门户与合规配置（Site Portal）

> 登录/注册/法律页统一展示品牌、版权、版本、中国大陆备案/许可及法律链接。配置键：`SITE_PORTAL_CONFIG`。

## 1. 管理入口

| 入口 | 路径 |
|------|------|
| 专用管理页 | 后台 **系统管理 → 站点门户**（`siteportal.html`） |
| 参数管理 | **系统管理 → 参数管理** 中 JSON 键 `SITE_PORTAL_CONFIG` |

API（系统管理员）：`GET/POST /sys/site-portal/*`

管理页嵌入后台 iframe 时使用 `statics/js/autumn-iframe-auto-height.js`（设置 `__AUTUMN_IFRAME_AUTO_HEIGHT__`），由框架页滚动而非页内滚动条。页面逻辑见 `statics/js/siteportal-admin.js`（备案预览请求 300ms 防抖）。

## 2. 配置结构 `SitePortalConfig`

| 分区 | 字段 | 说明 |
|------|------|------|
| `branding` | `siteName` / `tagline` / `logoUrl` / `logoAlt` | 登录页顶部品牌；`siteName` 为空时回退 `LOADING_THEME.brand` |
| `meta` | `copyrightHolder` / `copyrightYearStart` / `copyrightYearEnd` / `versionLabel` | 页脚版权与版本；未填不展示；**仅填版本号**时不生成 © 行，版本单独展示 |
| `legalLinks` | `privacyUrl` / `termsUrl` / `aboutUrl` / `helpUrl` / `contactUrl` | 法律与帮助链接；相对路径自动加 `contextPath` |
| `filings[]` | `type` / `number` / `prefix` / `suffix` / `url` / `showIcon` | 备案/许可列表；`prefix`/`suffix` 可选，仅展示不参与链接；`number` 为空则整项不展示 |
| 开关 | `syncLoadingTheme` | 保存时同步品牌名与 Logo 到 `LOADING_THEME` |

### 2.1 法律链接默认值

| 字段 | 默认路径 |
|------|----------|
| `privacyUrl` | `/user/privacy.html` |
| `termsUrl` | `/user/service.html` |
| `aboutUrl` | 空（不展示；可设为 `/user/about.html`） |

### 2.2 备案类型与默认跳转

默认 URL 指向**主管部门门户**，便于访客了解监管来源；各地公示系统、审图号查询等请手填「跳转地址」。展示文案可配合「前缀/后缀」拆分（如 `增值电信业务经营许可证:` + `川B2-xxx`）。

#### 基础网信

| type | 说明 | 默认 URL |
|------|------|----------|
| `icp` | ICP 备案 | `https://beian.miit.gov.cn/` |
| `psb` | 公安联网备案 / 网安备 | `...?recordcode={recordcode}`（见下） |
| `app_icp` | App 备案 | `https://beian.miit.gov.cn/` |
| `algorithm` | 互联网信息服务算法备案 | `https://beian.cac.gov.cn/` |

#### 电信与经营许可

| type | 说明 | 默认 URL |
|------|------|----------|
| `icp_license` | ICP 经营许可证 | `https://www.miit.gov.cn/` |
| `telecom` | 增值电信业务经营许可证 | `https://www.miit.gov.cn/` |

#### 广电与网络文化

| type | 说明 | 默认 URL |
|------|------|----------|
| `network_culture` | 网络文化经营许可证 | `https://www.mct.gov.cn/` |
| `audiovisual` | 信息网络传播视听节目许可证 | `https://www.nrta.gov.cn/` |
| `broadcast_tv` | 广播电视节目制作经营许可证 | `https://www.nrta.gov.cn/` |
| `online_performance` | 网络表演 / 互联网直播服务 | `https://www.mct.gov.cn/` |
| `game_isbn` | 网络游戏版号（ISBN） | `https://www.nppa.gov.cn/` |

#### 新闻出版

| type | 说明 | 默认 URL |
|------|------|----------|
| `news_license` | 互联网新闻信息服务许可证 | `https://www.nia.gov.cn/` |
| `publish_license` | 网络出版服务许可证 | `https://www.nppa.gov.cn/` |

#### 药品与医疗器械

| type | 说明 | 默认 URL |
|------|------|----------|
| `drug_info` | 互联网药品信息服务资格证 | `https://www.nmpa.gov.cn/` |
| `medical_device` | 医疗器械经营/网络交易服务相关 | `https://www.nmpa.gov.cn/` |
| `medical_ad` | 医疗广告审查证明 | `https://www.nhc.gov.cn/` |

#### 食品相关

| type | 说明 | 默认 URL |
|------|------|----------|
| `food_license` | 食品经营许可证 | `https://www.samr.gov.cn/` |
| `food_production` | 食品生产许可证 | `https://www.samr.gov.cn/` |
| `special_food` | 特殊食品（保健食品等）注册/备案 | `https://www.samr.gov.cn/` |

#### 其他

| type | 说明 | 默认 URL |
|------|------|----------|
| `internet_religion` | 互联网宗教信息服务许可证 | `https://www.sara.gov.cn/` |
| `map_approval` | 地图审图号 / 测绘资质 | `https://www.mnr.gov.cn/` |
| `custom` | 自定义 | 须手填 `url` |

公安备案 `{recordcode}` 由 `SitePortalConfig.extractPsbRecordCode(number)` 从文案中提取连续数字；仅 `psb` 类型支持页脚盾牌图标。

## 3. 前台展示

模板片段：

- `_auth_site_header.html` — 登录页顶部（OAuth 授权模式保留「应用授权」标题）
- `_auth_site_footer.html` — 版权、备案、法律链接
- `_auth_site_psb_icon.html` — 公安备案官方图标（`statics/img/beian-psb.png`）
- `_auth_site_branding_icon.html` — 可复用 Logo 块

注入：`AuthPageAttributes.apply(..., SitePortalSupport)` → Model 属性 `siteBranding`、`siteMeta`、`siteFilings`、`siteLegalLinks` 等。

**规则**：字段为空则不渲染对应块。

## 4. 下游扩展法律链接（SPI）

实现 `cn.org.autumn.config.SiteLegalLinksHandler`（`@Component` + `@Order`）：

```java
@Component
@Order(100)
public class MyLegalLinks implements SiteLegalLinksHandler {
    @Override
    public String privacyUrl(SitePortalConfig config) {
        return "https://my.example.com/legal/privacy";
    }
}
```

`SiteLegalLinksFactory` 按 `@Order` 取**首个非空**返回值覆盖配置；仍空则用 `SitePortalConfig.legalLinks` 或框架默认路径。

## 5. 下游覆盖法律正文

| 方式 | 说明 |
|------|------|
| 模板覆盖 | 子项目提供同名 `user/privacy.html`、`user/service.html`、`user/about.html` |
| 外链 | 在站点门户配置或 SPI 中将链接指向外部 URL |

路由（`SysPageController`，Shiro `anon`）：`/user/privacy.html`、`/user/service.html`、`/user/about.html`

## 6. 相关代码

| 组件 | 路径 |
|------|------|
| 配置模型 | `autumn-lib/.../model/SitePortalConfig.java` |
| 解析 | `autumn-modules/.../site/SitePortalSupport.java` |
| 备案类型 | `autumn-lib/.../model/ComplianceFilingType.java` |
| SPI | `autumn-lib/.../config/SiteLegalLinksHandler.java` |
| 管理 API | `SitePortalAdminController` `/sys/site-portal` |
| 管理页 | `templates/siteportal.html` |
| 管理页脚本 | `statics/js/siteportal-admin.js` |
| iframe 撑高 | `statics/js/autumn-iframe-auto-height.js` + `statics/js/index.js` |

## 7. 与授权登录文档

双轨 OAuth/OPL 授权页同样使用 `_auth_site_header/footer`，见 `docs/AI_AUTH_LOGIN_MODES.md` §站点门户。
