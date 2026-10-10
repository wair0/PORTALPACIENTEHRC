(function () {
  'use strict';
  if (window.__hrcUiMapInstalled) return;
  window.__hrcUiMapInstalled = true;

  var lastSignature = '';
  var sentRoutes = Object.create(null);
  var timer = 0;

  function cleanUrl(value) {
    try {
      var u = new URL(value, location.href);
      return u.origin + u.pathname;
    } catch (_) { return ''; }
  }
  function short(value, max) {
    value = String(value || '').replace(/\\s+/g, ' ').trim();
    return value.length > max ? value.slice(0, max) : value;
  }
  function visible(el) {
    var r = el.getBoundingClientRect();
    var s = getComputedStyle(el);
    return r.width > 0 && r.height > 0 && s.display !== 'none' &&
      s.visibility !== 'hidden' && Number(s.opacity) !== 0;
  }
  function styleOf(el) {
    var s = getComputedStyle(el);
    var r = el.getBoundingClientRect();
    return {
      display: s.display, position: s.position, boxSizing: s.boxSizing,
      width: Math.round(r.width), height: Math.round(r.height),
      x: Math.round(r.x), y: Math.round(r.y),
      color: s.color, backgroundColor: s.backgroundColor,
      backgroundImage: s.backgroundImage === 'none' ? '' : short(s.backgroundImage, 160),
      fontFamily: s.fontFamily, fontSize: s.fontSize, fontWeight: s.fontWeight,
      lineHeight: s.lineHeight, letterSpacing: s.letterSpacing, textAlign: s.textAlign,
      border: [s.borderTopWidth, s.borderTopStyle, s.borderTopColor].join(' '),
      borderRadius: s.borderRadius, boxShadow: s.boxShadow === 'none' ? '' : short(s.boxShadow, 120),
      padding: s.padding, margin: s.margin, gap: s.gap, flex: s.flex,
      gridTemplateColumns: s.gridTemplateColumns === 'none' ? '' : s.gridTemplateColumns,
      zIndex: s.zIndex, overflow: s.overflow
    };
  }
  function semanticText(el) {
    var tag = el.tagName;
    if (!/^(H1|H2|H3|H4|H5|H6|BUTTON|LABEL|SUMMARY|OPTION)$/.test(tag) &&
        !el.matches('a[role="button"],[role="button"],[role="menuitem"],nav a,[role="tab"]')) return '';
    // Evitar capturar texto dinámico en enlaces/tarjetas con datos personales.
    return short(el.innerText || el.getAttribute('aria-label') || el.getAttribute('title') || '', 100);
  }
  function frameworkHints() {
    var scripts = Array.from(document.scripts).map(function (s) { return cleanUrl(s.src); }).filter(Boolean);
    var styles = Array.from(document.querySelectorAll('link[rel~="stylesheet"]')).map(function (s) { return cleanUrl(s.href); }).filter(Boolean);
    var signatures = [];
    if (window.React || document.querySelector('[data-reactroot],#root')) signatures.push('React (indicadores DOM/runtime)');
    if (window.__NEXT_DATA__ || document.querySelector('script#__NEXT_DATA__')) signatures.push('Next.js');
    if (window.Vue || document.querySelector('[data-v-app],#__vue')) signatures.push('Vue');
    if (window.angular || document.querySelector('[ng-version]')) signatures.push('Angular');
    if (window.jQuery || window.$ && window.$.fn && window.$.fn.jquery) signatures.push('jQuery');
    if (window.bootstrap) signatures.push('Bootstrap JS');
    if (styles.some(function (s) { return /bootstrap/i.test(s); })) signatures.push('Bootstrap CSS (nombre de recurso)');
    if (styles.some(function (s) { return /tailwind/i.test(s); }) || document.querySelector('[class*="tw-"]')) signatures.push('posible Tailwind CSS');
    return { detected: signatures, scriptResources: scripts.slice(0, 100), stylesheetResources: styles.slice(0, 100) };
  }
  function buildMap(reason) {
    if (!document.body || !window.HRCBridge || !window.HRCBridge.saveUiMap) return;
    var path = location.pathname;
    var routeKey = path + '|' + (document.title || '');
    var nodes = Array.from(document.body.querySelectorAll('*')).filter(visible);
    // Limitamos el tamaño y registramos primero los controles y elementos con geometría útil.
    nodes.sort(function (a, b) {
      function priority(el) {
        var t = el.tagName;
        return (/^(HEADER|NAV|MAIN|FOOTER|BUTTON|INPUT|SELECT|TEXTAREA|FORM|H1|H2|H3|LABEL|TABLE|DIALOG)$/.test(t) ||
          el.getAttribute('role') || el.id || el.className) ? 0 : 1;
      }
      return priority(a) - priority(b);
    });
    nodes = nodes.slice(0, 450);
    var elements = nodes.map(function (el) {
      var attrs = {};
      ['role', 'type', 'name', 'placeholder', 'aria-label', 'title', 'for', 'data-testid'].forEach(function (key) {
        var v = el.getAttribute(key);
        if (v) attrs[key] = short(v, 100);
      });
      // Deliberadamente no se registran input.value, texto libre, párrafos, ni contenido de tarjetas.
      var text = semanticText(el);
      return {
        tag: el.tagName.toLowerCase(), id: short(el.id, 100),
        className: short(typeof el.className === 'string' ? el.className : '', 180),
        attrs: attrs, label: text, style: styleOf(el)
      };
    });
    var rootStyle = styleOf(document.documentElement);
    var bodyStyle = styleOf(document.body);
    var vars = {};
    var rootComputed = getComputedStyle(document.documentElement);
    for (var i = 0; i < rootComputed.length; i++) {
      var prop = rootComputed[i];
      if (prop.indexOf('--') === 0) vars[prop] = short(rootComputed.getPropertyValue(prop), 100);
    }
    var report = {
      kind: 'portal-ui-map', schema: 1, capturedAt: new Date().toISOString(),
      reason: reason, url: cleanUrl(location.href), title: short(document.title, 120),
      readyState: document.readyState, viewport: { width: innerWidth, height: innerHeight, dpr: devicePixelRatio },
      document: { htmlClass: short(document.documentElement.className, 180), bodyClass: short(document.body.className, 180),
        rootStyle: rootStyle, bodyStyle: bodyStyle, cssVariables: vars },
      frontend: frameworkHints(), visibleElementCount: nodes.length, elements: elements,
      privacy: 'No se guardaron valores de campos, texto de párrafos ni contenido clínico de tarjetas.'
    };
    var signature = path + '|' + document.title + '|' + nodes.length + '|' +
      Array.from(document.querySelectorAll('h1,h2,h3,button,[role="tab"],nav a')).map(function (e) {
        return e.tagName + ':' + short(e.innerText, 24);
      }).join('|');
    if (sentRoutes[routeKey] === signature) return;
    sentRoutes[routeKey] = signature;
    try { window.HRCBridge.saveUiMap(JSON.stringify(report)); } catch (_) {}
  }
  function schedule(reason) {
    if (timer) clearTimeout(timer);
    timer = setTimeout(function () { buildMap(reason || 'DOM estable'); }, 1800);
  }
  function start() {
    schedule('carga inicial');
    var observer = new MutationObserver(function () { schedule('cambio DOM / SPA'); });
    observer.observe(document.documentElement, { childList: true, subtree: true });
    var lastPath = location.pathname;
    setInterval(function () {
      if (location.pathname !== lastPath) {
        lastPath = location.pathname;
        schedule('cambio de ruta SPA');
      }
    }, 1000);
    setTimeout(function () { buildMap('segunda captura estable'); }, 6500);
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start, { once: true });
  else start();
})();