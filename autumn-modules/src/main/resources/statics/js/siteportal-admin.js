(function(global) {
    'use strict';

    var PREVIEW_DEBOUNCE_MS = 300;

    var FILING_TYPE_LABELS = {
        icp: 'ICP 备案',
        psb: '公安备案（网安备）',
        app_icp: 'App 备案',
        algorithm: '算法备案',
        icp_license: 'ICP 经营许可证',
        telecom: '增值电信业务经营许可证',
        network_culture: '网络文化经营许可证',
        audiovisual: '信息网络传播视听节目许可证',
        broadcast_tv: '广播电视节目制作经营许可证',
        online_performance: '网络表演/互联网直播许可',
        game_isbn: '网络游戏版号（ISBN）',
        news_license: '互联网新闻信息服务许可证',
        publish_license: '网络出版服务许可证',
        drug_info: '互联网药品信息服务资格证',
        medical_device: '医疗器械经营/网络服务许可',
        medical_ad: '医疗广告审查证明',
        food_license: '食品经营许可证',
        food_production: '食品生产许可证',
        special_food: '特殊食品注册/备案',
        internet_religion: '互联网宗教信息服务许可证',
        map_approval: '地图审图号/测绘资质',
        custom: '自定义'
    };

    var FILING_TYPE_GROUPS = [
        { label: '基础网信', ids: ['icp', 'psb', 'app_icp', 'algorithm'] },
        { label: '电信与经营许可', ids: ['icp_license', 'telecom'] },
        { label: '广电与网络文化', ids: ['network_culture', 'audiovisual', 'broadcast_tv', 'online_performance', 'game_isbn'] },
        { label: '新闻出版', ids: ['news_license', 'publish_license'] },
        { label: '药品与医疗器械', ids: ['drug_info', 'medical_device', 'medical_ad'] },
        { label: '食品相关', ids: ['food_license', 'food_production', 'special_food'] },
        { label: '其他合规', ids: ['internet_religion', 'map_approval', 'custom'] }
    ];

    function buildFilingTypeOptions(selected) {
        var html = FILING_TYPE_GROUPS.map(function(group) {
            var opts = group.ids.map(function(id) {
                var label = FILING_TYPE_LABELS[id] || id;
                return '<option value="' + id + '"' + (selected === id ? ' selected' : '') + '>' + label + '</option>';
            }).join('');
            return '<optgroup label="' + group.label + '">' + opts + '</optgroup>';
        }).join('');
        if (selected && !FILING_TYPE_LABELS[selected]) {
            html = '<option value="' + escapeAttr(selected) + '" selected>' + escapeAttr(selected) + '（已废弃/未知）</option>' + html;
        }
        return html;
    }

    function initSitePortalAdmin(ctx) {
        ctx = ctx || '';
        var API = ctx + '/sys/site-portal';
        var state = { syncLoadingTheme: true, filings: [] };
        var defaultFilingUrls = {};

        function toast(msg, ok) {
            var el = document.getElementById('toast');
            el.textContent = msg || '';
            el.className = 'toast ' + (ok ? 'ok' : msg ? 'err' : '');
        }

        function api(path, options) {
            return fetch(API + path, Object.assign({ credentials: 'same-origin', headers: { 'Content-Type': 'application/json' } }, options || {}))
                .then(function(r) { return r.json(); });
        }

        function escapeAttr(value) {
            if (!value) {
                return '';
            }
            return String(value).replace(/"/g, '&quot;');
        }

        function filingTemplate(item, index) {
            var isPsb = item.type === 'psb';
            var typeOptions = buildFilingTypeOptions(item.type);
            return '<div class="filing-item" data-index="' + index + '">' +
                '<div class="filing-head"><strong>备案项 #' + (index + 1) + '</strong><button type="button" class="btn btn-danger btn-remove">删除</button></div>' +
                '<div class="grid filing-grid">' +
                '<div class="field"><label>类型</label><select class="f-type">' + typeOptions + '</select></div>' +
                '<div class="field"><label>展示文案</label><input class="f-number" value="' + escapeAttr(item.number || '') + '" placeholder="蜀ICP备... / 川公网安备..."></div>' +
                '<div class="field"><label>前缀（可选，不参与链接）</label><input class="f-prefix" value="' + escapeAttr(item.prefix || '') + '" placeholder="如：增值电信业务经营许可证:"></div>' +
                '<div class="field"><label>后缀（可选，不参与链接）</label><input class="f-suffix" value="' + escapeAttr(item.suffix || '') + '" placeholder="如：号"></div>' +
                '<div class="field"><label>跳转地址</label><input class="f-url" value="' + escapeAttr(item.url || '') + '" placeholder="留空使用类型默认地址"></div>' +
                '<div class="field"><label>预览地址</label><input class="f-preview" readonly value="" placeholder="根据类型与文案自动解析"></div>' +
                '<div class="field full f-icon-field"' + (isPsb ? '' : ' hidden') + '><label class="f-checkbox-label"><input type="checkbox" class="f-icon"' + (isPsb && item.showIcon !== false ? ' checked' : '') + (isPsb ? '' : ' disabled') + '><span>公安备案显示盾牌图标</span></label></div>' +
                '</div></div>';
        }

        function readForm() {
            return {
                syncLoadingTheme: document.getElementById('syncLoadingTheme').checked,
                branding: {
                    siteName: document.getElementById('siteName').value.trim(),
                    tagline: document.getElementById('tagline').value.trim(),
                    logoUrl: document.getElementById('logoUrl').value.trim(),
                    logoAlt: document.getElementById('logoAlt').value.trim()
                },
                meta: {
                    copyrightHolder: document.getElementById('copyrightHolder').value.trim(),
                    copyrightYearStart: document.getElementById('copyrightYearStart').value.trim(),
                    copyrightYearEnd: document.getElementById('copyrightYearEnd').value.trim(),
                    versionLabel: document.getElementById('versionLabel').value.trim()
                },
                legalLinks: {
                    privacyUrl: document.getElementById('privacyUrl').value.trim(),
                    termsUrl: document.getElementById('termsUrl').value.trim(),
                    aboutUrl: document.getElementById('aboutUrl').value.trim(),
                    helpUrl: document.getElementById('helpUrl').value.trim(),
                    contactUrl: document.getElementById('contactUrl').value.trim()
                },
                filings: state.filings
            };
        }

        function readFilingFromElement(el) {
            var type = el.querySelector('.f-type').value;
            var iconEl = el.querySelector('.f-icon');
            return {
                type: type,
                number: el.querySelector('.f-number').value.trim(),
                prefix: el.querySelector('.f-prefix').value.trim(),
                suffix: el.querySelector('.f-suffix').value.trim(),
                url: el.querySelector('.f-url').value.trim(),
                showIcon: type === 'psb' && iconEl ? iconEl.checked : false
            };
        }

        function updateFilingIconField(el, type) {
            var field = el.querySelector('.f-icon-field');
            var icon = el.querySelector('.f-icon');
            if (!field || !icon) {
                return;
            }
            var isPsb = type === 'psb';
            field.hidden = !isPsb;
            icon.disabled = !isPsb;
            if (!isPsb) {
                icon.checked = false;
            } else if (!icon.checked) {
                icon.checked = true;
            }
        }

        function collectFilingsFromDom() {
            document.querySelectorAll('.filing-item').forEach(function(el) {
                var idx = parseInt(el.getAttribute('data-index'), 10);
                state.filings[idx] = readFilingFromElement(el);
            });
        }

        function fillForm(data) {
            state.filings = (data.filings || []).slice();
            document.getElementById('syncLoadingTheme').checked = data.syncLoadingTheme !== false;
            document.getElementById('siteName').value = (data.branding && data.branding.siteName) || '';
            document.getElementById('tagline').value = (data.branding && data.branding.tagline) || '';
            document.getElementById('logoUrl').value = (data.branding && data.branding.logoUrl) || '';
            document.getElementById('logoAlt').value = (data.branding && data.branding.logoAlt) || '';
            document.getElementById('copyrightHolder').value = (data.meta && data.meta.copyrightHolder) || '';
            document.getElementById('copyrightYearStart').value = (data.meta && data.meta.copyrightYearStart) || '';
            document.getElementById('copyrightYearEnd').value = (data.meta && data.meta.copyrightYearEnd) || '';
            document.getElementById('versionLabel').value = (data.meta && data.meta.versionLabel) || '';
            var legal = data.legalLinks || {};
            document.getElementById('privacyUrl').value = legal.privacyUrl || '';
            document.getElementById('termsUrl').value = legal.termsUrl || '';
            document.getElementById('aboutUrl').value = legal.aboutUrl || '';
            document.getElementById('helpUrl').value = legal.helpUrl || '';
            document.getElementById('contactUrl').value = legal.contactUrl || '';
            renderFilings();
        }

        function renderFilings() {
            var box = document.getElementById('filingsBox');
            box.innerHTML = state.filings.map(function(item, i) { return filingTemplate(item, i); }).join('');
            bindFilingEvents();
            global.AutumnIframeAutoHeight.scheduleFrameHeightSync();
        }

        function fetchFilingPreview(el, item) {
            var preview = el.querySelector('.f-preview');
            var seq = String((parseInt(el.dataset.previewSeq, 10) || 0) + 1);
            el.dataset.previewSeq = seq;
            var params = new URLSearchParams({ type: item.type || 'custom', number: item.number || '', url: '' });
            api('/preview-filing?' + params.toString()).then(function(res) {
                if (el.dataset.previewSeq !== seq) {
                    return;
                }
                preview.value = (res.code === 0 && res.url) ? res.url : (defaultFilingUrls[item.type] || '');
            }).catch(function() {
                if (el.dataset.previewSeq === seq) {
                    preview.value = defaultFilingUrls[item.type] || '';
                }
            });
        }

        function updateFilingPreview(el, item) {
            var preview = el.querySelector('.f-preview');
            var configured = (item.url || '').trim();
            if (configured) {
                if (el._previewTimer) {
                    clearTimeout(el._previewTimer);
                    el._previewTimer = null;
                }
                preview.value = configured;
                return;
            }
            if (el._previewTimer) {
                clearTimeout(el._previewTimer);
            }
            el._previewTimer = setTimeout(function() {
                el._previewTimer = null;
                fetchFilingPreview(el, item);
            }, PREVIEW_DEBOUNCE_MS);
        }

        function bindFilingEvents() {
            document.querySelectorAll('.filing-item').forEach(function(el) {
                var idx = parseInt(el.getAttribute('data-index'), 10);
                var sync = function() {
                    state.filings[idx] = readFilingFromElement(el);
                    updateFilingPreview(el, state.filings[idx]);
                };
                el.querySelectorAll('input,select').forEach(function(node) {
                    node.addEventListener('input', sync);
                });
                el.querySelectorAll('select').forEach(function(node) {
                    node.addEventListener('change', function() {
                        var urlInput = el.querySelector('.f-url');
                        if (!urlInput.value.trim() && defaultFilingUrls[this.value]) {
                            var def = defaultFilingUrls[this.value];
                            if (this.value !== 'psb' || def.indexOf('{recordcode}') < 0) {
                                urlInput.value = def;
                            }
                        }
                        updateFilingIconField(el, this.value);
                        sync();
                    });
                });
                el.querySelector('.btn-remove').addEventListener('click', function() {
                    state.filings.splice(idx, 1);
                    renderFilings();
                });
                sync();
            });
        }

        function loadAll() {
            toast('加载中...', true);
            Promise.all([api('/config'), api('/defaults')]).then(function(results) {
                var cfgRes = results[0];
                var defRes = results[1];
                if (cfgRes.code !== 0) {
                    throw new Error(cfgRes.msg || '加载失败');
                }
                defaultFilingUrls = (defRes.data && defRes.data.filingUrls) || {};
                var legalDefaults = (defRes.data && defRes.data.legalDefaults) || {};
                fillForm(cfgRes.data || {});
                if (!document.getElementById('privacyUrl').value) {
                    document.getElementById('privacyUrl').value = legalDefaults.privacyUrl || '/user/privacy.html';
                }
                if (!document.getElementById('termsUrl').value) {
                    document.getElementById('termsUrl').value = legalDefaults.termsUrl || '/user/service.html';
                }
                toast('已加载', true);
                global.AutumnIframeAutoHeight.scheduleFrameHeightSync();
            }).catch(function(e) {
                toast(e.message || '加载失败', false);
            });
        }

        document.getElementById('addFilingBtn').addEventListener('click', function() {
            state.filings.push({ type: 'icp', number: '', url: defaultFilingUrls.icp || '', showIcon: false });
            renderFilings();
        });
        document.getElementById('saveBtn').addEventListener('click', function() {
            collectFilingsFromDom();
            var payload = readForm();
            api('/save', { method: 'POST', body: JSON.stringify(payload) }).then(function(res) {
                if (res.code !== 0) {
                    throw new Error(res.msg || '保存失败');
                }
                fillForm(res.data || payload);
                toast('保存成功', true);
                global.AutumnIframeAutoHeight.scheduleFrameHeightSync();
            }).catch(function(e) {
                toast(e.message || '保存失败', false);
            });
        });
        document.getElementById('reloadBtn').addEventListener('click', loadAll);

        global.AutumnIframeAutoHeight.init();
        loadAll();
    }

    global.SitePortalAdmin = {
        init: initSitePortalAdmin
    };
})(typeof window !== 'undefined' ? window : this);
