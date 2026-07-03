package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.client.dto.AuthClientBundle;
import cn.org.autumn.modules.client.dto.AuthClientSummary;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.entity.WebOauthCombineEntity;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.qrc.entity.ClientGrantEntity;
import cn.org.autumn.modules.qrc.service.ClientGrantService;
import cn.org.autumn.modules.qrc.service.ScanTicketService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Uuid;
import cn.org.autumn.validator.ValidatorUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthClientHubService {

    @Autowired
    private ClientDetailsService clientDetailsService;

    @Autowired
    private WebAuthenticationService webAuthenticationService;

    @Autowired
    private WebOauthCombineService webOauthCombineService;

    @Autowired
    private ClientGrantService clientGrantService;

    @Autowired
    private ScanTicketService scanTicketService;

    public PageUtils queryPage(Map<String, Object> params) {
        PageUtils page = clientDetailsService.queryPage(params, "create_time");
        List<?> rows = page.getList();
        List<AuthClientSummary> summaries = new ArrayList<>();
        if (rows != null) {
            for (Object row : rows) {
                if (row instanceof ClientDetailsEntity) {
                    summaries.add(toSummary((ClientDetailsEntity) row));
                }
            }
        }
        page.setList(summaries);
        return page;
    }

    public AuthClientBundle getBundle(String clientId) {
        if (StringUtils.isBlank(clientId)) {
            return null;
        }
        AuthClientBundle bundle = new AuthClientBundle();
        bundle.setClientId(clientId);
        bundle.setOauth(clientDetailsService.findByClientId(clientId));
        bundle.setWeb(webAuthenticationService.getByClientId(clientId));
        bundle.setQrc(clientGrantService.getOrDefault(clientId));
        bundle.setCombine(webOauthCombineService.getByClientId(clientId));
        return bundle;
    }

    public PageUtils queryTickets(String clientId, Map<String, Object> params) {
        if (StringUtils.isNotBlank(clientId)) {
            params.put("clientId", clientId);
        }
        return scanTicketService.queryPage(params, "created");
    }

    @Transactional(rollbackFor = Exception.class)
    public AuthClientBundle createClient(String clientId) {
        if (StringUtils.isBlank(clientId)) {
            throw new IllegalArgumentException("clientId不能为空");
        }
        if (clientDetailsService.findByClientId(clientId) != null) {
            throw new IllegalStateException("客户端已存在: " + clientId);
        }
        WebOauthCombineEntity combine = new WebOauthCombineEntity();
        combine.setClientId(clientId);
        if (!webOauthCombineService.insert(combine)) {
            throw new IllegalStateException("创建客户端失败");
        }
        ClientGrantEntity grant = new ClientGrantEntity();
        grant.setClientId(clientId);
        grant.setEnabled(true);
        clientGrantService.saveGrant(grant);
        return getBundle(clientId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveBundle(AuthClientBundle bundle) {
        if (bundle == null || StringUtils.isBlank(bundle.getClientId())) {
            throw new IllegalArgumentException("clientId不能为空");
        }
        String clientId = bundle.getClientId();
        ClientDetailsEntity oauth = bundle.getOauth();
        if (oauth != null) {
            oauth.setClientId(clientId);
            if (StringUtils.isBlank(oauth.getUuid())) {
                oauth.setUuid(Uuid.uuid());
            }
            if (oauth.getId() == null) {
                if (oauth.getCreateTime() == null) {
                    oauth.setCreateTime(new Date());
                }
                clientDetailsService.insert(oauth);
            } else {
                ValidatorUtils.validateEntity(oauth);
                clientDetailsService.updateAllColumnById(oauth);
            }
        }
        WebAuthenticationEntity web = bundle.getWeb();
        if (web != null) {
            web.setClientId(clientId);
            if (StringUtils.isBlank(web.getUuid())) {
                web.setUuid(Uuid.uuid());
            }
            if (web.getId() == null) {
                if (web.getCreateTime() == null) {
                    web.setCreateTime(new Date());
                }
                webAuthenticationService.insert(web);
            } else {
                ValidatorUtils.validateEntity(web);
                webAuthenticationService.updateAllColumnById(web);
            }
        }
        ClientGrantEntity qrc = bundle.getQrc();
        if (qrc != null) {
            qrc.setClientId(clientId);
            clientGrantService.saveGrant(qrc);
        }
        ensureCombine(clientId, oauth, web);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteByClientId(String clientId) {
        if (StringUtils.isBlank(clientId)) {
            return;
        }
        ClientGrantEntity grant = clientGrantService.getByClientId(clientId);
        if (grant != null && grant.getId() != null) {
            clientGrantService.deleteById(grant.getId());
        }
        WebOauthCombineEntity combine = webOauthCombineService.getByClientId(clientId);
        if (combine != null && combine.getId() != null) {
            webOauthCombineService.deleteById(combine.getId());
        }
        WebAuthenticationEntity web = webAuthenticationService.getByClientId(clientId);
        if (web != null && web.getId() != null) {
            webAuthenticationService.deleteById(web.getId());
        }
        ClientDetailsEntity oauth = clientDetailsService.findByClientId(clientId);
        if (oauth != null && oauth.getId() != null) {
            clientDetailsService.deleteById(oauth.getId());
        }
    }

    private void ensureCombine(String clientId, ClientDetailsEntity oauth, WebAuthenticationEntity web) {
        if (oauth == null || web == null) {
            return;
        }
        WebOauthCombineEntity combine = webOauthCombineService.getByClientId(clientId);
        if (combine == null) {
            combine = new WebOauthCombineEntity();
            combine.setClientId(clientId);
            combine.setClientDetailsUuid(oauth.getUuid());
            combine.setWebAuthenticationUuid(web.getUuid());
            combine.setCreateTime(new Date());
            if (StringUtils.isBlank(combine.getUuid())) {
                combine.setUuid(Uuid.uuid());
            }
            webOauthCombineService.insert(combine);
            return;
        }
        combine.setClientDetailsUuid(oauth.getUuid());
        combine.setWebAuthenticationUuid(web.getUuid());
        combine.setUpdateTime(new Date());
        webOauthCombineService.updateAllColumnById(combine);
    }

    private AuthClientSummary toSummary(ClientDetailsEntity oauth) {
        AuthClientSummary summary = new AuthClientSummary();
        summary.setOauthId(oauth.getId());
        summary.setClientId(oauth.getClientId());
        summary.setClientName(oauth.getClientName());
        summary.setTrusted(oauth.getTrusted());
        summary.setArchived(oauth.getArchived());
        summary.setCreateTime(oauth.getCreateTime());
        summary.setHasWeb(webAuthenticationService.getByClientId(oauth.getClientId()) != null);
        summary.setHasCombine(webOauthCombineService.getByClientId(oauth.getClientId()) != null);
        ClientGrantEntity grant = clientGrantService.getByClientId(oauth.getClientId());
        summary.setHasQrc(grant != null);
        summary.setQrcEnabled(grant != null && grant.isEnabled());
        return summary;
    }
}
