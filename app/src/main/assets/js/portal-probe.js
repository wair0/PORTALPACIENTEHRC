/* Diagnóstico (document-start). Todo sale por console.log con prefijo [probe]. */
(function () {
  'use strict';
  if (window.__hrcProbe) return;
  window.__hrcProbe = true;

  var budget = 120;
  function log(m) {
    if (budget-- <= 0) return;
    try { console.log('[probe] ' + m); } catch (e) {}
  }
  function short(u) { u = String(u); return u.length > 140 ? u.slice(0, 140) + '…' : u; }

  log('url=' + location.href + ' online=' + navigator.onLine + ' cookieLen=' + (document.cookie || '').length);

  window.addEventListener('error', function (e) {
    var t = e.target;
    if (t && t !== window && (t.src || t.href)) log('RESOURCE FAIL ' + t.tagName + ' ' + short(t.src || t.href));
    else log('JS ERROR ' + e.message + ' @' + short(e.filename || '') + ':' + e.lineno);
  }, true);
  window.addEventListener('unhandledrejection', function (e) {
    var r = e.reason; log('REJECTION ' + (r && (r.stack || r.message) || r));
  });

  if (window.fetch) {
    var of = window.fetch;
    window.fetch = function (input, init) {
      var url = short(input && input.url || input);
      var method = (init && init.method) || (input && input.method) || 'GET';
      var t0 = Date.now();
      return of.apply(this, arguments).then(function (res) {
        log('FETCH ' + method + ' ' + res.status + ' ' + (Date.now() - t0) + 'ms ' + url);
        return res;
      }, function (err) {
        log('FETCH FAIL ' + method + ' ' + url + ' -> ' + err);
        throw err;
      });
    };
  }

  var xo = XMLHttpRequest.prototype.open, xs = XMLHttpRequest.prototype.send;
  XMLHttpRequest.prototype.open = function (m, u) { this.__m = m; this.__u = short(u); return xo.apply(this, arguments); };
  XMLHttpRequest.prototype.send = function () {
    var x = this, t0 = Date.now();
    x.addEventListener('loadend', function () {
      log('XHR ' + x.__m + ' ' + (x.status || 'FAIL(0)') + ' ' + (Date.now() - t0) + 'ms ' + x.__u);
    });
    return xs.apply(this, arguments);
  };

  function snapshot(tag) {
    try {
      var b = document.body;
      if (!b) { log(tag + ' sin body'); return; }
      var r = b.getBoundingClientRect();
      var cs = getComputedStyle(b);
      var all = b.querySelectorAll('*').length;
      var visible = 0, big = 0;
      b.querySelectorAll('*').forEach(function (el) {
        var q = el.getBoundingClientRect();
        if (q.width > 0 && q.height > 0) visible++;
        if (q.width > innerWidth * 0.5 && q.height > innerHeight * 0.5) big++;
      });
      log(tag + ' readyState=' + document.readyState + ' title="' + document.title + '" body=' + Math.round(r.width) + 'x' + Math.round(r.height) +
        ' bg=' + cs.backgroundColor + ' color=' + cs.color + ' nodes=' + all + ' visible=' + visible + ' bigElems=' + big);
      log(tag + ' text="' + (b.innerText || '').replace(/\s+/g, ' ').trim().slice(0, 200) + '"');
      var loader = b.querySelector('[class*="loader" i],[class*="loading" i],[class*="spinner" i]');
      if (loader) log(tag + ' loader=' + loader.className + ' display=' + getComputedStyle(loader).display + ' vis=' + getComputedStyle(loader).visibility);
      log(tag + ' html=' + b.innerHTML.replace(/\s+/g, ' ').slice(0, 300));
    } catch (e) { log(tag + ' snapshot error ' + e); }
  }
  setTimeout(function () { snapshot('T+4s'); }, 4000);
  setTimeout(function () { snapshot('T+12s'); }, 12000);
})();
