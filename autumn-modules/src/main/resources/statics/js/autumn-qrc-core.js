(function (window) {
    'use strict';

    var QR_RENDER_MAX_ATTEMPTS = 12;

    function isBoxVisible(box) {
        if (!box) {
            return false;
        }
        var rect = box.getBoundingClientRect();
        return rect.width > 8 && rect.height > 8;
    }

    function resolveQrBoxSize(box, fallback) {
        fallback = fallback || 200;
        if (!box) {
            return fallback;
        }
        var style = window.getComputedStyle(box);
        var padX = (parseFloat(style.paddingLeft) || 0) + (parseFloat(style.paddingRight) || 0);
        var padY = (parseFloat(style.paddingTop) || 0) + (parseFloat(style.paddingBottom) || 0);
        var width = box.clientWidth;
        var height = box.clientHeight;
        if ((!width || width < 40) && style.width) {
            width = parseFloat(style.width);
        }
        if ((!height || height < 40) && style.height) {
            height = parseFloat(style.height);
        }
        var size = Math.floor(Math.min((width || 0) - padX, (height || 0) - padY));
        if (size < 120) {
            size = fallback;
        }
        if (size > 280) {
            size = 280;
        }
        return size;
    }

    function isBrokenQrImage(img) {
        if (!img) {
            return true;
        }
        var src = img.getAttribute('src') || '';
        if (!src || src === 'about:blank') {
            return true;
        }
        if (img.complete && img.naturalWidth === 0) {
            return true;
        }
        return false;
    }

    function normalizeQrMarkup(box, size) {
        if (!box) {
            return;
        }
        var canvases = box.querySelectorAll('canvas');
        var imgs = box.querySelectorAll('img');
        var i;
        for (i = 1; i < canvases.length; i++) {
            if (canvases[i].parentNode) {
                canvases[i].parentNode.removeChild(canvases[i]);
            }
        }
        var canvas = canvases[0] || null;
        if (canvas) {
            canvas.style.display = 'block';
            var paintSize = canvas.width || size;
            canvas.style.width = paintSize + 'px';
            canvas.style.height = paintSize + 'px';
            for (i = 0; i < imgs.length; i++) {
                if (imgs[i].parentNode) {
                    imgs[i].parentNode.removeChild(imgs[i]);
                }
            }
        } else if (imgs.length > 0) {
            for (i = 1; i < imgs.length; i++) {
                if (imgs[i].parentNode) {
                    imgs[i].parentNode.removeChild(imgs[i]);
                }
            }
            var img = imgs[0];
            img.alt = '';
            if (isBrokenQrImage(img)) {
                if (img.parentNode) {
                    img.parentNode.removeChild(img);
                }
            } else {
                img.style.display = 'block';
                img.style.width = size + 'px';
                img.style.height = size + 'px';
            }
        }
        box.setAttribute('data-qr-size', String(size));
    }

    function isQrPainted(box) {
        if (!box) {
            return false;
        }
        var canvas = box.querySelector('canvas');
        if (canvas && canvas.width > 0 && canvas.height > 0) {
            try {
                var ctx = canvas.getContext('2d');
                var sample = ctx.getImageData(0, 0, Math.min(8, canvas.width), Math.min(8, canvas.height));
                for (var i = 0; i < sample.data.length; i += 4) {
                    if (sample.data[i] < 250 || sample.data[i + 1] < 250 || sample.data[i + 2] < 250) {
                        return true;
                    }
                }
            } catch (ignored) {
                return true;
            }
        }
        var img = box.querySelector('img');
        return !!(img && !isBrokenQrImage(img) && img.naturalWidth > 0);
    }

    function paintWithJquery(box, text, size) {
        if (!window.jQuery || !jQuery.fn || !jQuery.fn.qrcode) {
            return false;
        }
        try {
            jQuery(box).empty().qrcode({
                render: 'canvas',
                text: text,
                width: size,
                height: size,
                foreground: '#1a1d26',
                background: '#ffffff'
            });
            normalizeQrMarkup(box, size);
            return isQrPainted(box);
        } catch (ignored) {
            return false;
        }
    }

    function paintWithQrCode(box, text, renderOpts) {
        if (typeof QRCode === 'undefined') {
            return false;
        }
        try {
            box.innerHTML = '';
            new QRCode(box, {
                text: text,
                width: renderOpts.size,
                height: renderOpts.size,
                colorDark: renderOpts.colorDark || '#1a1d26',
                colorLight: renderOpts.colorLight || '#ffffff',
                correctLevel: QRCode.CorrectLevel.M
            });
            return true;
        } catch (ignored) {
            return false;
        }
    }

    function finalizeQrRender(box, text, renderOpts, attempt, callback) {
        normalizeQrMarkup(box, renderOpts.size);
        if (isQrPainted(box)) {
            if (typeof callback === 'function') {
                callback(true);
            }
            return;
        }
        if (attempt >= 5) {
            var ok = paintWithJquery(box, text, renderOpts.size);
            if (typeof callback === 'function') {
                callback(ok);
            }
            return;
        }
        window.setTimeout(function () {
            finalizeQrRender(box, text, renderOpts, attempt + 1, callback);
        }, attempt === 0 ? 0 : 40);
    }

    function renderIntoBox(box, text, renderOpts, attempt) {
        renderOpts = renderOpts || {};
        attempt = attempt || 0;
        if (!box || !text) {
            return false;
        }
        if (!isBoxVisible(box)) {
            if (attempt >= QR_RENDER_MAX_ATTEMPTS) {
                var forcedSize = renderOpts.size || renderOpts.fallbackSize || 200;
                renderOpts.size = forcedSize;
                if (!paintWithQrCode(box, text, renderOpts)) {
                    return paintWithJquery(box, text, forcedSize);
                }
                finalizeQrRender(box, text, renderOpts, 0, function (ok) {
                    if (!ok) {
                        paintWithJquery(box, text, forcedSize);
                    }
                });
                return true;
            }
            var nextAttempt = attempt + 1;
            window.requestAnimationFrame(function () {
                renderIntoBox(box, text, renderOpts, nextAttempt);
            });
            return true;
        }
        var size = renderOpts.size || resolveQrBoxSize(box, renderOpts.fallbackSize || 200);
        renderOpts.size = size;
        if (!paintWithQrCode(box, text, renderOpts)) {
            return paintWithJquery(box, text, size);
        }
        finalizeQrRender(box, text, renderOpts, 0, function (ok) {
            if (!ok) {
                paintWithJquery(box, text, size);
            }
        });
        return true;
    }

    function normalizeMode(mode) {
        mode = (mode || 'as').toLowerCase();
        return mode === 'rp' ? 'rp' : 'as';
    }

    function resolveCredentialType(options) {
        return options.credentialType || options.type || '';
    }

    function resolveCredentialId(options) {
        return options.credentialId || options.id || '';
    }

    function isOpenCredential(options) {
        return resolveCredentialType(options) === 'oauth2_open';
    }

    function buildAsCreatePayload(options) {
        var payload = { intent: options.intent || 'SELF_WEB_LOGIN' };
        var credType = resolveCredentialType(options);
        var credId = resolveCredentialId(options);
        if (credType) {
            payload.type = credType;
        }
        if (credId) {
            payload.id = credId;
        }
        if (options.callback) {
            payload.callback = options.callback;
        }
        return payload;
    }

    function apiPrefix(mode, ctx) {
        if (mode === 'rp') {
            return (ctx || '') + '/client/oauth2/qrc/web';
        }
        return (ctx || '') + '/qrc/scanticket/web';
    }

    function resolveScannerIconUrl(icon, ctx) {
        if (!icon) {
            return '';
        }
        if (/^https?:\/\//i.test(icon)) {
            return icon;
        }
        var base = ctx || '';
        if (icon.charAt(0) === '/') {
            return base + icon;
        }
        return base + '/' + icon;
    }

    function wireVueScannerUi(vm, ctx) {
        if (!vm) {
            return;
        }
        var pageCtx = ctx || '';
        vm.applyScannerBrief = function (brief) {
            var name = brief && brief.displayName ? brief.displayName : '';
            var iconUrl = '';
            if (brief && brief.icon) {
                iconUrl = typeof vm.resolveScannerIconUrl === 'function'
                    ? vm.resolveScannerIconUrl(brief.icon)
                    : resolveScannerIconUrl(brief.icon, pageCtx);
            }
            var quickPath = !!vm.appQuickActive;
            if (typeof vm.$set === 'function') {
                vm.$set(vm, 'scannerDisplayName', name);
                vm.$set(vm, 'scannerIconUrl', iconUrl);
                if (!quickPath) {
                    vm.$set(vm, 'qrPhase', 'scanned');
                }
            } else {
                vm.scannerDisplayName = name;
                vm.scannerIconUrl = iconUrl;
                if (!quickPath) {
                    vm.qrPhase = 'scanned';
                }
            }
        };
        vm.resetQrScannedState = function () {
            if (typeof vm.$set === 'function') {
                vm.$set(vm, 'qrPhase', 'pending');
                vm.$set(vm, 'scannerDisplayName', '');
                vm.$set(vm, 'scannerIconUrl', '');
            } else {
                vm.qrPhase = 'pending';
                vm.scannerDisplayName = '';
                vm.scannerIconUrl = '';
            }
            if (vm.appQuickActive != null || vm.appQuickPhase != null) {
                if (typeof vm.$set === 'function') {
                    vm.$set(vm, 'appQuickActive', false);
                    vm.$set(vm, 'appQuickBusy', false);
                    vm.$set(vm, 'appQuickPhase', 'idle');
                    vm.$set(vm, 'appQuickHint', '');
                    vm.$set(vm, 'appQuickTargetKey', '');
                } else {
                    vm.appQuickActive = false;
                    vm.appQuickBusy = false;
                    vm.appQuickPhase = 'idle';
                    vm.appQuickHint = '';
                    vm.appQuickTargetKey = '';
                }
            }
        };
        vm.setQrStatus = function (text) {
            if (typeof vm.$set === 'function') {
                vm.$set(vm, 'qrStatus', text || '');
            } else {
                vm.qrStatus = text || '';
            }
        };
    }

    function bindPlainHostScannedUi(host, ui) {
        ui = ui || {};
        if (!host) {
            return function () {};
        }
        var pendingEl = ui.pendingEl;
        var scannedEl = ui.scannedEl;
        var tipEl = ui.tipEl;
        var avatarImg = ui.avatarImg;
        var avatarPh = ui.avatarPh;
        var nameEl = ui.nameEl;
        var statusEl = ui.statusEl;
        var pageCtx = ui.ctx || '';
        return function syncPlainHostScannedUi() {
            var phase = host.qrPhase || 'pending';
            var scanned = (phase === 'scanned' || phase === 'done') && !host.appQuickActive;
            if (pendingEl) {
                pendingEl.style.display = scanned ? 'none' : '';
            }
            if (scannedEl) {
                scannedEl.style.display = scanned ? '' : 'none';
            }
            if (tipEl) {
                tipEl.style.display = scanned ? 'none' : '';
                if (!scanned && host.qrStatus) {
                    tipEl.textContent = host.qrStatus;
                }
            }
            if (!scanned) {
                return;
            }
            var iconUrl = host.scannerIconUrl || '';
            if (!iconUrl && host.scannerBrief && host.scannerBrief.icon) {
                iconUrl = typeof host.resolveScannerIconUrl === 'function'
                    ? host.resolveScannerIconUrl(host.scannerBrief.icon)
                    : resolveScannerIconUrl(host.scannerBrief.icon, pageCtx);
            }
            if (avatarImg && avatarPh) {
                if (iconUrl) {
                    avatarImg.src = iconUrl;
                    avatarImg.style.display = '';
                    avatarPh.style.display = 'none';
                } else {
                    avatarImg.style.display = 'none';
                    avatarPh.style.display = '';
                }
            }
            if (nameEl) {
                nameEl.textContent = host.scannerDisplayName || '';
                nameEl.style.display = host.scannerDisplayName ? '' : 'none';
            }
            if (statusEl && host.qrStatus) {
                statusEl.textContent = host.qrStatus;
            }
        };
    }

    function createQrMethods(options) {
        options = options || {};
        var ctx = options.ctx || '';
        var mode = normalizeMode(options.mode);
        var pollIntervalMs = options.pollIntervalMs || 2000;
        var sseFallbackDelayMs = options.sseFallbackDelayMs == null ? 5000 : options.sseFallbackDelayMs;
        var sseFallbackOnError = options.sseFallbackOnError !== false;
        var boxId = options.boxId || 'loginQrcodeBox';
        var prefix = apiPrefix(mode, ctx);

        return {
            qrcUuid: '',
            qrStatus: '',
            qrPhase: 'pending',
            scannerDisplayName: '',
            scannerIconUrl: '',
            qrcPollTimer: null,
            qrcEventSource: null,
            qrcNotifyChannel: null,
            sseOpened: false,
            sseReceivedStatus: false,
            sseFallbackTimer: null,
            _sseUuid: '',
            _confirmWatchTimer: null,
            clearSseFallbackTimer: function () {
                if (this.sseFallbackTimer) {
                    clearTimeout(this.sseFallbackTimer);
                    this.sseFallbackTimer = null;
                }
            },
            stopNotify: function () {
                this.clearSseFallbackTimer();
                this.clearConfirmWatch();
                if (this.qrcPollTimer) {
                    clearInterval(this.qrcPollTimer);
                    this.qrcPollTimer = null;
                }
                if (this.qrcEventSource) {
                    this.qrcEventSource.close();
                    this.qrcEventSource = null;
                }
                this.qrcNotifyChannel = null;
                this.sseOpened = false;
                this.sseReceivedStatus = false;
                this._sseUuid = '';
            },
            clearConfirmWatch: function () {
                if (this._confirmWatchTimer) {
                    clearInterval(this._confirmWatchTimer);
                    this._confirmWatchTimer = null;
                }
            },
            /**
             * 扫码/快捷确认等待期间并行轮询，避免 SSE 漏推 COMPLETED 导致页面卡住。
             */
            ensureConfirmWatch: function (onUnavailable) {
                var self = this;
                if (self._confirmWatchTimer || self.qrPhase === 'done') {
                    return;
                }
                self._confirmWatchTimer = setInterval(function () {
                    if (self.qrPhase === 'done') {
                        self.clearConfirmWatch();
                        return;
                    }
                    self.pollQrStatus(onUnavailable);
                }, Math.max(1000, Math.min(pollIntervalMs, 2000)));
            },
            forceNavigate: function (target) {
                var url = target != null && String(target) !== '' ? String(target) : '/';
                var win = window.top || window;
                try {
                    win.location.href = url;
                } catch (e1) {
                    /* ignore */
                }
                window.setTimeout(function () {
                    try {
                        win.location.replace(url);
                    } catch (e2) {
                        try {
                            win.location.reload();
                        } catch (e3) {
                            /* ignore */
                        }
                    }
                }, 800);
            },
            finishSessionExchange: function (exchange, onUnavailable) {
                var self = this;
                if (self.qrPhase === 'done') {
                    return;
                }
                self.qrPhase = 'done';
                if (self.appQuickActive) {
                    self.appQuickPhase = 'waiting';
                    self.appQuickHint = '登录成功，正在跳转…';
                    self.appQuickBusy = true;
                }
                self.qrStatus = '登录成功，正在跳转...';
                self.stopNotify();
                $.ajax({
                    type: 'POST',
                    url: prefix + '/session/exchange',
                    contentType: 'application/json',
                    data: JSON.stringify({ data: { exchange: exchange, rememberMe: true } }),
                    dataType: 'json',
                    success: function (result) {
                        if (result && result.code === 0) {
                            var target = result.data != null ? String(result.data) : null;
                            if (typeof options.onSuccess === 'function') {
                                try {
                                    options.onSuccess(target, self);
                                } catch (e) {
                                    /* fall through to force navigate */
                                }
                            }
                            self.forceNavigate(target || '/');
                            return;
                        }
                        // exchange 失败但本地可能已有会话：尝试刷新恢复
                        if (self.appQuickActive) {
                            self.appQuickHint = (result && result.msg) || '登录可能已完成，正在刷新…';
                        }
                        self.qrStatus = (result && result.msg) || '登录失败';
                        window.setTimeout(function () {
                            self.recoverAfterMissingSession();
                        }, 400);
                    },
                    error: function () {
                        if (typeof onUnavailable === 'function') {
                            onUnavailable(true);
                        }
                        if (self.appQuickActive) {
                            self.appQuickHint = '网络异常，正在尝试刷新…';
                        }
                        self.qrStatus = '网络异常，请刷新重试';
                        window.setTimeout(function () {
                            self.recoverAfterMissingSession();
                        }, 500);
                    }
                });
            },
            resetQrScannedState: function () {
                this.qrPhase = 'pending';
                this.scannerDisplayName = '';
                this.scannerIconUrl = '';
                this.appQuickActive = false;
                this.appQuickBusy = false;
                this.appQuickPhase = 'idle';
                this.appQuickHint = '';
                this.appQuickTargetKey = '';
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
                this.scannerDisplayName = (brief && brief.displayName) ? brief.displayName : '';
                this.scannerIconUrl = brief && brief.icon ? this.resolveScannerIconUrl(brief.icon) : '';
                if (!this.appQuickActive) {
                    this.qrPhase = 'scanned';
                }
            },
            setQrStatus: function (text) {
                if (this.appQuickActive) {
                    var t = text || '';
                    if (/扫码/.test(t)) {
                        this.appQuickPhase = 'waiting';
                        this.appQuickHint = /成功|确认|授权/.test(t) ? '等待超然信确认…' : '请在超然信 APP 中确认登录';
                        this.appQuickBusy = true;
                        return;
                    }
                }
                this.qrStatus = text || '';
            },
            markQrScanned: function (brief, statusText) {
                this.applyScannerBrief(brief);
                if (this.appQuickActive) {
                    this.appQuickPhase = 'waiting';
                    this.appQuickHint = '等待超然信确认…';
                    this.appQuickBusy = true;
                    return;
                }
                this.setQrStatus(statusText || '扫码成功，请在手机点击确认授权');
            },
            completeQrRedirect: function (target) {
                if (this.qrPhase === 'done') {
                    return;
                }
                if (typeof this.$set === 'function') {
                    this.$set(this, 'qrPhase', 'done');
                } else {
                    this.qrPhase = 'done';
                }
                if (this.appQuickActive) {
                    this.appQuickPhase = 'waiting';
                    this.appQuickHint = '登录成功，正在跳转…';
                    this.appQuickBusy = true;
                }
                this.setQrStatus('授权成功，正在跳转...');
                this.stopNotify();
                var url = target != null && String(target) !== '' ? String(target) : '/';
                if (typeof options.onSuccess === 'function') {
                    try {
                        options.onSuccess(url, this);
                    } catch (e) {
                        /* ignore */
                    }
                }
                this.forceNavigate(url);
            },
            recoverAfterMissingSession: function () {
                var self = this;
                if (self.qrPhase === 'done') {
                    return;
                }
                if (typeof self.$set === 'function') {
                    self.$set(self, 'qrPhase', 'done');
                } else {
                    self.qrPhase = 'done';
                }
                self.setQrStatus('登录可能已完成，正在刷新...');
                self.stopNotify();
                window.setTimeout(function () {
                    (window.top || window).location.reload();
                }, 300);
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
                renderIntoBox(box, text, {
                    size: options.qrSize || resolveQrBoxSize(box, 200),
                    fallbackSize: options.qrSize || 200,
                    colorDark: '#1a1d26',
                    colorLight: '#ffffff'
                });
            },
            stopPoll: function () {
                this.stopNotify();
            },
            refreshQrLogin: function () {
                this.resetQrScannedState();
                this.startQrLogin();
            },
            startQrLogin: function (onUnavailable) {
                var self = this;
                self.resetQrScannedState();
                self.qrStatus = '正在加载二维码...';
                self.stopNotify();
                var createUrl = prefix + '/ticket/create';
                var createData = { callback: options.callback || '' };
                if (options.type) {
                    createData.type = options.type;
                }
                if (options.id) {
                    createData.id = options.id;
                }
                if (options.credentialType) {
                    createData.type = options.credentialType;
                }
                if (options.credentialId) {
                    createData.id = options.credentialId;
                }
                var payload = mode === 'rp'
                    ? { data: createData }
                    : { data: buildAsCreatePayload(options) };
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
                        var qrUrl = res.data.qrUrl;
                        var rendered = false;
                        var ensureRender = function () {
                            if (rendered) {
                                return;
                            }
                            rendered = true;
                            self.renderQrCode(qrUrl);
                            if (self.qrPhase === 'pending') {
                                self.qrStatus = '等待扫码...';
                            }
                        };
                        // SSE 先挂：唤起期间 App scan 即可推送到浏览器（同一 uuid）。
                        self.startTicketNotify(onUnavailable);
                        var wakeHandled = false;
                        if (typeof options.wakeClient === 'function') {
                            self.qrStatus = options.wakeStatusText || '正在打开客户端...';
                            try {
                                wakeHandled = options.wakeClient({
                                    uuid: self.qrcUuid,
                                    qrUrl: qrUrl,
                                    host: self,
                                    renderQr: ensureRender,
                                    // 建票下发的 qrc_client_grant.quick；无 client 时可能为 undefined
                                    quick: res.data.quick,
                                    probeNonce: res.data.probeNonce
                                }) === true;
                            } catch (wakeErr) {
                                wakeHandled = false;
                            }
                        }
                        if (!wakeHandled) {
                            ensureRender();
                        }
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
                    self.processAsStatusData(res.data, onUnavailable);
                });
            },
            processAsStatusData: function (data, onUnavailable) {
                var self = this;
                if (!data || self.qrPhase === 'done') {
                    return;
                }
                if (data.status === 'SCANNED') {
                    self.applyScannerBrief(data.scannerBrief);
                    if (self.appQuickActive) {
                        self.appQuickPhase = 'waiting';
                        self.appQuickHint = '等待超然信确认…';
                        self.appQuickBusy = true;
                    } else {
                        self.setQrStatus(isOpenCredential(options) ? '扫码成功，请在手机点击确认授权' : '扫码成功，请在手机点击登录');
                    }
                    self.ensureConfirmWatch(onUnavailable);
                }
                if (isOpenCredential(options) && data.status === 'COMPLETED' && data.result && data.result.code) {
                    self.completeOpenQrcLogin(data.result.code, onUnavailable);
                    return;
                }
                if (typeof options.onAuthorizeExchange === 'function' && (data.status === 'CONFIRMED' || data.status === 'COMPLETED') && data.exchange) {
                    self.stopNotify();
                    if (self.appQuickActive) {
                        self.appQuickHint = '登录成功，正在跳转…';
                    }
                    options.onAuthorizeExchange(data.exchange, self);
                    return;
                }
                if ((data.status === 'CONFIRMED' || data.status === 'COMPLETED') && data.exchange) {
                    self.finishSessionExchange(data.exchange, onUnavailable);
                    return;
                }
                if ((data.status === 'CONFIRMED' || data.status === 'COMPLETED') && !data.exchange) {
                    // 无 exchange 但已完成：会话可能已建立，刷新避免卡死
                    if (self.appQuickActive) {
                        self.appQuickHint = '登录成功，正在刷新…';
                    }
                    self.recoverAfterMissingSession();
                    return;
                }
                self.handleQrTerminalStatus(data.status);
            },
            completeOpenQrcLogin: function (code, onUnavailable) {
                var self = this;
                var credType = resolveCredentialType(options);
                var credId = resolveCredentialId(options);
                if (!credType || !credId) {
                    self.qrStatus = '扫码凭证未配置，请刷新重试';
                    return;
                }
                self.qrPhase = 'done';
                self.qrStatus = '授权成功，正在跳转...';
                self.stopNotify();
                $.ajax({
                    type: 'POST',
                    url: (ctx || '') + '/open/oauth2/qrc/web/complete',
                    contentType: 'application/json',
                    data: JSON.stringify({
                        data: {
                            type: credType,
                            id: credId,
                            code: code,
                            callback: options.callback || ''
                        }
                    }),
                    dataType: 'json',
                    success: function (result) {
                        if (result.code === 0) {
                            var target = result.data != null ? String(result.data) : null;
                            if (typeof options.onSuccess === 'function') {
                                try {
                                    options.onSuccess(target, self);
                                } catch (e) {
                                    /* ignore */
                                }
                            }
                            self.forceNavigate(target || 'index.html');
                        } else {
                            self.qrStatus = result.msg || '登录失败';
                        }
                    },
                    error: function () {
                        if (typeof onUnavailable === 'function') {
                            onUnavailable(true);
                            return;
                        }
                        self.qrStatus = '网络异常，请刷新二维码重试';
                        window.setTimeout(function () {
                            self.recoverAfterMissingSession();
                        }, 500);
                    }
                });
            },
            startTicketNotify: function (onUnavailable) {
                this.startSseNotify(onUnavailable);
            },
            startPollFallback: function (onUnavailable) {
                var self = this;
                if (self.qrcPollTimer || self.qrPhase === 'done') {
                    return;
                }
                if (self.qrcNotifyChannel === 'sse') {
                    self.qrcNotifyChannel = 'poll';
                } else if (!self.qrcNotifyChannel) {
                    self.qrcNotifyChannel = 'poll';
                }
                self.qrcPollTimer = setInterval(function () {
                    if (self.qrPhase === 'done') {
                        return;
                    }
                    self.pollQrStatus(onUnavailable);
                }, pollIntervalMs);
            },
            startPollOnly: function (onUnavailable) {
                var self = this;
                self.stopNotify();
                self.qrcNotifyChannel = 'poll';
                self.startPollFallback(onUnavailable);
            },
            startAsPoll: function (onUnavailable) {
                this.startPollOnly(onUnavailable);
            },
            scheduleSseFallback: function (onUnavailable) {
                var self = this;
                self.clearSseFallbackTimer();
                if (self.sseReceivedStatus || self.qrPhase === 'done') {
                    return;
                }
                self.sseFallbackTimer = setTimeout(function () {
                    self.sseFallbackTimer = null;
                    if (!self.sseReceivedStatus && self.qrPhase !== 'done') {
                        self.startPollFallback(onUnavailable);
                    }
                }, sseFallbackDelayMs);
            },
            markSseHealthy: function (data) {
                if (!data || !data.status || data.status === 'PENDING') {
                    return;
                }
                this.sseReceivedStatus = true;
                this.clearSseFallbackTimer();
            },
            startSseNotify: function (onUnavailable) {
                var self = this;
                if (!self.qrcUuid) {
                    return;
                }
                if (typeof EventSource === 'undefined') {
                    self.startPollFallback(onUnavailable);
                    return;
                }
                // 同票已在听：不重连，避免无故 close → 服务端 Broken pipe、扫码丢推送
                if (self.qrcEventSource && self.qrcNotifyChannel === 'sse' && self._sseUuid === self.qrcUuid) {
                    return;
                }
                self.stopNotify();
                self.qrcNotifyChannel = 'sse';
                self._sseUuid = self.qrcUuid;
                var streamUrl = prefix + '/ticket/stream?uuid=' + encodeURIComponent(self.qrcUuid);
                var es = new EventSource(streamUrl);
                self.qrcEventSource = es;
                es.addEventListener('status', function (ev) {
                    if (!ev || !ev.data) {
                        return;
                    }
                    var data;
                    try {
                        data = JSON.parse(ev.data);
                    } catch (e) {
                        return;
                    }
                    self.markSseHealthy(data);
                    self.handleStreamEvent(data, onUnavailable);
                });
                es.onopen = function () {
                    self.sseOpened = true;
                    self.scheduleSseFallback(onUnavailable);
                };
                es.onerror = function () {
                    if (self.qrPhase === 'done') {
                        return;
                    }
                    if (sseFallbackOnError) {
                        self.startPollFallback(onUnavailable);
                    }
                };
            },
            startRpNotify: function (onUnavailable) {
                this.startSseNotify(onUnavailable);
            },
            subscribeRpStream: function (onUnavailable) {
                this.startSseNotify(onUnavailable);
            },
            handleStreamEvent: function (data, onUnavailable) {
                if (mode === 'rp') {
                    this.handleRpStreamEvent(data, onUnavailable);
                    return;
                }
                this.handleAsStreamEvent(data, onUnavailable);
            },
            handleAsStreamEvent: function (data, onUnavailable) {
                this.processAsStatusData(data, onUnavailable);
            },
            resumeTicketNotify: function (resumeOpts) {
                resumeOpts = resumeOpts || {};
                var self = this;
                self.stopNotify();
                self.resetQrScannedState();
                if (resumeOpts.uuid) {
                    self.qrcUuid = resumeOpts.uuid;
                }
                if (!self.qrcUuid) {
                    self.qrStatus = '二维码加载失败，请刷新页面重试';
                    return;
                }
                var qrUrl = resumeOpts.qrUrl || '';
                var skipRender = resumeOpts.skipRender === true;
                var rendered = skipRender;
                var ensureRender = function () {
                    if (rendered || !qrUrl) {
                        if (!rendered && self.qrPhase === 'pending') {
                            self.qrStatus = resumeOpts.statusText || resumeOpts.qrStatus || '等待扫码...';
                        }
                        return;
                    }
                    rendered = true;
                    self.renderQrCode(qrUrl);
                    if (self.qrPhase === 'pending') {
                        self.qrStatus = resumeOpts.statusText || resumeOpts.qrStatus || '等待扫码...';
                    }
                };
                self.startTicketNotify(resumeOpts.onUnavailable);
                var wakeHandled = false;
                var allowWake = resumeOpts.wake !== false && typeof options.wakeClient === 'function';
                if (allowWake && qrUrl && !skipRender) {
                    self.qrStatus = options.wakeStatusText || '正在打开客户端...';
                    try {
                        wakeHandled = options.wakeClient({
                            uuid: self.qrcUuid,
                            qrUrl: qrUrl,
                            host: self,
                            renderQr: ensureRender,
                            quick: resumeOpts.quick,
                            probeNonce: resumeOpts.probeNonce
                        }) === true;
                    } catch (wakeErr) {
                        wakeHandled = false;
                    }
                }
                if (!wakeHandled) {
                    if (qrUrl && !skipRender) {
                        ensureRender();
                    } else {
                        self.qrStatus = resumeOpts.statusText || resumeOpts.qrStatus || '等待扫码...';
                    }
                }
            },
            handleRpStreamEvent: function (data, onUnavailable) {
                var self = this;
                if (!data || self.qrPhase === 'done') {
                    return;
                }
                if (data.status === 'SCANNED') {
                    self.markQrScanned(data.scannerBrief, '扫码成功，请在手机点击确认授权');
                    self.ensureConfirmWatch(onUnavailable);
                }
                if (data.status === 'SESSION_EXPIRED') {
                    self.resetQrScannedState();
                    self.setQrStatus('登录会话已过期，请刷新二维码重试');
                    self.stopNotify();
                    return;
                }
                if (data.status === 'COMPLETED') {
                    var target = data.redirectUrl || data.redirect || '/';
                    self.completeQrRedirect(target);
                    return;
                }
                self.handleQrTerminalStatus(data.status);
            },
            pollRpStatus: function (onUnavailable) {
                var self = this;
                if (!self.qrcUuid) {
                    return;
                }
                $.ajax({
                    type: 'GET',
                    url: prefix + '/ticket/status',
                    data: { uuid: self.qrcUuid },
                    dataType: 'json',
                    success: function (res) {
                        if (!res || res.code !== 0 || !res.data) {
                            var msg = res && res.msg ? String(res.msg) : '';
                            if (msg.indexOf('不存在') >= 0 || msg.indexOf('过期') >= 0) {
                                if (self.qrPhase === 'scanned' || self.qrPhase === 'done') {
                                    self.recoverAfterMissingSession();
                                    return;
                                }
                                self.resetQrScannedState();
                                self.setQrStatus(msg || '扫码会话不存在或已过期，请刷新重试');
                                self.stopNotify();
                            }
                            return;
                        }
                        var data = res.data;
                        if (data.redirect && !data.redirectUrl) {
                            data.redirectUrl = data.redirect;
                        }
                        self.handleRpStreamEvent(data, onUnavailable);
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
                    self.stopNotify();
                    return;
                }
                $.ajax({
                    type: 'POST',
                    url: prefix + '/ticket/cancel',
                    data: { uuid: self.qrcUuid },
                    complete: function () {
                        self.stopNotify();
                    }
                });
            }
        };
    }

    window.AutumnQrc = {
        createMethods: createQrMethods,
        renderInto: renderIntoBox,
        wireVueScannerUi: wireVueScannerUi,
        bindPlainHostScannedUi: bindPlainHostScannedUi,
        mergeInto: function (target, options) {
            options = options || {};
            var methods = createQrMethods(options);
            Object.keys(methods).forEach(function (key) {
                target[key] = methods[key];
            });
            if (target && typeof target.$set === 'function') {
                wireVueScannerUi(target, options.ctx || '');
            }
            return target;
        }
    };
})(window);
