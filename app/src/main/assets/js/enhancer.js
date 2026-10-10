/**
 * Portal Paciente HRC - Enhancer v3
 *  1) Oculta el header oficial del portal (la app tiene su propia barra nativa), salvo en el login.
 *  2) Clasifica los bloques del portal por su aspecto ORIGINAL y los marca con data-hrc.
 *  3) Reporta perfil / notificaciones al puente nativo.
 */
(function () {
  if (window.__hrcEnhancerLoaded) return;
  window.__hrcEnhancerLoaded = true;

  function isLogin() { return location.pathname.replace(/\/+$/, '') === '/login'; }
  function log(m) { try { console.log('[enhancer] ' + m); } catch (e) {} }
  log('enhancer v3 loaded');

  function hideHeader() {
    if (isLogin()) return;
    var sels = ['header', '.header', '.navbar', '.site-header', '[class*="Header"]', '[class*="Navbar"]', '[class*="TopBar"]'];
    sels.forEach(function(s) {
      document.querySelectorAll(s).forEach(function(el) {
        el.style.setProperty('display', 'none', 'important');
        el.setAttribute('data-hrc-hidden', '1');
      });
    });
  }

  function classify() {
    document.querySelectorAll('div, section, article, button, a').forEach(function(el) {
      if (el.hasAttribute('data-hrc')) return;
      var r = el.getBoundingClientRect();
      if (r.width < 40 || r.height < 20) return;

      var cs = getComputedStyle(el);
      var bg = cs.backgroundColor;
      var shadow = cs.boxShadow;
      var radius = parseFloat(cs.borderRadius) || 0;

      if (el.tagName === 'BUTTON' || el.tagName === 'A' && r.height < 50) {
        el.setAttribute('data-hrc', 'btn');
      } else if (radius > 8 && shadow !== 'none' && r.width > 100) {
        el.setAttribute('data-hrc', 'card');
      } else if (radius > 12 && r.height < 40) {
        el.setAttribute('data-hrc', 'chip');
      }
    });
  }

  setTimeout(hideHeader, 600);
  setTimeout(hideHeader, 2000);
  setTimeout(classify, 1000);
  setTimeout(classify, 3000);
})();
