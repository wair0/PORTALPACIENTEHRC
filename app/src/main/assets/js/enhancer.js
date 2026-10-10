/**
 * Enhancer v2 - ocultamiento agresivo del header oficial
 */
(function () {
  if (window.__hrcEnhancerLoaded) return;
  window.__hrcEnhancerLoaded = true;

  function isLogin() { return location.pathname.replace(/\/+$/, '') === '/login'; }
  function log(m) { try { console.log('[enhancer] ' + m); } catch (e) {} }

  function hideHeaders() {
    if (isLogin()) return;

    // Ocultar por selectores comunes
    var selectors = [
      'header', '.header', '.navbar', '.site-header', '.app-header',
      '[class*="Header"]', '[class*="header"]', '[class*="Navbar"]',
      '[class*="TopBar"]', '[class*="AppBar"]', 'footer', '.footer'
    ];
    selectors.forEach(function(sel) {
      document.querySelectorAll(sel).forEach(function(el) {
        el.style.setProperty('display', 'none', 'important');
        el.setAttribute('data-hrc-hidden', '1');
      });
    });

    // Detectar por posición (barra superior)
    var w = window.innerWidth;
    var el = document.elementFromPoint(w / 2, 8);
    if (el && el !== document.body && el !== document.documentElement) {
      var cur = el;
      for (var i = 0; i < 8 && cur; i++) {
        var r = cur.getBoundingClientRect();
        if (r.top < 10 && r.height > 20 && r.height < 100 && r.width > w * 0.6) {
          cur.style.setProperty('display', 'none', 'important');
          cur.setAttribute('data-hrc-hidden', '1');
          log('header posicional oculto');
          break;
        }
        cur = cur.parentElement;
      }
    }
  }

  function tryReportProfile() {
    try {
      var nameEl = document.querySelector('.user-name, .profile-name, .patient-name, .welcome');
      var name = nameEl ? nameEl.textContent.trim() : '';
      if (name && window.HRCBridge && window.HRCBridge.onProfile) {
        window.HRCBridge.onProfile(JSON.stringify({ name: name }));
      }
    } catch (e) {}
  }

  setTimeout(hideHeaders, 500);
  setTimeout(hideHeaders, 1500);
  setTimeout(hideHeaders, 3000);
  setTimeout(tryReportProfile, 2000);

  log('enhancer loaded');
})();
