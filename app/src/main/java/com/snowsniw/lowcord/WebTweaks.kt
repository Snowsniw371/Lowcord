package com.snowsniw.lowcord

import android.webkit.WebView

object WebTweaks {

    private const val BASE_UI_CSS = """
        body.lowcord-hide-sidebar [class*="guilds_"],
        body.lowcord-hide-sidebar [class*="sidebar_"] {
            display: none !important;
        }
        #lowcord-controls {
            position: fixed;
            left: 8px;
            top: 50%;
            transform: translateY(-50%);
            z-index: 99999;
            display: none;
            flex-direction: column;
            gap: 8px;
        }
        .lowcord-fab {
            width: 36px;
            height: 36px;
            border-radius: 18px;
            background: rgba(30, 31, 34, 0.82);
            color: #ffffff;
            border: 1px solid rgba(255, 255, 255, 0.18);
            font-size: 17px;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 0;
        }
        body.lowcord-hide-sidebar #lowcord-settings-btn {
            display: none !important;
        }
    """

    private const val PERFORMANCE_CSS = """
        *, *::before, *::after {
            backdrop-filter: none !important;
            -webkit-backdrop-filter: none !important;
            animation-duration: 0.01ms !important;
            animation-iteration-count: 1 !important;
            transition-duration: 0.01ms !important;
            scroll-behavior: auto !important;
        }
    """

    private const val AMOLED_CSS = """
        .theme-dark {
            --background-primary: #000000 !important;
            --background-secondary: #000000 !important;
            --background-secondary-alt: #080808 !important;
            --background-tertiary: #000000 !important;
            --background-floating: #0a0a0a !important;
            --channeltextarea-background: #111111 !important;
            --bg-base-primary: #000000 !important;
            --bg-base-secondary: #000000 !important;
            --bg-base-tertiary: #000000 !important;
            --bg-surface-overlay: #0a0a0a !important;
            --chat-background-default: #000000 !important;
        }
        html, body, [class*="app_"], [class*="chat_"], [class*="content_"],
        [class*="container_"], [class*="scroller_"], [class*="sidebar_"], [class*="guilds_"] {
            background-color: #000000 !important;
        }
    """

