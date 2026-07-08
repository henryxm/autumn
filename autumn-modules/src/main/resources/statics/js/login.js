(function () {
    'use strict';

    var cfg = window.__LOGIN_CONFIG__ || {};
    var ctx = cfg.ctx || (window.AuthPage && AuthPage.readCtx()) || '';
    var oauthLogin = !!cfg.oauthLogin;
    var oauthAuthorize = !!cfg.oauthAuthorize;
    var oplAuthorize = !!cfg.oplAuthorize;
    var authorizeMode = oauthAuthorize || oplAuthorize;
    var authorizeLoggedIn = !!cfg.authorizeLoggedIn;
    var oauthDenyRedirect = cfg.oauthDenyRedirect || '';
    var safeOauthCallback = cfg.safeOauthCallback || '';
    var serverUuid = cfg.serverUuid || '';
    var serverQrUrl = cfg.serverQrUrl || '';
    var serverPollIntervalMs = cfg.serverPollIntervalMs || 2000;
    var skipAutologinCookie = cfg.skipAutologinCookie || 'autumn_skip_autologin';
    var devAutologinEnabled = !!cfg.devAutologinEnabled;
    var authFlowOpts = { ctx: ctx, safeOauthCallback: safeOauthCallback };

    function shouldSkipAutologin() {
        try {
            return document.cookie.indexOf(skipAutologinCookie + '=1') >= 0;
        } catch (e) {
            return false;
        }
    }

    function buildProviderUrl(provider) {
        if (window.AuthFlow && window.AuthFlow.buildAuthProviderUrl) {
            return window.AuthFlow.buildAuthProviderUrl(provider, authFlowOpts);
        }
        if (!provider || !provider.loginUrl) {
            return '#';
        }
        var url = provider.loginUrl.indexOf('http') === 0 ? provider.loginUrl : ctx + (provider.loginUrl.charAt(0) === '/' ? provider.loginUrl : '/' + provider.loginUrl);
        var loginPage = ctx + '/login';
        var callbackTarget = provider.sameInstance ? loginPage : (safeOauthCallback || loginPage);
        return url + (url.indexOf('?') >= 0 ? '&' : '?') + 'callback=' + encodeURIComponent(callbackTarget);
    }

    var vm = new Vue({
        el: '#rrapp',
        data: {
            loginTab: 'account',
            username: '',
            password: '',
            captcha: '',
            mobile: '',
            mobilePassword: '',
            mobileCaptcha: '',
            error: false,
            errorMsg: '',
            src: window.AuthPage ? AuthPage.captchaUrl(ctx) : ctx + '/captcha.jpg',
            qrStatus: authorizeMode ? '正在加载二维码...' : '切换到扫码登录后加载二维码',
            qrPhase: 'pending',
            scannerDisplayName: '',
            scannerIconUrl: '',
            qrcUuid: '',
            qrcPollTimer: null,
            oauthCallback: '',
            authorizeLoggedIn: authorizeLoggedIn,
            consentChecked: true,
            oauthDenyRedirect: oauthDenyRedirect,
            authLoginVisible: false,
            authLoginProviders: [],
            authLoginDefaultIcon: '/statics/img/auth-login-default.svg'
        },
        computed: {
            mobileClean: function () {
                return (this.mobile || '').replace(/\s/g, '');
            },
            canConfirmConsent: function () {
                return this.authorizeLoggedIn && this.consentChecked;
            },
            consentTip: function () {
                if (!this.authorizeLoggedIn) {
                    return '请先在左侧完成登录';
                }
                if (!this.consentChecked) {
                    return '请勾选授权协议';
                }
                return '确认后将返回应用并继续操作';
            }
        },
        beforeCreate: function () {
            if (self != top) {
                top.location.href = self.location.href;
            }
        },
        methods: {
            switchTab: function (tab) {
                if (this.loginTab === tab) {
                    return;
                }
                this.error = false;
                this.loginTab = tab;
                if (tab === 'qr') {
                    if (authorizeMode && serverQrUrl) {
                        this.initAuthorizeQr();
                    } else {
                        this.startQrLogin();
                    }
                } else if (this.qrcPollTimer) {
                    clearInterval(this.qrcPollTimer);
                    this.qrcPollTimer = null;
                }
                if (!oauthLogin && (tab === 'account' || tab === 'phone')) {
                    this.refreshCode();
                }
            },
            loadAuthLoginProviders: function () {
                if (authorizeMode) {
                    return;
                }
                var self = this;
                $.ajax({
                    type: 'GET',
                    url: ctx + '/auth/login/providers',
                    dataType: 'json',
                    success: function (res) {
                        if (!res || res.code !== 0 || !res.data || !res.data.visible) {
                            return;
                        }
                        self.authLoginVisible = true;
                        self.authLoginProviders = res.data.providers || [];
                        self.authLoginDefaultIcon = res.data.defaultIconUrl || '/statics/img/auth-login-default.svg';
                    }
                });
            },
            buildProviderUrl: function (provider) {
                return buildProviderUrl(provider);
            },
            providerIcon: function (provider) {
                var icon = provider && provider.iconUrl ? provider.iconUrl.trim() : '';
                if (icon) {
                    if (/^https?:\/\//i.test(icon) || icon.charAt(0) === '/') {
                        return icon.charAt(0) === '/' ? ctx + icon : icon;
                    }
                    return ctx + '/' + icon;
                }
                var fallback = this.authLoginDefaultIcon || '/statics/img/auth-login-default.svg';
                return fallback.charAt(0) === '/' ? ctx + fallback : fallback;
            },
            syncOauthCallback: function () {
                if (authorizeMode) {
                    this.oauthCallback = window.location.href.split('#')[0];
                    return;
                }
                this.oauthCallback = safeOauthCallback || '';
            },
            beforeAuthorizeLogin: function (e) {
                if (authorizeMode) {
                    if (this.loginTab === 'phone') {
                        if (!/^1\d{10}$/.test(this.mobileClean)) {
                            e.preventDefault();
                            this.showError('请输入正确的手机号');
                            return false;
                        }
                        if (!this.mobilePassword) {
                            e.preventDefault();
                            this.showError('请输入密码');
                            return false;
                        }
                    }
                    var url = window.location.href.split('#')[0];
                    this.oauthCallback = url;
                    var form = e.target;
                    if (form && form.querySelector) {
                        var cbInput = form.querySelector('input[name=callback]');
                        if (cbInput) {
                            cbInput.value = url;
                        }
                    }
                    return true;
                }
                this.syncOauthCallback();
                if (this.loginTab === 'phone') {
                    return this.beforePhoneOauthSubmit(e);
                }
            },
            beforeOauthAuthorizeLogin: function (e) {
                return this.beforeAuthorizeLogin(e);
            },
            beforePhoneOauthSubmit: function (e) {
                this.syncOauthCallback();
                if (!/^1\d{10}$/.test(this.mobileClean)) {
                    e.preventDefault();
                    this.showError('请输入正确的手机号');
                    return false;
                }
                if (!this.mobilePassword) {
                    e.preventDefault();
                    this.showError('请输入密码');
                    return false;
                }
            },
            refreshQrLogin: function () {
                if (authorizeMode) {
                    window.location.reload();
                    return;
                }
                this.resetQrScannedState();
                this.startQrLogin();
            },
            resetQrScannedState: function () {
                this.qrPhase = 'pending';
                this.scannerDisplayName = '';
                this.scannerIconUrl = '';
            },
            resolveScannerIconUrl: function (icon) {
                if (!icon) {
                    return '';
                }
                if (/^https?:\/\//i.test(icon)) {
                    return icon;
                }
                if (icon.charAt(0) === '/') {
                    return ctx + icon;
                }
                return ctx + '/' + icon;
            },
            applyScannerBrief: function (brief) {
                if (!brief) {
                    return;
                }
                this.scannerDisplayName = brief.displayName || '';
                this.scannerIconUrl = this.resolveScannerIconUrl(brief.icon);
                this.qrPhase = 'scanned';
            },
            handleQrTerminalStatus: function (status) {
                if (status === 'DENIED' || status === 'CANCELLED' || status === 'EXPIRED') {
                    this.resetQrScannedState();
                    this.qrStatus = '扫码已取消或过期，请刷新重试';
                }
            },
            initAuthorizeQr: function () {
                var self = this;
                self.resetQrScannedState();
                if (self.qrcPollTimer) {
                    clearInterval(self.qrcPollTimer);
                    self.qrcPollTimer = null;
                }
                if (!serverUuid || !serverQrUrl) {
                    self.qrStatus = '二维码加载失败，请刷新页面重试';
                    return;
                }
                self.qrcUuid = serverUuid;
                self.renderQrCode(serverQrUrl);
                self.qrStatus = '等待扫码...';
                self.qrcPollTimer = setInterval(function () { self.pollQrStatus(); }, serverPollIntervalMs);
            },
            submitAuthorizeConsent: function () {
                if (!this.authorizeLoggedIn) {
                    this.showError('请先在左侧完成登录');
                    return;
                }
                if (!this.consentChecked) {
                    this.showError('请勾选授权协议后再确认');
                    return;
                }
                var formId = oplAuthorize ? 'oplApproveForm' : 'oauthApproveForm';
                var form = document.getElementById(formId);
                if (form) {
                    form.submit();
                }
            },
            submitOAuthConsent: function () {
                this.submitAuthorizeConsent();
            },
            completeAuthorizeExchange: function (exchange) {
                var self = this;
                $.ajax({
                    type: 'POST',
                    url: ctx + '/qrc/scanticket/web/session/exchange',
                    contentType: 'application/json',
                    data: JSON.stringify({ data: { exchange: exchange } }),
                    dataType: 'json',
                    success: function (result) {
                        if (result.code === 0) {
                            self.qrStatus = '登录成功，请确认授权';
                            window.location.reload();
                        } else {
                            self.qrStatus = result.msg || '登录失败';
                        }
                    },
                    error: function () {
                        self.qrStatus = '网络异常，请刷新二维码重试';
                    }
                });
            },
            refreshCode: function () {
                this.src = window.AuthPage ? AuthPage.captchaUrl(ctx) : ctx + '/captcha.jpg?t=' + Date.now();
            },
            renderQrCode: function (text) {
                var box = document.getElementById('loginQrcodeBox');
                if (!box || !text) {
                    return;
                }
                var renderOpts = {
                    fallbackSize: 200,
                    colorDark: '#1a1d26',
                    colorLight: '#ffffff'
                };
                if (window.AutumnQrc && typeof AutumnQrc.renderInto === 'function') {
                    AutumnQrc.renderInto(box, text, renderOpts);
                    return;
                }
                box.innerHTML = '';
                if (typeof QRCode === 'undefined') {
                    return;
                }
                var size = Math.min(box.clientWidth || 200, 200);
                if (size < 120) {
                    size = 200;
                }
                new QRCode(box, {
                    text: text,
                    width: size,
                    height: size,
                    colorDark: renderOpts.colorDark,
                    colorLight: renderOpts.colorLight,
                    correctLevel: QRCode.CorrectLevel.M
                });
                if (box.querySelector('canvas')) {
                    var legacyImgs = box.querySelectorAll('img');
                    for (var i = 0; i < legacyImgs.length; i++) {
                        if (legacyImgs[i].parentNode) {
                            legacyImgs[i].parentNode.removeChild(legacyImgs[i]);
                        }
                    }
                }
            },
            showError: function (msg) {
                this.error = true;
                this.errorMsg = msg || '登录失败';
                if (!oauthLogin) {
                    this.refreshCode();
                }
            },
            redirectAfterLogin: function (target) {
                if (oauthLogin) {
                    if (safeOauthCallback) {
                        (window.top || window).location.href = safeOauthCallback;
                        return;
                    }
                    if (target) {
                        (window.top || window).location.href = target;
                        return;
                    }
                    (window.top || window).location.reload();
                    return;
                }
                (window.top || window).location.href = target || 'index.html';
            },
            submitLogin: function (username, password, captcha, options) {
                options = options || {};
                var data = 'username=' + encodeURIComponent(username) + '&password=' + encodeURIComponent(password) + '&captcha=' + encodeURIComponent(captcha);
                $.ajax({
                    type: 'POST',
                    url: ctx + '/sys/login',
                    data: data,
                    dataType: 'json',
                    success: function (result) {
                        if (result.code == 0) {
                            vm.redirectAfterLogin(result.data != null ? String(result.data) : 'index.html');
                        } else if (!options.silent) {
                            vm.showError(result.msg);
                        }
                    },
                    error: function () {
                        if (!options.silent) {
                            vm.showError('网络异常，请稍后重试');
                        }
                    }
                });
            },
            login: function () {
                if (!this.username) {
                    this.showError('请输入账号');
                    return;
                }
                if (!this.password) {
                    this.showError('请输入密码');
                    return;
                }
                this.submitLogin(this.username, this.password, this.captcha);
            },
            loginByPhone: function () {
                if (!/^1\d{10}$/.test(this.mobileClean)) {
                    this.showError('请输入正确的手机号');
                    return;
                }
                if (!this.mobilePassword) {
                    this.showError('请输入密码');
                    return;
                }
                this.submitLogin(this.mobileClean, this.mobilePassword, this.mobileCaptcha);
            },
            checkenv: function () {
                if (!devAutologinEnabled) {
                    return;
                }
                if (window.AutologinCheck) {
                    AutologinCheck.run({
                        ctx: ctx,
                        autologinEnabled: devAutologinEnabled,
                        oauthLogin: oauthLogin,
                        authorizeMode: authorizeMode,
                        skipAutologinCookie: skipAutologinCookie,
                        onRedirect: function (url) {
                            vm.redirectAfterLogin(url);
                        },
                        onDevProbe: function () {
                            vm.submitLogin('', '', '', { silent: true });
                        }
                    });
                    return;
                }
                if (oauthLogin || authorizeMode) {
                    return;
                }
                if (shouldSkipAutologin()) {
                    return;
                }
                $.ajax({
                    type: 'POST',
                    url: ctx + '/sys/autologin',
                    dataType: 'json',
                    success: function (result) {
                        if (!result || result.code != 0) {
                            return;
                        }
                        if (result.data != null && String(result.data) !== '') {
                            vm.redirectAfterLogin(String(result.data));
                            return;
                        }
                        if (result.devProbe) {
                            vm.submitLogin('', '', '', { silent: true });
                        }
                    }
                });
            },
            startQrLogin: function () {
                var self = this;
                self.resetQrScannedState();
                self.qrStatus = '正在加载二维码...';
                if (self.qrcPollTimer) {
                    clearInterval(self.qrcPollTimer);
                    self.qrcPollTimer = null;
                }
                $.ajax({
                    type: 'POST',
                    url: ctx + '/qrc/scanticket/web/ticket/create',
                    contentType: 'application/json',
                    data: JSON.stringify({ data: { intent: 'SELF_WEB_LOGIN' } }),
                    dataType: 'json',
                    success: function (res) {
                        if (!res || res.code !== 0 || !res.data) {
                            self.qrStatus = (res && res.msg) ? res.msg : '二维码加载失败，请点击刷新重试';
                            return;
                        }
                        self.qrcUuid = res.data.uuid;
                        self.renderQrCode(res.data.qrUrl);
                        self.qrStatus = '等待扫码...';
                        self.qrcPollTimer = setInterval(function () { self.pollQrStatus(); }, 2000);
                    },
                    error: function (xhr) {
                        var msg = '网络异常，请点击刷新重试';
                        if (xhr && xhr.responseJSON && xhr.responseJSON.msg) {
                            msg = xhr.responseJSON.msg;
                        } else if (xhr && xhr.status === 404) {
                            msg = '扫码服务不可用，请确认应用已启动 QRC 模块';
                        }
                        self.qrStatus = msg;
                    }
                });
            },
            pollQrStatus: function () {
                var self = this;
                if (!self.qrcUuid) {
                    return;
                }
                $.getJSON(ctx + '/qrc/scanticket/web/ticket/status', { uuid: self.qrcUuid }, function (res) {
                    if (!res || res.code !== 0 || !res.data) {
                        return;
                    }
                    var data = res.data;
                    if (data.status === 'SCANNED') {
                        self.applyScannerBrief(data.scannerBrief);
                        self.qrStatus = '扫码成功，请在手机点击登录';
                    }
                    if (authorizeMode) {
                        if ((data.status === 'CONFIRMED' || data.status === 'COMPLETED') && data.exchange) {
                            self.qrPhase = 'done';
                            self.qrStatus = '扫码成功，正在登录...';
                            if (self.qrcPollTimer) {
                                clearInterval(self.qrcPollTimer);
                                self.qrcPollTimer = null;
                            }
                            self.completeAuthorizeExchange(data.exchange);
                            return;
                        }
                        self.handleQrTerminalStatus(data.status);
                        return;
                    }
                    if ((data.status === 'CONFIRMED' || data.status === 'COMPLETED') && data.exchange) {
                        self.qrPhase = 'done';
                        self.qrStatus = '登录成功，正在跳转...';
                        if (self.qrcPollTimer) {
                            clearInterval(self.qrcPollTimer);
                            self.qrcPollTimer = null;
                        }
                        $.ajax({
                            type: 'POST',
                            url: ctx + '/qrc/scanticket/web/session/exchange',
                            contentType: 'application/json',
                            data: JSON.stringify({ data: { exchange: res.data.exchange } }),
                            dataType: 'json',
                            success: function (result) {
                                if (result.code === 0) {
                                    self.redirectAfterLogin(result.data != null ? String(result.data) : null);
                                } else {
                                    self.qrStatus = result.msg || '登录失败';
                                }
                            },
                            error: function () {
                                self.qrStatus = '网络异常，请刷新二维码重试';
                            }
                        });
                        return;
                    }
                    self.handleQrTerminalStatus(data.status);
                });
            }
        }
    });

    window.__LOGIN_VM__ = vm;

    try {
        var qs = new URLSearchParams(window.location.search || '');
        vm.syncOauthCallback();
        if (authorizeMode && !authorizeLoggedIn) {
            var authorizeReturnUrl = window.location.href.split('#')[0];
            var cbMain = document.getElementById('oauthAuthorizeCallback');
            var cbPhone = document.getElementById('oauthAuthorizeCallbackPhone');
            if (cbMain) {
                cbMain.value = authorizeReturnUrl;
            }
            if (cbPhone) {
                cbPhone.value = authorizeReturnUrl;
            }
            vm.oauthCallback = authorizeReturnUrl;
        }
        if (authorizeMode && serverQrUrl && vm.loginTab === 'qr') {
            vm.initAuthorizeQr();
        }
        var expired = qs.get('sessionExpired') === '1';
        var reason = qs.get('reason');
        if (!expired) {
            expired = window.sessionStorage.getItem('autumn_session_expired') === '1';
            if (!reason) {
                reason = window.sessionStorage.getItem('autumn_session_expired_reason');
            }
        }
        if (expired) {
            vm.error = true;
            vm.errorMsg = reason === 'session_terminated'
                ? '你的登录会话已被终止，请重新登录。'
                : '登录状态已过期，请重新登录。';
            window.sessionStorage.removeItem('autumn_session_expired');
            window.sessionStorage.removeItem('autumn_session_expired_reason');
        }
        if (cfg.errorMsg) {
            vm.error = true;
            vm.errorMsg = cfg.errorMsg;
        }
        if (!authorizeMode) {
            vm.loadAuthLoginProviders();
        }
    } catch (e) {
    }

    vm.checkenv();
})();
