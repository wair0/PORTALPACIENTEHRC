/* HRC enhancer v4: marca superficies por su aspecto original y mantiene la SPA observada. */
(function () {
  'use strict';
  if (window.__hrcEnhancerLoaded) return;
  window.__hrcEnhancerLoaded = true;

  var lastPath = '', scheduled = false, headerPasses = 0;
  var SKIP = /^(SCRIPT|STYLE|LINK|META|NOSCRIPT|SVG|PATH|IMG|CANVAS|VIDEO|INPUT|SELECT|TEXTAREA|OPTION|BR|HR|DEFS|G|USE)$/i;
  var BAD = /loader|overlay|backdrop|modal|dialog|spinner|skeleton/i;

  function log(msg) { try { console.log('[enhancer] ' + msg); } catch (_) {} }
  function isLogin() { return location.pathname.replace(/\/+$/, '') === '/login'; }
  function keepHeader() { try { return localStorage.getItem('hrc_keep_header') === '1'; } catch (_) { return false; } }
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
  function hideHeader() {
    if (isLogin() || keepHeader()) {
      document.querySelectorAll('[data-hrc-hidden="1"]').forEach(function (e) { e.removeAttribute('data-hrc-hidden'); });
      return;
    }
    var width = window.innerWidth, found = [];
    function candidate(el) {
      if (!el || el === document.body || el === document.documentElement || el.id === 'root') return;
      if (BAD.test(el.getAttribute('class') || '') || el.getAttribute('role') === 'dialog') return;
      var r = el.getBoundingClientRect();
      if (r.top <= 3 && r.height >= 36 && r.height <= 170 && r.width >= width * .9 &&
          getComputedStyle(el).display !== 'none') found.push(el);
    }
    document.querySelectorAll('header,[role="banner"],[class*="header" i],[class*="navbar" i],[class*="app-bar" i],[class*="appbar" i],[class*="topbar" i],[class*="top-bar" i],[class*="toolbar" i]').forEach(candidate);
    if (!found.length && document.elementsFromPoint) {
      [6, 24].forEach(function (y) {
        document.elementsFromPoint(width / 2, y).forEach(function (el) {
          var cur = el;
          while (cur && cur.parentElement && cur.parentElement !== document.body && cur.parentElement.id !== 'root') {
            var pr = cur.parentElement.getBoundingClientRect();
            if (pr.height > 170 || pr.width < width * .9) break;
            cur = cur.parentElement;
          }
          candidate(cur);
        });
      });
    }
    found = found.filter(function (a) { return !found.some(function (b) { return a !== b && b.contains(a); }); });
    found.forEach(function (el) {
      if (el.getAttribute('data-hrc-hidden') === '1') return;
      var h = el.getBoundingClientRect().height;
      el.setAttribute('data-hrc-hidden', '1');
      [document.body, document.getElementById('root'), document.getElementById('root') && document.getElementById('root').firstElementChild].forEach(function (node) {
        if (!node) return;
        var cs = getComputedStyle(node);
        ['paddingTop', 'marginTop'].forEach(function (prop) {
          var value = parseFloat(cs[prop]);
          if (value >= h - 6 && value <= h + 6) node.style.setProperty(prop === 'paddingTop' ? 'padding-top' : 'margin-top', '0', 'important');
        });
      });
      log('header oculto: ' + el.tagName.toLowerCase() + '.' + String(el.className || '').toString().split(/\s+/).slice(0, 3).join('.') + ' (' + Math.round(h) + 'px)');
    });
  }

  function classify() {
    if (!document.body) return;
    var list = Array.prototype.slice.call(document.body.querySelectorAll('*:not([data-hrc-seen])'), 0, 1800);
    if (!list.length) return;
    var kinds = [], theme = window.__hrcTheme;
    try { if (theme) theme(false); } catch (_) {}
    list.forEach(function (el) {
      var kind = null;
      try {
        if (!SKIP.test(el.tagName) && !el.closest('svg')) {
          var cs = getComputedStyle(el), r = el.getBoundingClientRect();
          var bgAlpha = alpha(cs.backgroundColor);
          var hasBg = bgAlpha > .25 || cs.backgroundImage !== 'none';
          var hasShadow = cs.boxShadow !== 'none';
          var hasBorder = cs.borderTopStyle !== 'none' && parseFloat(cs.borderTopWidth) > 0 && alpha(cs.borderTopColor) > .2;
          var isButton = el.tagName === 'BUTTON' || el.getAttribute('role') === 'button' ||
            (el.tagName === 'A' && hasBg && r.height <= 64);
          if (r.width <= 12 || r.height <= 12) { if (hasBg) kind = 'keep'; }
          else if (isButton) kind = (bgAlpha > .5 && saturation(cs.backgroundColor) > 40) ? 'btn-primary' : 'btn';
          else if (hasBg || hasShadow || hasBorder) {
            if (r.width >= innerWidth * .96 && r.height >= innerHeight * .5) kind = 'wrap';
            else if (hasBg && r.height <= 40 && r.width <= 180) kind = 'chip';
            else if (r.width >= 120 && r.height >= 40) kind = 'card';
          }
        }
      } catch (_) {}
      kinds.push(kind);
    });
    try { if (theme) theme(true); } catch (_) {}
    list.forEach(function (el, i) {
      el.setAttribute('data-hrc-seen', '1');
      var kind = kinds[i];
      if (kind === 'card' && el.parentElement && el.parentElement.closest('[data-hrc="card"]')) kind = 'inner';
      if (kind) el.setAttribute('data-hrc', kind);
    });
  }

  function run() {
    scheduled = false;
    try {
      if (location.pathname !== lastPath) { lastPath = location.pathname; headerPasses = 0; }
      document.documentElement.setAttribute('data-hrc-route', isLogin() ? 'login' : 'app');
      if (headerPasses++ < 80) hideHeader();
      classify();
    } catch (e) { log('error: ' + e); }
  }
  function schedule() {
    if (scheduled) return;
    scheduled = true;
    setTimeout(run, 120);
  }
  function start() {
    run();
    new MutationObserver(schedule).observe(document.documentElement, { childList: true, subtree: true, attributes: true, attributeFilter: ['class', 'style', 'role'] });
    setInterval(run, 1800);
  }
  log('enhancer loaded');
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start, { once: true }); else start();
})();