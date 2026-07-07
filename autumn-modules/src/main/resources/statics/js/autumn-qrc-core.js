(function (window) {
    'use strict';

    function normalizeMode(mode) {
        mode = (mode || 'as').toLowerCase();
        return mode === 'rp' ? 'rp' : 'as';
    }

    function apiPrefix(mode, ctx) {
        if (mode === 'rp') {
            return (ctx || '') + '/client/oauth2/qrc/web';
        }
        return (ctx || '') + '/qrc/scanticket/web';
    }

    function createQrMethods(options) {
        options = options || {};
        var ctx = options.ctx || '';
        var mode = normalizeMode(options.mode);
        var pollIntervalMs = options.pollIntervalMs || 2000;
        var boxId = options.boxId || 'loginQrcodeBox';
        var prefix = apiPrefix(mode, ctx);

        return {
            qrcUuid: '',
            qrStatus: '',
            qrPhase: 'pending',
            scannerDisplayName: '',
            scannerIconUrl: '',
            qrcPollTimer: null,
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
            renderQrCode: function (text) {
                var box = document.getElementById(boxId);
                if (!box) {
                    return;
                }
                box.innerHTML = '';
                if (typeof QRCode === 'undefined') {
                    return;
                }
                new QRCode(box, {
                    text: text,
                    width: options.qrSize || 200,
                    height: options.qrSize || 200,
                    colorDark: '#1a1d26',
                    colorLight: '#ffffff',
                    correctLevel: QRCode.CorrectLevel.M
                });
            },
            stopPoll: function () {
                if (this.qrcPollTimer) {
                    clearInterval(this.qrcPollTimer);
                    this.qrcPollTimer = null;
                }
            },
            refreshQrLogin: function () {
                this.resetQrScannedState();
                this.startQrLogin();
            },
            startQrLogin: function (onUnavailable) {
                var self = this;
                self.resetQrScannedState();
                self.qrStatus = '正在加载二维码...';
                self.stopPoll();
                var createUrl = prefix + '/ticket/create';
                var payload = mode === 'rp'
                    ? { data: { callback: options.callback || '' } }
                    : { data: { intent: options.intent || 'SELF_WEB_LOGIN' } };
                $.ajax({
                    type: 'POST',
                    url: createUrl,
                    contentType: 'application/json',
                    data: JSON.stringify(payload),
                    dataType: 'json',
                    success: function (res) {
                        if (!res || res.code !== 0 || !res.data) {
                            if (typeof onUnavailable === 'function') {
                                onUnavailable(true);
                                return;
                            }
                            self.qrStatus = (res && res.msg) ? res.msg : '二维码加载失败，请点击刷新重试';
                            return;
                        }
                        self.qrcUuid = res.data.uuid;
                        self.renderQrCode(res.data.qrUrl);
                        self.qrStatus = '等待扫码...';
                        self.qrcPollTimer = setInterval(function () {
                            self.pollQrStatus(onUnavailable);
                        }, pollIntervalMs);
                    },
                    error: function (xhr) {
                        if (typeof onUnavailable === 'function') {
                            onUnavailable(true, xhr);
                            return;
                        }
                        var msg = '网络异常，请点击刷新重试';
                        if (xhr && xhr.responseJSON && xhr.responseJSON.msg) {
                            msg = xhr.responseJSON.msg;
                        } else if (xhr && xhr.status === 404) {
                            msg = '扫码服务不可用，请确认应用已启用 QRC/RP 模块';
                        }
                        self.qrStatus = msg;
                    }
                });
            },
            pollQrStatus: function (onUnavailable) {
                var self = this;
                if (!self.qrcUuid) {
                    return;
                }
                if (mode === 'rp') {
                    self.pollRpStatus(onUnavailable);
                    return;
                }
                self.pollAsStatus(onUnavailable);
            },
            pollAsStatus: function (onUnavailable) {
                var self = this;
                $.getJSON(prefix + '/ticket/status', { uuid: self.qrcUuid }, function (res) {
                    if (!res || res.code !== 0 || !res.data) {
                        return;
                    }
                    var data = res.data;
                    if (data.status === 'SCANNED') {
                        self.applyScannerBrief(data.scannerBrief);
                        self.qrStatus = '扫码成功，请在手机点击登录';
                    }
                    if (typeof options.onAuthorizeExchange === 'function' && (data.status === 'CONFIRMED' || data.status === 'COMPLETED') && data.exchange) {
                        self.stopPoll();
                        options.onAuthorizeExchange(data.exchange, self);
                        return;
                    }
                    if ((data.status === 'CONFIRMED' || data.status === 'COMPLETED') && data.exchange) {
                        self.qrPhase = 'done';
                        self.qrStatus = '登录成功，正在跳转...';
                        self.stopPoll();
                        $.ajax({
                            type: 'POST',
                            url: prefix + '/session/exchange',
                            contentType: 'application/json',
                            data: JSON.stringify({ data: { exchange: data.exchange } }),
                            dataType: 'json',
                            success: function (result) {
                                if (result.code === 0) {
                                    var target = result.data != null ? String(result.data) : null;
                                    if (typeof options.onSuccess === 'function') {
                                        options.onSuccess(target, self);
                                    } else if (target) {
                                        (window.top || window).location.href = target;
                                    } else {
                                        (window.top || window).location.href = 'index.html';
                                    }
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
            },
            pollRpStatus: function (onUnavailable) {
                var self = this;
                $.ajax({
                    type: 'POST',
                    url: prefix + '/ticket/status',
                    data: { uuid: self.qrcUuid },
                    dataType: 'json',
                    success: function (res) {
                        if (!res || res.code !== 0 || !res.data) {
                            return;
                        }
                        var data = res.data;
                        if (data.status === 'SCANNED') {
                            self.applyScannerBrief(data.scannerBrief);
                            self.qrStatus = '扫码成功，请在手机点击确认授权';
                        }
                        if (data.status === 'COMPLETED' || ((data.status === 'CONFIRMED' || data.status === 'COMPLETED') && data.result && data.result.code)) {
                            self.qrPhase = 'done';
                            self.qrStatus = '授权成功，正在登录...';
                            self.stopPoll();
                            $.ajax({
                                type: 'POST',
                                url: prefix + '/ticket/complete',
                                contentType: 'application/json',
                                data: JSON.stringify({ data: { uuid: self.qrcUuid, callback: options.callback || '' } }),
                                dataType: 'json',
                                success: function (result) {
                                    if (result.code === 0) {
                                        var target = result.data != null ? String(result.data) : null;
                                        if (typeof options.onSuccess === 'function') {
                                            options.onSuccess(target, self);
                                        } else if (target) {
                                            (window.top || window).location.href = target;
                                        } else {
                                            (window.top || window).location.href = '/';
                                        }
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
                    },
                    error: function () {
                        if (typeof onUnavailable === 'function') {
                            onUnavailable(true);
                        }
                    }
                });
            },
            cancelQrLogin: function () {
                var self = this;
                if (!self.qrcUuid || mode !== 'rp') {
                    self.stopPoll();
                    return;
                }
                $.ajax({
                    type: 'POST',
                    url: prefix + '/ticket/cancel',
                    data: { uuid: self.qrcUuid },
                    complete: function () {
                        self.stopPoll();
                    }
                });
            }
        };
    }

    window.AutumnQrc = {
        createMethods: createQrMethods,
        mergeInto: function (target, options) {
            var methods = createQrMethods(options);
            Object.keys(methods).forEach(function (key) {
                target[key] = methods[key];
            });
            return target;
        }
    };
})(window);
