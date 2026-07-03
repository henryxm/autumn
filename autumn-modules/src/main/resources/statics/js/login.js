(function () {
    'use strict';

    var cfg = window.__LOGIN_CONFIG__ || {};
    var ctx = cfg.ctx || (window.AuthPage && AuthPage.readCtx()) || '';
    var oauthLogin = !!cfg.oauthLogin;
    var oauthAuthorize = !!cfg.oauthAuthorize;
    var authorizeLoggedIn = !!cfg.authorizeLoggedIn;
    var oauthDenyRedirect = cfg.oauthDenyRedirect || '';
    var serverUuid = cfg.serverUuid || '';
    var serverQrUrl = cfg.serverQrUrl || '';
    var serverPollIntervalMs = cfg.serverPollIntervalMs || 2000;

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
            qrStatus: oauthAuthorize ? '正在加载二维码...' : '切换到扫码登录后加载二维码',
            qrcUuid: '',
            qrcPollTimer: null,
            oauthCallback: '',
            authorizeLoggedIn: authorizeLoggedIn,
            consentChecked: true,
            oauthDenyRedirect: oauthDenyRedirect
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
                return '确认后将跳转回第三方应用';
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
                    if (oauthAuthorize && serverQrUrl) {
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
            syncOauthCallback: function () {
                if (oauthAuthorize) {
                    this.oauthCallback = window.location.href.split('#')[0];
                    return;
                }
                var qs = new URLSearchParams(window.location.search || '');
                this.oauthCallback = qs.get('callback') || '';
            },
            beforeOauthAuthorizeLogin: function (e) {
                if (oauthAuthorize) {
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
                if (oauthAuthorize) {
                    window.location.reload();
                    return;
                }
                this.startQrLogin();
            },
            initAuthorizeQr: function () {
                var self = this;
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
            submitOAuthConsent: function () {
                if (!this.authorizeLoggedIn) {
                    this.showError('请先在左侧完成登录');
                    return;
                }
                if (!this.consentChecked) {
                    this.showError('请勾选授权协议后再确认');
                    return;
                }
                var form = document.getElementById('oauthApproveForm');
                if (form) {
                    form.submit();
                }
            },
            completeAuthorizeExchange: function (exchange) {
                var self = this;
                $.ajax({
                    type: 'POST',
                    url: ctx + '/qrc/web/v1/session/exchange',
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
                if (!box) {
                    return;
                }
                box.innerHTML = '';
                if (typeof QRCode === 'undefined') {
                    return;
                }
                new QRCode(box, {
                    text: text,
                    width: 200,
                    height: 200,
                    colorDark: '#1a1d26',
                    colorLight: '#ffffff',
                    correctLevel: QRCode.CorrectLevel.M
                });
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
                    var qs = new URLSearchParams(window.location.search || '');
                    var callback = qs.get('callback');
                    if (callback) {
                        (window.top || window).location.href = callback;
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
            submitLogin: function (username, password, captcha) {
                var data = 'username=' + encodeURIComponent(username) + '&password=' + encodeURIComponent(password) + '&captcha=' + encodeURIComponent(captcha);
                $.ajax({
                    type: 'POST',
                    url: ctx + '/sys/login',
                    data: data,
                    dataType: 'json',
                    success: function (result) {
                        if (result.code == 0) {
                            vm.redirectAfterLogin(result.data != null ? String(result.data) : 'index.html');
                        } else {
                            vm.showError(result.msg);
                        }
                    },
                    error: function () {
                        vm.showError('网络异常，请稍后重试');
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
                if (oauthLogin) {
                    return;
                }
                $.ajax({
                    type: 'POST',
                    url: ctx + '/sys/autologin',
                    dataType: 'json',
                    success: function (result) {
                        if (result.code == 0) {
                            vm.login();
                        }
                    }
                });
            },
            startQrLogin: function () {
                var self = this;
                self.qrStatus = '正在加载二维码...';
                if (self.qrcPollTimer) {
                    clearInterval(self.qrcPollTimer);
                    self.qrcPollTimer = null;
                }
                $.ajax({
                    type: 'POST',
                    url: ctx + '/qrc/web/v1/ticket/create',
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
                $.getJSON(ctx + '/qrc/web/v1/ticket/status', { uuid: self.qrcUuid }, function (res) {
                    if (!res || res.code !== 0 || !res.data) {
                        return;
                    }
                    var data = res.data;
                    if (data.status === 'SCANNED') {
                        self.qrStatus = '已扫码，请在 APP 确认';
                    }
                    if (oauthAuthorize) {
                        if ((data.status === 'CONFIRMED' || data.status === 'COMPLETED') && data.exchange) {
                            self.qrStatus = '扫码成功，正在登录...';
                            if (self.qrcPollTimer) {
                                clearInterval(self.qrcPollTimer);
                                self.qrcPollTimer = null;
                            }
                            self.completeAuthorizeExchange(data.exchange);
                            return;
                        }
                        if (data.status === 'DENIED' || data.status === 'CANCELLED' || data.status === 'EXPIRED') {
                            self.qrStatus = '扫码已取消或过期，请刷新重试';
                        }
                        return;
                    }
                    if ((data.status === 'CONFIRMED' || data.status === 'COMPLETED') && data.exchange) {
                        self.qrStatus = '登录成功，正在跳转...';
                        if (self.qrcPollTimer) {
                            clearInterval(self.qrcPollTimer);
                            self.qrcPollTimer = null;
                        }
                        $.ajax({
                            type: 'POST',
                            url: ctx + '/qrc/web/v1/session/exchange',
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
                    }
                });
            }
        }
    });

    window.__LOGIN_VM__ = vm;

    try {
        var qs = new URLSearchParams(window.location.search || '');
        vm.syncOauthCallback();
        if (oauthAuthorize && !authorizeLoggedIn) {
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
        if (oauthAuthorize && serverQrUrl && vm.loginTab === 'qr') {
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
    } catch (e) {
    }

    vm.checkenv();
})();
