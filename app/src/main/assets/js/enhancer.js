/**
 * Portal Paciente HRC - Enhancer v2
 *  1) Oculta el header oficial del portal (la app ya tiene su propia barra nativa) en todas las
 *     pantallas menos el login. Lo detecta por posición, no por clase.
 *  2) Reporta perfil / notificaciones al puente nativo.
 * Idempotente. Para desactivarlo: localStorage.hrc_keep_header = '1'
 */
(function () {
  if (window.__hrcEnhancerLoaded) return;
  window.__hrcEnhancerLoaded = true;

  function isLogin() { return location.pathname.replace(/\/+$/, '') === '/login'; }
  function log(m) { try { console.log('[enhancer] ' + m); } catch (e) {} }
  function keep() { try { return localStorage.getItem('hrc_keep_header') === '1'; } catch (e) { return false; } }

  function setHidden(el, hidden) {
    if (hidden) el.setAttribute('data-hrc-hidden', '1'); else el.removeAttribute('data-hrc-hidden');
  }

  function hideOfficialHeader() {
    var hidden = document.querySelectorAll('[data-hrc-hidden="1"]');
    if (isLogin() || keep()) {
      hidden.forEach(function (e) { setHidden(e, false); });
      return;
    }
    if (hidden.length) return;

    var w = window.innerWidth;
    var el = document.elementFromPoint(w / 2, 6);
    if (!el || el === document.documentElement || el === document.body || el.id === 'root') return;

    var cur = el;
    while (cur.parentElement && cur.parentElement !== document.body && cur.parentElement.id !== 'root') {
      var r = cur.getBoundingClientRect();
      if (r.width > w * 0.7 && r.height > 30 && r.height < 120 && r.top < 20) {
        setHidden(cur, true);
        log('header oculto');
        return;
      }
      cur = cur.parentElement;
    }
  }

  function tryReportProfile() {
    try {
      if (document.body && document.body.innerText && document.body.innerText.indexOf('tws.loader.loading') !== -1) return;

      var nameEl = document.querySelector('.user-name, .profile-name, .patient-name, .welcome');
      var name = nameEl ? nameEl.textContent.trim() : '';

      var photoEl = document.querySelector('.avatar, .profile-photo');
      var photo = (photoEl && photoEl.src && photoEl.src.indexOf('data:image') === 0) ? photoEl.src : '';

      if (name && window.HRCBridge && window.HRCBridge.onProfile) {
        window.HRCBridge.onProfile(JSON.stringify({ name: name, photo: photo }));
      }

      var badge = document.querySelector('.badge, .notification-count');
      var count = badge ? (parseInt(badge.textContent, 10) || 0) : 0;
      if (window.HRCBridge && window.HRCBridge.onUnread) {
        window.HRCBridge.onUnread(count);
      }
    } catch (e) {}
  }

  setTimeout(hideOfficialHeader, 800);
  setTimeout(hideOfficialHeader, 2000);
  setTimeout(tryReportProfile, 2000);
  setTimeout(tryReportProfile, 5000);

  log('enhancer v2 loaded');
})();
