/**
 * Portal Paciente HRC - Enhancer
 * Inyectado junto con el CSS. Idempotente.
 * Reporta datos de perfil al bridge nativo cuando están disponibles.
 */
(function () {
  if (window.__hrcEnhancerLoaded) return;
  window.__hrcEnhancerLoaded = true;

  function tryReportProfile() {
    try {
      // Intentar extraer nombre del DOM (ajustar selectores según el portal real)
      var nameEl = document.querySelector('.user-name, .profile-name, .patient-name, h1, .welcome');
      var name = nameEl ? nameEl.textContent.trim() : '';

      // Foto si existe (data URL o src)
      var photoEl = document.querySelector('.avatar, .profile-photo, img.user-photo');
      var photo = '';
      if (photoEl && photoEl.src && photoEl.src.startsWith('data:image')) {
        photo = photoEl.src;
      }

      if (name && window.HRCBridge && window.HRCBridge.onProfile) {
        window.HRCBridge.onProfile(JSON.stringify({
          name: name,
          photo: photo
        }));
      }

      // Unread placeholder (ajustar si el portal muestra badge)
      var badge = document.querySelector('.badge, .notification-count, .unread');
      var count = badge ? parseInt(badge.textContent, 10) || 0 : 0;
      if (window.HRCBridge && window.HRCBridge.onUnread) {
        window.HRCBridge.onUnread(count);
      }
    } catch (e) {
      // silencioso
    }
  }

  // Intentar al cargar y después de posibles actualizaciones AJAX
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', tryReportProfile);
  } else {
    tryReportProfile();
  }

  // Observar cambios de DOM para SPAs o cargas parciales
  var observer = new MutationObserver(function () {
    tryReportProfile();
  });
  observer.observe(document.body || document.documentElement, {
    childList: true,
    subtree: true
  });

  // Reintentar después de un tiempo
  setTimeout(tryReportProfile, 1500);
  setTimeout(tryReportProfile, 4000);
})();
