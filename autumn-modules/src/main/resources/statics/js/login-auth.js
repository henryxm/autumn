(function (window) {
    'use strict';

    var PASSWORD_MIN_LENGTH = 6;

    function trim(value) {
        return (value || '').trim();
    }

    function readCtx() {
        var body = document.body;
        if (body && body.getAttribute('data-ctx') != null) {
            return body.getAttribute('data-ctx') || '';
        }
        return window.__AUTH_CTX__ || '';
    }

    window.AuthPage = {
        PASSWORD_MIN_LENGTH: PASSWORD_MIN_LENGTH,
        readCtx: readCtx,
        captchaUrl: function (ctx) {
            return (ctx || readCtx()) + '/captcha.jpg?t=' + Date.now();
        },
        trimAccount: trim,
        isPasswordMismatch: function (message) {
            return message && message.indexOf('不一致') >= 0;
        },
        mapPasswordError: function (message, emptyLabel) {
            if (message === '请设置密码') {
                return emptyLabel || message;
            }
            return message;
        },
        validatePasswordPair: function (password, confirmPassword, minLength) {
            var min = minLength || PASSWORD_MIN_LENGTH;
            if (!password) {
                return '请设置密码';
            }
            if (password.length < min) {
                return '密码长度不能少于' + min + '位';
            }
            if (password !== confirmPassword) {
                return '两次输入的密码不一致';
            }
            return null;
        },
        createBaseVm: function (ctx) {
            ctx = ctx || readCtx();
            return {
                ctx: ctx,
                src: window.AuthPage.captchaUrl(ctx),
                error: false,
                errorMsg: '',
                passwordMismatch: false,
                submitting: false,
                refreshCode: function () {
                    this.src = window.AuthPage.captchaUrl(ctx);
                },
                showError: function (msg) {
                    this.error = true;
                    this.errorMsg = msg || '操作失败';
                },
                clearError: function () {
                    this.error = false;
                    this.errorMsg = '';
                    this.passwordMismatch = false;
                },
                validateAccountPassword: function (account, password, confirmPassword, emptyPasswordLabel) {
                    if (!trim(account)) {
                        this.showError('请输入账号');
                        return false;
                    }
                    var passwordError = window.AuthPage.validatePasswordPair(password, confirmPassword);
                    if (passwordError) {
                        this.passwordMismatch = window.AuthPage.isPasswordMismatch(passwordError);
                        this.showError(window.AuthPage.mapPasswordError(passwordError, emptyPasswordLabel));
                        return false;
                    }
                    return true;
                },
                submitAuth: function (options) {
                    var self = this;
                    self.submitting = true;
                    window.AuthPage.post({
                        url: options.url,
                        data: options.data,
                        success: function (res) {
                            if (typeof options.onSuccess === 'function') {
                                options.onSuccess(res);
                                return;
                            }
                            if (res.code === 0) {
                                (window.top || window).location.href = res.data || 'index.html';
                            } else {
                                self.showError(res.msg || options.failMessage || '操作失败');
                                self.refreshCode();
                            }
                        },
                        onFail: function (msg) {
                            self.showError(msg);
                            self.refreshCode();
                        },
                        complete: function () {
                            self.submitting = false;
                        }
                    });
                }
            };
        },
        post: function (options) {
            $.ajax({
                type: 'POST',
                url: options.url,
                data: options.data,
                dataType: 'json',
                success: options.success,
                error: options.error || function () {
                    if (typeof options.onFail === 'function') {
                        options.onFail('网络异常，请稍后重试');
                    }
                },
                complete: options.complete
            });
        }
    };
})(window);
