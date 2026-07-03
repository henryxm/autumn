(function () {
    'use strict';

    var ctx = AuthPage.readCtx();
    var base = AuthPage.createBaseVm(ctx);

    new Vue({
        el: '#rrapp',
        data: Object.assign({}, base, {
            account: '',
            password: '',
            confirmPassword: '',
            captcha: '',
            agree: false,
            accountTaken: false,
            agreeError: ''
        }),
        methods: {
            refreshCode: base.refreshCode,
            showError: base.showError,
            checkAvailable: function () {
                var account = AuthPage.trimAccount(this.account);
                this.accountTaken = false;
                if (!account) {
                    return;
                }
                var self = this;
                $.getJSON(ctx + '/sys/account/available', { account: account }, function (res) {
                    self.accountTaken = !!(res && res.code !== 0);
                });
            },
            submit: function () {
                this.clearError();
                this.agreeError = '';
                var account = AuthPage.trimAccount(this.account);
                if (!this.validateAccountPassword(account, this.password, this.confirmPassword, '请设置登录密码')) {
                    return;
                }
                if (!this.agree) {
                    this.agreeError = '请先阅读并同意服务条款与隐私政策';
                    this.showError(this.agreeError);
                    return;
                }
                if (this.accountTaken) {
                    this.showError('该账号已被注册');
                    return;
                }
                var self = this;
                this.submitAuth({
                    url: ctx + '/sys/register',
                    failMessage: '注册失败',
                    data: {
                        account: account,
                        password: self.password,
                        confirmPassword: self.confirmPassword,
                        captcha: self.captcha
                    }
                });
            },
            clearError: base.clearError,
            validateAccountPassword: base.validateAccountPassword,
            submitAuth: base.submitAuth
        },
        mounted: function () {
            this.refreshCode();
        }
    });
})();
