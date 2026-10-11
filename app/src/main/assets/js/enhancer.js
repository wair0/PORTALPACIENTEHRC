/* HRC enhancer v6: marca elementos pero no oculta header ni contenido. */
(function () {
  'use strict';
  if (window.__hrcEnhancerLoaded) return;
  window.__hrcEnhancerLoaded = true;

  var lastPath = '', scheduled = false;
  var SKIP = /^(SCRIPT|STYLE|LINK|META|NOSCRIPT|SVG|PATH|IMG|CANVAS|VIDEO|INPUT|SELECT|TEXTAREA|OPTION|BR|HR|DEFS|G|USE)$/i;

  function log(msg) { try { console.log('[enhancer] ' + msg); } catch (_) {} }
  function isLogin() { return location.pathname.replace(/\/+$/, '') === '/login'; }
  function alpha(color) {
    var m = String(color || '').match(/rgba?\(([^)]+)\)/i);
    if (!m) return 0;
    var parts = m[1].split(/[ ,/]+/).filter(Boolean);
    return parts.length > 3 ? (parseFloat(parts[3]) || 0) : 1;
  }
  function saturation(color) {
    var m = String(color || '').match(/rgba?\(([^)]+)\)/i);
    if (!m) return 0;
    var p = m[1].split(/[ ,/]+/).filter(Boolean).slice(0, 3).map(Number);
    return p.length === 3 ? Math.max.apply(null, p) - Math.min.apply(null, p) : 0;
  }

  function classify() {
    if (!document.body) return;
    var list = Array.prototype.slice.call(document.body.querySelectorAll('*:not([data-hrc-seen])'), 0, 1200);
    if (!list.length) return;
    var kinds = [];
    list.forEach(function (el) {
      var kind = null;
      try {
        if (!SKIP.test(el.tagName) && !el.closest('svg')) {
          var cs = getComputedStyle(el), r = el.getBoundingClientRect();
          var bgAlpha = alpha(cs.backgroundColor);
          var hasBg = bgAlpha > .3 || cs.backgroundImage !== 'none';
          var hasShadow = cs.boxShadow !== 'none';
          var isButton = el.tagName === 'BUTTON' || el.getAttribute('role') === 'button' ||
            (el.tagName === 'A' && hasBg && r.height <= 64);
          if (r.width <= 8 || r.height <= 8) { /* skip tiny */ }
          else if (isButton) kind = (bgAlpha > .5 && saturation(cs.backgroundColor) > 40) ? 'btn-primary' : 'btn';
          else if (hasBg || hasShadow) {
            if (hasBg && r.height <= 36 && r.width <= 160) kind = 'chip';
            else if (r.width >= 100 && r.height >= 36) kind = 'card';
          }
        }
      } catch (_) {}
      kinds.push(kind);
    });
    list.forEach(function (el, i) {
      el.setAttribute('data-hrc-seen', '1');
      var kind = kinds[i];
      if (kind === 'card' && el.parentElement && el.parentElement.closest('[data-hrc="card"]')) kind = 'inner';
      if (kind) el.setAttribute('data-hrc', kind);
    });
  }

  function styleLogin() {
    if (!isLogin()) return;
    document.documentElement.setAttribute('data-hrc-route', 'login');
    var forms = document.querySelectorAll('form');
    forms.forEach(function (f) {
      if (!f.getAttribute('data-hrc')) f.setAttribute('data-hrc', 'card');
    });
    var buttons = document.querySelectorAll('button[type="submit"]');
    buttons.forEach(function (b) {
      if (!b.getAttribute('data-hrc')) b.setAttribute('data-hrc', 'btn-primary');
    });
  }

  function run() {
    scheduled = false;
    try {
      if (location.pathname !== lastPath) { lastPath = location.pathname; }
      document.documentElement.setAttribute('data-hrc-route', isLogin() ? 'login' : 'app');
      classify();
      styleLogin();
    } catch (e) { log('error: ' + e); }
  }
  function schedule() {
    if (scheduled) return;
    scheduled = true;
    setTimeout(run, 150);
  }
  function start() {
    run();
    new MutationObserver(schedule).observe(document.documentElement, { childList: true, subtree: true, attributes: true, attributeFilter: ['class', 'style'] });
    setInterval(run, 2000);
  }
  log('enhancer v6 loaded');
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start, { once: true }); else start();
})();