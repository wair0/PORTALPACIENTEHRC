(function () {
  'use strict';
  if (window.__hrcUiMapInstalled) return;
  window.__hrcUiMapInstalled = true;
  var sentRoutes = Object.create(null), timer = 0;

  function cleanUrl(value) {
    try { var u = new URL(value, location.href); return u.origin + u.pathname; } catch (_) { return ''; }
  }
  function short(value, max) {
    value = String(value || '').replace(/\s+/g, ' ').trim();
    return value.length > max ? value.slice(0, max) : value;
  }
  function visible(el) {
    var r = el.getBoundingClientRect(), s = getComputedStyle(el);
    return r.width > 0 && r.height > 0 && s.display !== 'none' && s.visibility !== 'hidden' && Number(s.opacity) !== 0;
  }
  function styleOf(el) {
    var s = getComputedStyle(el), r = el.getBoundingClientRect();
    return {
      display:s.display, position:s.position, boxSizing:s.boxSizing,
      width:Math.round(r.width), height:Math.round(r.height), x:Math.round(r.x), y:Math.round(r.y),
      color:s.color, backgroundColor:s.backgroundColor,
      backgroundImage:s.backgroundImage === 'none' ? '' : short(s.backgroundImage,160),
      fontFamily:s.fontFamily, fontSize:s.fontSize, fontWeight:s.fontWeight,
      lineHeight:s.lineHeight, letterSpacing:s.letterSpacing, textAlign:s.textAlign,
      border:[s.borderTopWidth,s.borderTopStyle,s.borderTopColor].join(' '),
      borderRadius:s.borderRadius, boxShadow:s.boxShadow === 'none' ? '' : short(s.boxShadow,120),
      padding:s.padding, margin:s.margin, gap:s.gap, flex:s.flex,
      gridTemplateColumns:s.gridTemplateColumns === 'none' ? '' : s.gridTemplateColumns,
      zIndex:s.zIndex, overflow:s.overflow
    };
  }
  function semanticTextLength(el) {
    if (!/^(H1|H2|H3|H4|H5|H6|BUTTON|LABEL|SUMMARY|OPTION)$/.test(el.tagName) &&
        !el.matches('[role="button"],[role="menuitem"],[role="tab"],nav a')) return 0;
    return short(el.innerText || '',300).length;
  }
  function frameworkHints() {
    var scripts=Array.from(document.scripts).map(function(s){return cleanUrl(s.src);}).filter(Boolean);
    var styles=Array.from(document.querySelectorAll('link[rel~="stylesheet"]')).map(function(s){return cleanUrl(s.href);}).filter(Boolean);
    var detected=[];
    if(window.React||document.querySelector('[data-reactroot],#root')) detected.push('React (indicadores DOM/runtime)');
    if(window.__NEXT_DATA__||document.querySelector('script#__NEXT_DATA__')) detected.push('Next.js');
    if(window.Vue||document.querySelector('[data-v-app],#__vue')) detected.push('Vue');
    if(window.angular||document.querySelector('[ng-version]')) detected.push('Angular');
    if(window.jQuery||window.$&&window.$.fn&&window.$.fn.jquery) detected.push('jQuery');
    if(window.bootstrap) detected.push('Bootstrap JS');
    if(styles.some(function(s){return /bootstrap/i.test(s);})) detected.push('Bootstrap CSS (nombre de recurso)');
    if(styles.some(function(s){return /tailwind/i.test(s);})||document.querySelector('[class*="tw-"]')) detected.push('posible Tailwind CSS');
    return {detected:detected,scriptResources:scripts.slice(0,100),stylesheetResources:styles.slice(0,100)};
  }

  function sendMap(payload) {
    // Primero intentar el puente normal
    if (window.HRCBridge && typeof window.HRCBridge.saveUiMap === 'function') {
      try {
        window.HRCBridge.saveUiMap(payload);
        console.log('[UI_MAP] Enviado por puente HRCBridge');
        return true;
      } catch (e) {
        console.warn('[UI_MAP] Puente falló, usando prompt: ' + e);
      }
    }
    // Fallback confiable: prompt
    try {
      window.prompt('HRC_UI_MAP:' + payload);
      console.log('[UI_MAP] Enviado por prompt');
      return true;
    } catch (e) {
      console.error('[UI_MAP] Prompt también falló: ' + e);
      return false;
    }
  }

  function buildMap(reason) {
    if(!document.body){console.warn('[UI_MAP] No hay document.body');return;}
    console.log('[UI_MAP] Iniciando captura; motivo='+reason);
    var path=location.pathname, routeKey=path+'|'+(document.title||'');
    var nodes=Array.from(document.body.querySelectorAll('*')).filter(visible);
    nodes.sort(function(a,b){
      function priority(el){return (/^(HEADER|NAV|MAIN|FOOTER|BUTTON|INPUT|SELECT|TEXTAREA|FORM|H1|H2|H3|LABEL|TABLE|DIALOG)$/.test(el.tagName)||el.getAttribute('role')||el.id||el.className)?0:1;}
      return priority(a)-priority(b);
    });
    nodes=nodes.slice(0,450);
    var elements=nodes.map(function(el){
      var attrs={};
      ['role','type','name','placeholder','for','data-testid','aria-expanded','aria-hidden'].forEach(function(key){
        var v=el.getAttribute(key); if(v)attrs[key]=short(v,100);
      });
      return {tag:el.tagName.toLowerCase(),id:short(el.id,100),
        className:short(typeof el.className==='string'?el.className:'',180),
        attrs:attrs,semanticTextLength:semanticTextLength(el),style:styleOf(el)};
    });
    var rootComputed=getComputedStyle(document.documentElement),vars={};
    for(var i=0;i<rootComputed.length;i++){var prop=rootComputed[i];if(prop.indexOf('--')===0)vars[prop]=short(rootComputed.getPropertyValue(prop),100);}
    var report={
      kind:'portal-ui-map',schema:3,capturedAt:new Date().toISOString(),reason:reason,
      url:cleanUrl(location.href),title:short(document.title,120),readyState:document.readyState,
      viewport:{width:innerWidth,height:innerHeight,dpr:devicePixelRatio},
      document:{htmlClass:short(document.documentElement.className,180),bodyClass:short(document.body.className,180),
        rootStyle:styleOf(document.documentElement),bodyStyle:styleOf(document.body),cssVariables:vars},
      frontend:frameworkHints(),visibleElementCount:nodes.length,elements:elements,
      privacy:'No se guardaron valores de campos ni texto clínico/personal.'
    };
    var signature=path+'|'+document.title+'|'+nodes.length+'|'+nodes.slice(0,180).map(function(e){
      return e.tagName+':'+short(e.id,30)+':'+short(typeof e.className==='string'?e.className:'',60);
    }).join('|');
    if(sentRoutes[routeKey]===signature)return;
    sentRoutes[routeKey]=signature;
    var payload=JSON.stringify(report);
    console.log('[UI_MAP] Preparado; elementos='+nodes.length+', chars='+payload.length);
    sendMap(payload);
  }

  function schedule(reason){if(timer)clearTimeout(timer);timer=setTimeout(function(){buildMap(reason||'DOM estable');},1800);}
  function start(){
    console.log('[UI_MAP] Script activo en '+cleanUrl(location.href));
    schedule('carga inicial');
    var observer=new MutationObserver(function(){schedule('cambio DOM/estilo/menú SPA');});
    observer.observe(document.documentElement,{childList:true,subtree:true,attributes:true,
      attributeFilter:['class','style','hidden','aria-expanded','aria-hidden','role']});
    var lastPath=location.pathname;
    setInterval(function(){if(location.pathname!==lastPath){lastPath=location.pathname;schedule('cambio de ruta SPA');}},1000);
    setTimeout(function(){buildMap('segunda captura estable');},6500);
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',start,{once:true});else start();
})();