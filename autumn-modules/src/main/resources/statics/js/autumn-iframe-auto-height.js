(function(global) {
    'use strict';

    function measureDocumentHeight(doc) {
        doc = doc || document;
        var root = doc.documentElement;
        var body = doc.body;
        return Math.max(root.scrollHeight, root.offsetHeight, body ? body.scrollHeight : 0);
    }

    function syncParentFrameHeight() {
        if (global.parent === global) {
            return;
        }
        try {
            var h = measureDocumentHeight(document);
            var frame = global.frameElement;
            if (frame) {
                frame.setAttribute('scrolling', 'no');
                frame.style.overflow = 'visible';
                frame.style.height = h + 'px';
            }
            var resize = global.parent.resizeContentIframes;
            if (typeof resize === 'function') {
                resize();
            }
        } catch (e) {
            // ignore cross-origin or parent unavailable
        }
    }

    function scheduleFrameHeightSync() {
        global.requestAnimationFrame(syncParentFrameHeight);
    }

    function initAutumnIframeAutoHeight(options) {
        options = options || {};
        global.__AUTUMN_IFRAME_AUTO_HEIGHT__ = true;
        global.addEventListener('load', scheduleFrameHeightSync);
        global.addEventListener('resize', scheduleFrameHeightSync);
        if (typeof global.ResizeObserver !== 'undefined') {
            var target = options.observeTarget || document.body;
            new global.ResizeObserver(scheduleFrameHeightSync).observe(target);
        }
        scheduleFrameHeightSync();
    }

    global.AutumnIframeAutoHeight = {
        measureDocumentHeight: measureDocumentHeight,
        syncParentFrameHeight: syncParentFrameHeight,
        scheduleFrameHeightSync: scheduleFrameHeightSync,
        init: initAutumnIframeAutoHeight
    };
})(window);