    fun spoofDesktopBrowser(webView: WebView?) {
        webView ?: return
        val js = """
            (function() {
                try {
                    Object.defineProperty(navigator, 'platform', { get: function() { return 'Win32'; } });
                    Object.defineProperty(navigator, 'maxTouchPoints', { get: function() { return 0; } });
                    if (navigator.userAgentData) {
                        Object.defineProperty(navigator, 'userAgentData', {
                            get: function() {
                                return {
                                    brands: [
                                        { brand: 'Google Chrome', version: '131' },
                                        { brand: 'Chromium', version: '131' },
                                        { brand: 'Not_A Brand', version: '24' }
                                    ],
                                    mobile: false,
                                    platform: 'Windows',
                                    getHighEntropyValues: function() {
                                        return Promise.resolve({
                                            architecture: 'x86',
                                            bitness: '64',
                                            mobile: false,
                                            model: '',
                                            platform: 'Windows',
                                            platformVersion: '15.0.0',
                                            uaFullVersion: '131.0.0.0'
                                        });
                                    }
                                };
                            }
                        });
                    }
                } catch (e) {}

                if (!window.lowcordMicHooked && navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
                    window.lowcordMicHooked = true;
                    window.lowcordActiveTracks = 0;
                    var origGetUserMedia = navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices);

                    navigator.mediaDevices.getUserMedia = function(constraints) {
                        return origGetUserMedia(constraints).then(function(stream) {
                            var audioTracks = stream.getAudioTracks();
                            if (audioTracks.length > 0) {
                                window.lowcordActiveTracks += audioTracks.length;
                                if (window.LowcordBridge) window.LowcordBridge.onVoiceStarted();

                                audioTracks.forEach(function(track) {
                                    var origStop = track.stop.bind(track);
                                    var handled = false;
                                    function onTrackEnd() {
                                        if (handled) return;
                                        handled = true;
                                        window.lowcordActiveTracks = Math.max(0, window.lowcordActiveTracks - 1);
                                        if (window.lowcordActiveTracks === 0 && window.LowcordBridge) {
                                            window.LowcordBridge.onVoiceStopped();
                                        }
                                    }
                                    track.stop = function() {
                                        onTrackEnd();
                                        return origStop();
                                    };
                                    track.addEventListener('ended', onTrackEnd);
                                });
                            }
                            return stream;
                        });
                    };
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }

    fun setStyleEnabled(webView: WebView?, styleId: String, css: String, enabled: Boolean) {
        webView ?: return
        val cleanCss = css.trimIndent().replace("\n", " ")
        val js = if (enabled) {
            """
            (function() {
                var style = document.getElementById('$styleId');
                if (!style) {
                    style = document.createElement('style');
                    style.id = '$styleId';
                    style.type = 'text/css';
                    style.appendChild(document.createTextNode('$cleanCss'));
                    (document.head || document.documentElement).appendChild(style);
                }
            })();
            """.trimIndent()
        } else {
            """
            (function() {
                var style = document.getElementById('$styleId');
                if (style) style.remove();
            })();
            """.trimIndent()
        }
        webView.evaluateJavascript(js, null)
    }

    fun applyThemeAndPerf(webView: WebView?, perfEnabled: Boolean, amoledEnabled: Boolean) {
        setStyleEnabled(webView, "lowcord-perf-style", PERFORMANCE_CSS, perfEnabled)
        setStyleEnabled(webView, "lowcord-amoled-style", AMOLED_CSS, amoledEnabled)
    }

    fun applyAll(webView: WebView?, perfEnabled: Boolean, amoledEnabled: Boolean) {
        webView ?: return
        spoofDesktopBrowser(webView)
        setStyleEnabled(webView, "lowcord-base-style", BASE_UI_CSS, true)
        applyThemeAndPerf(webView, perfEnabled, amoledEnabled)

        val js = """
            (function() {
                if (!window.lowcordDrawerInit && document.body) {
                    window.lowcordDrawerInit = true;

                    var box = document.createElement('div');
                    box.id = 'lowcord-controls';

                    var drawerBtn = document.createElement('button');
                    drawerBtn.id = 'lowcord-drawer-btn';
                    drawerBtn.className = 'lowcord-fab';
                    drawerBtn.innerText = '☰';
                    drawerBtn.onclick = function() {
                        document.body.classList.toggle('lowcord-hide-sidebar');
                    };

                    var pressTimer;
                    drawerBtn.addEventListener('touchstart', function() {
                        pressTimer = setTimeout(function() {
                            if (window.LowcordBridge) window.LowcordBridge.openSettings();
                        }, 600);
                    }, { passive: true });
                    drawerBtn.addEventListener('touchend', function() {
                        clearTimeout(pressTimer);
                    }, { passive: true });

                    var settingsBtn = document.createElement('button');
                    settingsBtn.id = 'lowcord-settings-btn';
                    settingsBtn.className = 'lowcord-fab';
                    settingsBtn.innerText = '⚙';
                    settingsBtn.onclick = function() {
                        if (window.LowcordBridge) window.LowcordBridge.openSettings();
                    };

                    box.appendChild(drawerBtn);
                    box.appendChild(settingsBtn);
                    document.body.appendChild(box);

                    setInterval(function() {
                        var hasSidebar = document.querySelector('[class*="sidebar_"]');
                        box.style.display = hasSidebar ? 'flex' : 'none';
                    }, 1000);

                    document.addEventListener('click', function(e) {
                        var sidebar = e.target.closest ? e.target.closest('[class*="sidebar_"]') : null;
                        if (!sidebar) return;

                        var el = e.target;
                        while (el && el !== sidebar) {
                            var href = el.getAttribute ? el.getAttribute('href') : null;
                            var listId = el.getAttribute ? el.getAttribute('data-list-item-id') : null;
                            if ((href && href.indexOf('/channels/') !== -1) ||
                                (listId && (listId.indexOf('channels___') === 0 || listId.indexOf('private-channels-') === 0))) {
                                setTimeout(function() {
                                    document.body.classList.add('lowcord-hide-sidebar');
                                }, 120);
                                break;
                            }
                            el = el.parentElement;
                        }
                    }, true);

                    var startX = 0;
                    var startY = 0;
                    document.addEventListener('touchstart', function(e) {
                        if (e.touches.length === 1) {
                            startX = e.touches[0].clientX;
                            startY = e.touches[0].clientY;
                        }
                    }, { passive: true });

                    document.addEventListener('touchend', function(e) {
                        if (e.changedTouches.length === 1) {
                            var dx = e.changedTouches[0].clientX - startX;
                            var dy = Math.abs(e.changedTouches[0].clientY - startY);
                            if (Math.abs(dx) > 75 && dy < 50) {
                                if (dx > 0) {
                                    document.body.classList.remove('lowcord-hide-sidebar');
                                } else {
                                    document.body.classList.add('lowcord-hide-sidebar');
                                }
                            }
                        }
                    }, { passive: true });
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }
}

