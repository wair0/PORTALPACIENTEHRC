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

  // 1) window.chrome
  if (!window.chrome) {
    try {
      Object.defineProperty(window, 'chrome', {
        value: {
          runtime: {},
          loadTimes: function () { return {}; },
          csi: function () { return {}; },
          app: { isInstalled: false }
        },
        configurable: true,
        writable: true
      });
    } catch (e) {}
  }

  // 2) navigator.userAgentData
  var brands = [
    { brand: 'Chromium', version: MAJOR },
    { brand: 'Google Chrome', version: MAJOR },
    { brand: 'Not_A Brand', version: '24' }
  ];
  var fullBrands = brands.map(function (b) {
    return { brand: b.brand, version: b.brand === 'Not_A Brand' ? '24.0.0.0' : FULL };
  });
  var uaData = {
    brands: brands,
    mobile: true,
    platform: 'Android',
    getHighEntropyValues: function () {
      return Promise.resolve({
        brands: brands, mobile: true, platform: 'Android', platformVersion: '10.0.0',
        architecture: '', bitness: '', model: 'K', uaFullVersion: FULL,
        fullVersionList: fullBrands, wow64: false
      });
    },
    toJSON: function () { return { brands: brands, mobile: true, platform: 'Android' }; }
  };
  define(navigator, 'userAgentData', function () { return uaData; });

  // 3) Propiedades típicas
  define(navigator, 'webdriver', function () { return false; });
  define(navigator, 'vendor', function () { return 'Google Inc.'; });
  define(navigator, 'platform', function () { return 'Linux armv81'; });
  define(navigator, 'hardwareConcurrency', function () { return 8; });
  define(navigator, 'deviceMemory', function () { return 8; });
  if (!navigator.languages || !navigator.languages.length) {
    define(navigator, 'languages', function () { return ['es-AR', 'es', 'en']; });
  }

  // 4) Ocultar señales de WebView
  try {
    Object.defineProperty(window, 'outerHeight', { get: function () { return window.innerHeight; }, configurable: true });
    Object.defineProperty(window, 'outerWidth', { get: function () { return window.innerWidth; }, configurable: true });
  } catch (e) {}

  // 5) Forzar que no se detecte como app
  try {
    delete window.HRCBridge;
  } catch (e) {}

  if (DIAG) {
    try {
      console.log('[ua-compat] ua=' + navigator.userAgent);
      console.log('[ua-compat] chrome=' + (typeof window.chrome) +
        ' uaData=' + JSON.stringify(navigator.userAgentData) +
        ' webdriver=' + navigator.webdriver);
    } catch (e) {}
  }
})();
