(function (window) {
    'use strict';

    function trim(value) {
        return (value || '').trim();
    }

    function readCtx(cfg) {
        cfg = cfg || window.__AUTH_FLOW_CONFIG__ || {};
        if (cfg.ctx) {
            return cfg.ctx;
        }
        if (window.AuthPage && window.AuthPage.readCtx) {
            return window.AuthPage.readCtx();
        }
        return window.__AUTH_CTX__ || '';
    }

    function browserOrigin(ctx) {
        ctx = ctx || readCtx();
        return window.location.origin + ctx;
    }

    function randomState(prefix) {
        prefix = prefix || 'af';
        return prefix + '_' + Math.random().toString(36).slice(2, 12);
    }

    function resolveQueryParam(names) {
        var qs = new URLSearchParams(window.location.search || '');
        if (!names || !names.length) {
            return '';
        }
        for (var i = 0; i < names.length; i++) {
            var value = qs.get(names[i]);
            if (value) {
                return value;
            }
        }
        return '';
    }

    function resolveInputValue(inputId) {
        if (!inputId) {
            return '';
        }
        var input = document.getElementById(inputId);
        return input ? trim(input.value) : '';
    }

    function buildOauthAuthorizeUrl(options) {
        options = options || {};
        var ctx = readCtx(options);
        var clientId = options.clientId;
        var redirectUri = options.redirectUri || (browserOrigin(ctx) + '/client/oauth2/callback');
        var scope = options.scope || 'basic';
        var state = options.state || randomState('oauth');
        return browserOrigin(ctx) + '/oauth2/authorize?response_type=code&client_id=' + encodeURIComponent(clientId)
            + '&redirect_uri=' + encodeURIComponent(redirectUri)
            + '&scope=' + encodeURIComponent(scope)
            + '&state=' + encodeURIComponent(state);
    }

    function buildOpenAsAuthorizeUrl(options) {
        options = options || {};
        var ctx = readCtx(options);
        var appId = options.appId;
        var redirectUri = options.redirectUri;
        var scope = options.scope || 'basic';
        var state = options.state || randomState('opl');
        var url = browserOrigin(ctx) + '/open/oauth2/authorize?app_id=' + encodeURIComponent(appId)
            + '&redirect_uri=' + encodeURIComponent(redirectUri)
            + '&response_type=code&scope=' + encodeURIComponent(scope)
            + '&state=' + encodeURIComponent(state);
        return url;
    }

    function buildOpenAuthorizeEntryUrl(options) {
        options = options || {};
        var ctx = readCtx(options);
        var appId = options.appId;
        var state = options.state || randomState('open');
        return browserOrigin(ctx) + '/open/oauth2/opc/authorize?appId=' + encodeURIComponent(appId)
            + '&state=' + encodeURIComponent(state);
    }

    function buildOauthLoginEntryUrl(options) {
        options = options || {};
        var ctx = readCtx(options);
        var clientId = options.clientId;
        var callback = options.callback;
        var url = browserOrigin(ctx) + '/oauth2/login?client_id=' + encodeURIComponent(clientId);
        if (callback) {
            url += '&callback=' + encodeURIComponent(callback);
        }
        return url;
    }

    function bindOAuthLoginEntryButton(options) {
        options = options || {};
        var btn = document.getElementById(options.buttonId || 'btnAuthorize');
        if (!btn) {
            return;
        }
        btn.addEventListener('click', function () {
            var cfg = window.__AUTH_FLOW_CONFIG__ || {};
            var clientId = trim(options.clientId || cfg.clientId)
                || resolveQueryParam(['clientId', 'client_id'])
                || resolveInputValue(options.inputId || 'clientIdInput');
            if (!clientId) {
                if (typeof options.onMissingClientId === 'function') {
                    options.onMissingClientId();
                } else {
                    alert('无法发起授权，请从应用内重新进入');
                }
                return;
            }
            var sameInstance = options.sameInstance;
            if (sameInstance === undefined || sameInstance === null) {
                sameInstance = cfg.sameInstance !== false;
            }
            if (!sameInstance) {
                window.location.href = buildOauthLoginEntryUrl({
                    ctx: readCtx(cfg),
                    clientId: clientId,
                    callback: trim(options.callback || cfg.callback)
                });
                return;
            }
            var redirectUri = trim(options.redirectUri || cfg.redirectUri)
                || (browserOrigin(readCtx(cfg)) + '/client/oauth2/callback');
            window.location.href = buildOauthAuthorizeUrl({
                ctx: readCtx(cfg),
                clientId: clientId,
                redirectUri: redirectUri,
                scope: options.scope || cfg.scope,
                state: options.state
            });
        });
    }

    function bindOpenLoginEntryButton(options) {
        options = options || {};
        var btn = document.getElementById(options.buttonId || 'btnAuthorize');
        if (!btn) {
            return;
        }
        btn.addEventListener('click', function () {
            var cfg = window.__AUTH_FLOW_CONFIG__ || {};
            var appId = trim(options.appId || cfg.appId)
                || resolveQueryParam(['appId', 'app_id'])
                || resolveInputValue(options.inputId || 'appIdInput');
            if (!appId) {
                if (typeof options.onMissingAppId === 'function') {
                    options.onMissingAppId();
                } else {
                    alert('无法发起授权，请从应用内重新进入');
                }
                return;
            }
            window.location.href = buildOpenAuthorizeEntryUrl({
                ctx: readCtx(cfg),
                appId: appId,
                state: options.state
            });
        });
    }

    function syncAuthorizeCallback(form, url) {
        if (!form) {
            return;
        }
        url = url || window.location.href.split('#')[0];
        var cbInput = form.querySelector('input[name=callback]');
        if (cbInput) {
            cbInput.value = url;
        }
        return url;
    }

    function beforeAuthorizeLoginSubmit(event, options) {
        options = options || {};
        var form = event && event.target;
        syncAuthorizeCallback(form, options.callbackUrl);
        if (options.validate && typeof options.validate === 'function') {
            return options.validate(event) !== false;
        }
        return true;
    }

    function submitAuthorizeConsent(options) {
        options = options || {};
        if (!options.loggedIn) {
            if (typeof options.onError === 'function') {
                options.onError('请先在左侧完成登录');
            }
            return false;
        }
        if (!options.consented) {
            if (typeof options.onError === 'function') {
                options.onError('请勾选授权协议后再确认');
            }
            return false;
        }
        var formId = options.formId || (options.mode === 'opl' ? 'oplApproveForm' : 'oauthApproveForm');
        var form = document.getElementById(formId);
        if (form) {
            form.submit();
            return true;
        }
        return false;
    }

    function absoluteAppUrl(path, options) {
        options = options || {};
        var ctx = readCtx(options);
        if (!path) {
            return '#';
        }
        if (/^https?:\/\//i.test(path)) {
            return path;
        }
        var normalized = path.charAt(0) === '/' ? path : '/' + path;
        return ctx + normalized;
    }

    function isOauthLoginEntryUrl(url) {
        if (!url) {
            return false;
        }
        try {
            var u = new URL(url, window.location.origin);
            var pathname = (u.pathname || '').toLowerCase();
            return pathname.endsWith('/oauth2/login') || pathname.endsWith('/open/oauth2/login');
        } catch (e) {
            return /\/oauth2\/login(?:\?|$)/i.test(url) || /\/open\/oauth2\/login(?:\?|$)/i.test(url);
        }
    }

    function resolveAuthProviderCallback(provider, options) {
        options = options || {};
        var ctx = readCtx(options);
        var safeOauthCallback = options.safeOauthCallback || '';
        var loginPage = ctx + '/login';
        if (provider && provider.sameInstance) {
            return loginPage;
        }
        if (safeOauthCallback) {
            return safeOauthCallback;
        }
        var href = window.location.href.split('#')[0];
        if (isOauthLoginEntryUrl(href)) {
            return loginPage;
        }
        return href;
    }

    function buildAuthProviderUrl(provider, options) {
        options = options || {};
        if (!provider || !provider.loginUrl) {
            return '#';
        }
        var url = absoluteAppUrl(provider.loginUrl, options);
        var callback = encodeURIComponent(resolveAuthProviderCallback(provider, options));
        return url + (url.indexOf('?') >= 0 ? '&' : '?') + 'callback=' + callback;
    }

    window.AuthFlow = {
        readCtx: readCtx,
        browserOrigin: browserOrigin,
        randomState: randomState,
        resolveQueryParam: resolveQueryParam,
        buildOauthAuthorizeUrl: buildOauthAuthorizeUrl,
        buildOauthLoginEntryUrl: buildOauthLoginEntryUrl,
        buildOpenAsAuthorizeUrl: buildOpenAsAuthorizeUrl,
        buildOpenAuthorizeEntryUrl: buildOpenAuthorizeEntryUrl,
        bindOAuthLoginEntryButton: bindOAuthLoginEntryButton,
        bindOpenLoginEntryButton: bindOpenLoginEntryButton,
        syncAuthorizeCallback: syncAuthorizeCallback,
        beforeAuthorizeLoginSubmit: beforeAuthorizeLoginSubmit,
        submitAuthorizeConsent: submitAuthorizeConsent,
        absoluteAppUrl: absoluteAppUrl,
        isOauthLoginEntryUrl: isOauthLoginEntryUrl,
        resolveAuthProviderCallback: resolveAuthProviderCallback,
        buildAuthProviderUrl: buildAuthProviderUrl
    };
})(window);
