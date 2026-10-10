/* Se ejecuta en document-start. Oculta más señales de WebView. */
(function () {
  'use strict';
  if (window.__hrcUaCompat) return;
  window.__hrcUaCompat = true;

  var MAJOR = '__CHROME_MAJOR__';
  var FULL = MAJOR + '.0.0.0';

  function define(obj, prop, getter) {
    try { Object.defineProperty(obj, prop, { get: getter, configurable: true }); } catch (e) {}
  }

  // window.chrome
  if (!window.chrome) {
    try {
      Object.defineProperty(window, 'chrome', {
        value: { runtime: {}, loadTimes: function(){return{};}, csi: function(){return{};}, app: {isInstalled:false} },
        configurable: true, writable: true
      });
    } catch(e) {}
  }

  // navigator.userAgentData
  var brands = [
    {brand:'Chromium',version:MAJOR},
    {brand:'Google Chrome',version:MAJOR},
    {brand:'Not_A Brand',version:'24'}
  ];
  var uaData = {
    brands: brands, mobile: true, platform: 'Android',
    getHighEntropyValues: function() {
      return Promise.resolve({
        brands: brands, mobile: true, platform: 'Android', platformVersion: '10.0.0',
        architecture: '', bitness: '', model: 'K', uaFullVersion: FULL, wow64: false
      });
    }
  };
  define(navigator, 'userAgentData', function(){ return uaData; });

  // Propiedades
  define(navigator, 'webdriver', function(){ return false; });
  define(navigator, 'vendor', function(){ return 'Google Inc.'; });
  define(navigator, 'platform', function(){ return 'Linux armv81'; });
  define(navigator, 'hardwareConcurrency', function(){ return 8; });
  define(navigator, 'deviceMemory', function(){ return 8; });
  define(navigator, 'maxTouchPoints', function(){ return 5; });

  // Ocultar señales de WebView / app
  try {
    Object.defineProperty(window, 'outerHeight', { get: function(){ return window.innerHeight; }, configurable: true });
    Object.defineProperty(window, 'outerWidth', { get: function(){ return window.innerWidth; }, configurable: true });
  } catch(e) {}

  // Eliminar bridges conocidos
  try { delete window.HRCBridge; } catch(e) {}
  try { delete window.Android; } catch(e) {}

  console.log('[ua-compat] loaded');
})();
