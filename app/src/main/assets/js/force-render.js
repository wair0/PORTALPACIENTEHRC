/* Se ejecuta después de que la página cargue para intentar forzar el render. */
(function () {
  'use strict';
  if (window.__hrcForceRender) return;
  window.__hrcForceRender = true;

  function force() {
    try {
      // Forzar reflow
      document.body.style.display = 'none';
      document.body.offsetHeight;
      document.body.style.display = '';

      // Disparar resize
      window.dispatchEvent(new Event('resize'));

      // Si hay un root de React, intentar forzar update
      var root = document.getElementById('root') || document.getElementById('__next');
      if (root) {
        root.style.opacity = '0.99';
        setTimeout(function () { root.style.opacity = ''; }, 50);
      }

      console.log('[force-render] ejecutado');
    } catch (e) {
      console.log('[force-render] error: ' + e);
    }
  }

  if (document.readyState === 'complete') {
    setTimeout(force, 300);
  } else {
    window.addEventListener('load', function () {
      setTimeout(force, 300);
    });
  }
})();
