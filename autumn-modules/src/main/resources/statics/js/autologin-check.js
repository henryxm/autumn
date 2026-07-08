(function (window) {
    'use strict';

    function shouldSkipAutologin(cookieName) {
        try {
            return document.cookie.indexOf((cookieName || 'autumn_skip_autologin') + '=1') >= 0;
        } catch (e) {
            return false;
        }
    }

    function runAutologinCheck(options) {
        options = options || {};
        if (!options.autologinEnabled) {
            return;
        }
        if (options.oauthLogin || options.authorizeMode) {
            return;
        }
        if (options.shouldSkip && options.shouldSkip()) {
            return;
        }
        if (shouldSkipAutologin(options.skipAutologinCookie)) {
            return;
        }
        var ctx = options.ctx || '';
        var onRedirect = options.onRedirect || function () {};
        var onDevProbe = options.onDevProbe || function () {};
        $.ajax({
            type: 'POST',
            url: ctx + '/sys/autologin',
            dataType: 'json',
            success: function (result) {
                if (!result || result.code != 0) {
                    return;
                }
                if (result.data != null && String(result.data) !== '') {
                    onRedirect(String(result.data));
                    return;
                }
                if (result.devProbe) {
                    onDevProbe();
                }
            }
        });
    }

    window.AutologinCheck = {
        shouldSkip: shouldSkipAutologin,
        run: runAutologinCheck
    };
})(window);
