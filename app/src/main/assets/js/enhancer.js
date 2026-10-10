/**
 * Portal Paciente HRC - Enhancer (simplificado)
 * Idempotente. Reporta perfil solo cuando el DOM ya tiene contenido real.
 */
(function () {
  if (window.__hrcEnhancerLoaded) return;
  window.__hrcEnhancerLoaded = true;

  function tryReportProfile() {
    try {
      // Evitar actuar mientras el loader está visible
      if (document.body && document.body.innerText && document.body.innerText.indexOf('tws.loader.loading') !== -1) {
        return;
      }

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

  // Solo reintentar unas pocas veces, sin observer agresivo
  setTimeout(tryReportProfile, 2000);
  setTimeout(tryReportProfile, 5000);
  setTimeout(tryReportProfile, 10000);
})();
