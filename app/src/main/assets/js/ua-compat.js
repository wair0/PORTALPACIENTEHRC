/* Se ejecuta en document-start, solo en https://tws.sanluis.gob.ar.
   Hace que el WebView se comporte como Chrome móvil ante scripts de detección. */
(function () {
  'use strict';
  if (window.__hrcUaCompat) return;
  window.__hrcUaCompat = true;

  var MAJOR = '__CHROME_MAJOR__';
  var FULL = MAJOR + '.0.0.0';
  var DIAG = '__DIAGNOSTICS__' === 'true';

  function define(obj, prop, getter) {
    try { Object.defineProperty(obj, prop, { get: getter, configurable: true }); } catch (e) {}
  }

  // 1) window.chrome: existe en Chrome, falta en WebView.
  if (!window.chrome) {
    try {
      Object.defineProperty(window, 'chrome', {
        value: { runtime: {}, loadTimes: function () { return {}; }, csi: function () { return {}; }, app: { isInstalled: false } },
        configurable: true, writable: true
      });
    } catch (e) {}
  }

  // 2) navigator.userAgentData coherente con el UA / Client Hints.
  var brands = [
    { brand: 'Chromium', version: MAJOR },
    { brand: 'Google Chrome', version: MAJOR },
    { brand: 'Not_A Brand', version: '24' }
  ];
  var fullBrands = brands.map(function (b) {
    return { brand: b.brand, version: b.brand === 'Not_A Brand' ? '24.0.0.0' : FULL };
  });
  var uaData = {
    brands: brands, mobile: true, platform: 'Android',
    getHighEntropyValues: function (hints) {
      return Promise.resolve({
        brands: brands, mobile: true, platform: 'Android', platformVersion: '10.0.0',
        architecture: '', bitness: '', model: 'K', uaFullVersion: FULL,
        fullVersionList: fullBrands, wow64: false
      });
    },
    toJSON: function () { return { brands: brands, mobile: true, platform: 'Android' }; }
  };
  define(navigator, 'userAgentData', function () { return uaData; });

  // 3) Propiedades típicas de Chrome móvil real.
  define(navigator, 'webdriver', function () { return false; });
  define(navigator, 'vendor', function () { return 'Google Inc.'; });
  define(navigator, 'platform', function () { return 'Linux armv81'; });
  if (!navigator.languages || !navigator.languages.length) {
    define(navigator, 'languages', function () { return ['es-AR', 'es', 'en']; });
  }

  // 4) Diagnóstico: qué ve el portal. Ver en logcat (tag PortalWebView) o chrome://inspect.
  if (DIAG) {
    try {
      console.log('[ua-compat] ua=' + navigator.userAgent);
      console.log('[ua-compat] chrome=' + (typeof window.chrome) +
        ' uaData=' + JSON.stringify(navigator.userAgentData) +
        ' webdriver=' + navigator.webdriver +
        ' bridge=' + (typeof window.HRCBridge));
    } catch (e) {}
  }
})();
