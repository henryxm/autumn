(function (window) {
    'use strict';

    var catalogCache = {};

    function parseScopeValue(value) {
        if (!value) {
            return ['basic'];
        }
        return String(value).split(/[\s,;，；]+/).filter(function (part) {
            return part && part.trim();
        }).map(function (part) {
            return part.trim().toLowerCase();
        });
    }

    function basicCodesForTrack(track) {
        var key = track || 'oauth';
        var cached = catalogCache[key];
        if (cached && cached.basicCodes && cached.basicCodes.length) {
            return cached.basicCodes.slice();
        }
        return [];
    }

    function enabledCodes(items) {
        var codes = [];
        (items || []).forEach(function (item) {
            if (item && item.code && item.enabled !== false) {
                codes.push(item.code);
            }
        });
        return codes;
    }

    function expandScopeCodes(codes, track, items) {
        var enabledSet = {};
        enabledCodes(items).forEach(function (code) {
            enabledSet[code] = true;
        });
        var expanded = [];
        (codes || []).forEach(function (code) {
            if (code === 'basic') {
                basicCodesForTrack(track).forEach(function (c) {
                    if (enabledSet[c]) {
                        expanded.push(c);
                    }
                });
            } else if (code === 'all') {
                Object.keys(enabledSet).forEach(function (c) {
                    expanded.push(c);
                });
            } else if (enabledSet[code]) {
                expanded.push(code);
            }
        });
        var deduped = [];
        expanded.forEach(function (code) {
            if (deduped.indexOf(code) < 0) {
                deduped.push(code);
            }
        });
        return deduped.length ? deduped : basicCodesForTrack(track).filter(function (c) {
            return enabledSet[c];
        });
    }

    function sameSet(a, b) {
        if (!a || !b || a.length !== b.length) {
            return false;
        }
        var sortedA = a.slice().sort();
        var sortedB = b.slice().sort();
        for (var i = 0; i < sortedA.length; i++) {
            if (sortedA[i] !== sortedB[i]) {
                return false;
            }
        }
        return true;
    }

    function joinScopeValue(selected, track, items) {
        if (!selected || !selected.length) {
            return 'basic';
        }
        var allEnabled = enabledCodes(items);
        if (sameSet(selected, basicCodesForTrack(track).filter(function (c) {
            return allEnabled.indexOf(c) >= 0;
        }))) {
            return 'basic';
        }
        if (allEnabled.length && sameSet(selected, allEnabled)) {
            return 'all';
        }
        return selected.join(' ');
    }

    function findItem(items, code) {
        for (var i = 0; i < (items || []).length; i++) {
            if (items[i] && items[i].code === code) {
                return items[i];
            }
        }
        return null;
    }

    function findScopeFieldRoot(container) {
        if (!container) {
            return null;
        }
        return container.closest ? container.closest('.scope-field') : container.parentElement;
    }

    function sensClass(level) {
        if (level === 'high') {
            return 'scope-sens-high';
        }
        if (level === 'medium') {
            return 'scope-sens-medium';
        }
        return 'scope-sens-low';
    }

    function sensLabel(level) {
        if (level === 'high') {
            return '高敏感';
        }
        if (level === 'medium') {
            return '中敏感';
        }
        return '低敏感';
    }

    function updateSummary(summaryEl, selected, items) {
        if (!summaryEl) {
            return;
        }
        summaryEl.innerHTML = '';
        if (!selected || !selected.length) {
            var empty = document.createElement('span');
            empty.className = 'scope-summary-chip empty';
            empty.textContent = '未选择（将按基本授权处理）';
            summaryEl.appendChild(empty);
            return;
        }
        selected.forEach(function (code) {
            var item = findItem(items, code);
            var chip = document.createElement('span');
            chip.className = 'scope-summary-chip';
            chip.textContent = item && item.label ? item.label : code;
            summaryEl.appendChild(chip);
        });
    }

    function updateStoredValue(storedEl, storedText) {
        if (!storedEl) {
            return;
        }
        storedEl.textContent = storedText || 'basic';
    }

    function updatePresets(presetsEl, container, input, track, items, selected) {
        if (!presetsEl) {
            return;
        }
        presetsEl.innerHTML = '';
        var presets = [
            { code: 'basic', label: '基本授权（推荐）' },
            { code: 'all', label: '全部权限' }
        ];
        presets.forEach(function (preset) {
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'scope-preset-btn';
            btn.textContent = preset.label;
            var expanded = expandScopeCodes([preset.code], track, items);
            if (sameSet(selected, expanded)) {
                btn.classList.add('active');
            }
            btn.addEventListener('click', function () {
                renderPicker(container, input, items, expanded, track, presetsEl);
            });
            presetsEl.appendChild(btn);
        });
    }

    function syncInput(container, input, track, items, fieldRoot, presetsEl) {
        var selected = [];
        container.querySelectorAll('input[type=checkbox]').forEach(function (box) {
            if (box.checked) {
                selected.push(box.value);
            }
        });
        selected.sort();
        var stored = joinScopeValue(selected, track, items);
        input.value = stored;
        container.querySelectorAll('.scope-pick-row').forEach(function (row) {
            var box = row.querySelector('input[type=checkbox]');
            row.classList.toggle('checked', !!(box && box.checked));
        });
        if (fieldRoot) {
            updateSummary(fieldRoot.querySelector('.scope-field-summary'), selected, items);
            updateStoredValue(fieldRoot.querySelector('.scope-stored-text'), stored);
        }
        updatePresets(presetsEl, container, input, track, items, selected);
    }

    function renderPicker(container, input, items, selected, track, presetsEl) {
        if (!container || !input) {
            return;
        }
        var fieldRoot = findScopeFieldRoot(container);
        if (!presetsEl && fieldRoot) {
            presetsEl = fieldRoot.querySelector('.scope-presets');
        }
        container.innerHTML = '';
        var expanded = expandScopeCodes(selected, track, items);
        var selectedSet = {};
        expanded.forEach(function (code) {
            selectedSet[code] = true;
        });
        var visible = enabledCodes(items);
        if (!visible.length) {
            container.innerHTML = '<div class="scope-field-empty">暂无可选授权范围</div>';
            input.value = 'basic';
            if (fieldRoot) {
                updateSummary(fieldRoot.querySelector('.scope-field-summary'), [], items);
                updateStoredValue(fieldRoot.querySelector('.scope-stored-text'), 'basic');
            }
            return;
        }
        visible.forEach(function (code) {
            var item = findItem(items, code);
            if (!item) {
                return;
            }
            var row = document.createElement('div');
            row.className = 'scope-pick-row' + (selectedSet[code] ? ' checked' : '');
            var checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.value = code;
            checkbox.checked = !!selectedSet[code];
            checkbox.id = input.id + '_' + code;
            checkbox.addEventListener('change', function () {
                syncInput(container, input, track, items, fieldRoot, presetsEl);
            });
            var body = document.createElement('div');
            body.className = 'scope-pick-body';
            var title = document.createElement('label');
            title.className = 'scope-pick-title';
            title.setAttribute('for', checkbox.id);
            title.textContent = item.label || code;
            var meta = document.createElement('div');
            meta.className = 'scope-pick-meta';
            var codeEl = document.createElement('span');
            codeEl.className = 'scope-pick-code';
            codeEl.textContent = code;
            meta.appendChild(codeEl);
            if (item.sensitivity) {
                var sens = document.createElement('span');
                sens.className = 'scope-sens ' + sensClass(String(item.sensitivity).toLowerCase());
                sens.textContent = sensLabel(String(item.sensitivity).toLowerCase());
                meta.appendChild(sens);
            }
            body.appendChild(title);
            body.appendChild(meta);
            row.appendChild(checkbox);
            row.appendChild(body);
            row.addEventListener('click', function (e) {
                if (e.target === checkbox) {
                    return;
                }
                checkbox.checked = !checkbox.checked;
                syncInput(container, input, track, items, fieldRoot, presetsEl);
            });
            container.appendChild(row);
        });
        syncInput(container, input, track, items, fieldRoot, presetsEl);
    }

    function fetchCatalog(ctx, track) {
        var key = track || 'oauth';
        if (catalogCache[key]) {
            return Promise.resolve(catalogCache[key]);
        }
        var api = (ctx || '') + '/oauth/admin/scopes/catalog?track=' + encodeURIComponent(key);
        return fetch(api, { credentials: 'same-origin' })
            .then(function (res) { return res.json(); })
            .then(function (body) {
                var data = body && body.data ? body.data : {};
                var catalog = {
                    items: data.items || [],
                    basicCodes: data.basicCodes || null
                };
                catalogCache[key] = catalog;
                return catalog;
            });
    }

    function bindScopePicker(options) {
        var ctx = options.contextPath || '';
        var track = options.track || 'oauth';
        var pairs = options.pairs || [];
        fetchCatalog(ctx, track).then(function (catalog) {
            var items = catalog.items || [];
            pairs.forEach(function (pair) {
                var input = document.getElementById(pair.inputId);
                var container = document.getElementById(pair.containerId);
                if (!input || !container) {
                    return;
                }
                renderPicker(container, input, items, parseScopeValue(input.value), track, pair.presetsId ? document.getElementById(pair.presetsId) : null);
            });
        }).catch(function () {
            pairs.forEach(function (pair) {
                var container = document.getElementById(pair.containerId);
                if (container) {
                    container.innerHTML = '<div class="scope-field-empty">授权范围目录加载失败</div>';
                }
            });
        });
    }

    function refreshScopePicker(inputId, containerId, track, contextPath) {
        var input = document.getElementById(inputId);
        var container = document.getElementById(containerId);
        if (!input || !container) {
            return;
        }
        var ctx = contextPath || (window.__CTX__ || '');
        var resolvedTrack = track || 'oauth';
        fetchCatalog(ctx, resolvedTrack).then(function (catalog) {
            var items = catalog.items || [];
            var fieldRoot = findScopeFieldRoot(container);
            var presetsEl = fieldRoot ? fieldRoot.querySelector('.scope-presets') : null;
            renderPicker(container, input, items, parseScopeValue(input.value), resolvedTrack, presetsEl);
        });
    }

    function invalidateCatalog(track) {
        if (track) {
            delete catalogCache[track];
            return;
        }
        catalogCache = {};
    }

    window.AuthScopePicker = {
        bind: bindScopePicker,
        refresh: refreshScopePicker,
        invalidateCatalog: invalidateCatalog,
        parseScopeValue: parseScopeValue,
        joinScopeValue: joinScopeValue,
        renderPicker: renderPicker,
        expandScopeCodes: expandScopeCodes
    };
})(window);
