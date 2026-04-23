(function (window) {
    'use strict';

    function parseJson(text) {
        try {
            return JSON.parse(text || '{}');
        } catch (e) {
            return {};
        }
    }

    function resolveBasePath() {
        var pathname = window.location.pathname || '';
        if (pathname === '/' || pathname === '') {
            return '';
        }
        var idx = pathname.lastIndexOf('/');
        if (idx <= 0) {
            return '';
        }
        return pathname.substring(0, idx);
    }

    function redirectToLogin(reason) {
        var base = resolveBasePath();
        var loginUrl = (base ? base : '') + '/login.html?sessionExpired=1';
        if (reason) {
            loginUrl += '&reason=' + encodeURIComponent(reason);
        }
        try {
            window.sessionStorage.setItem('autumn_session_expired', '1');
            if (reason) {
                window.sessionStorage.setItem('autumn_session_expired_reason', reason);
            }
        } catch (e) {
        }
        window.location.href = loginUrl;
    }

    function createPingTask() {
        var xhr = new XMLHttpRequest();
        xhr.open('GET', 'sys/session/self/ping?_=' + Date.now(), true);
        xhr.setRequestHeader('Accept', 'application/json');
        xhr.onload = function () {
            var body = parseJson(xhr.responseText || '{}');
            if (xhr.status === 401 || Number(body.code) === 401) {
                var reason = body.reason ? String(body.reason) : 'session_expired';
                redirectToLogin(reason);
            }
        };
        xhr.onerror = function () {
        };
        xhr.send();
    }

    window.AutumnSessionGuard = {
        start: function (intervalMs) {
            var every = parseInt(intervalMs, 10);
            if (!(every >= 15000)) {
                every = 60000;
            }
            createPingTask();
            window.setInterval(createPingTask, every);
        }
    };
})(window);
