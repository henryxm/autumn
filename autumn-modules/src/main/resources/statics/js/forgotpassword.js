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
            success: false
        }),
        methods: {
            refreshCode: base.refreshCode,
            showError: base.showError,
            submit: function () {
                this.clearError();
                var account = AuthPage.trimAccount(this.account);
                if (!this.validateAccountPassword(account, this.password, this.confirmPassword, '请设置新密码')) {
                    return;
                }
                var self = this;
                this.submitAuth({
                    url: ctx + '/sys/forgotpassword',
                    failMessage: '重置失败',
                    data: {
                        account: account,
                        password: self.password,
                        confirmPassword: self.confirmPassword,
                        captcha: self.captcha
                    },
                    onSuccess: function (res) {
                        if (res.code === 0) {
                            self.success = true;
                        } else {
                            self.showError(res.msg || '重置失败');
                            self.refreshCode();
                        }
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
