/* Se ejecuta en document-start. El portal falla con:
     Uncaught (in promise) Error: [Text to Speech Service]: Device not supported
   porque el WebView no trae voces de síntesis de voz. Acá se simula un speechSynthesis
   mínimo (sin audio) para que el servicio de texto-a-voz del portal se inicialice igual. */
(function () {
  'use strict';
  if (window.__hrcTtsPolyfill) return;
  window.__hrcTtsPolyfill = true;

  var FAKE_VOICE = { default: true, lang: 'es-AR', localService: true, name: 'Voz del dispositivo', voiceURI: 'hrc-voice' };

  function FakeUtterance(text) {
    this.text = text === undefined ? '' : String(text);
    this.lang = 'es-AR'; this.pitch = 1; this.rate = 1; this.volume = 1; this.voice = null;
    this.onstart = this.onend = this.onerror = this.onpause = this.onresume = this.onboundary = this.onmark = null;
    this._l = {};
  }
  FakeUtterance.prototype.addEventListener = function (t, f) { (this._l[t] = this._l[t] || []).push(f); };
  FakeUtterance.prototype.removeEventListener = function (t, f) {
    var a = this._l[t] || []; var i = a.indexOf(f); if (i >= 0) a.splice(i, 1);
  };
  FakeUtterance.prototype.dispatchEvent = function (e) {
    var h = this['on' + e.type]; if (typeof h === 'function') { try { h.call(this, e); } catch (x) {} }
    (this._l[e.type] || []).slice().forEach(function (f) { try { f.call(this, e); } catch (x) {} }, this);
    return true;
  };

  if (typeof window.SpeechSynthesisUtterance !== 'function') {
    window.SpeechSynthesisUtterance = FakeUtterance;
  }

  function fire(u, type) {
    try {
      var ev;
      try { ev = new Event(type); } catch (x) { ev = { type: type }; }
      u.dispatchEvent(ev);
    } catch (x) {}
  }

  function fakeSpeak(u) {
    setTimeout(function () { fire(u, 'start'); }, 0);
    setTimeout(function () { fire(u, 'end'); }, 50);
  }

  var real = window.speechSynthesis;
  var hasReal = real && typeof real.speak === 'function';

  if (!hasReal) {
    var listeners = {};
    var fake = {
      speaking: false, pending: false, paused: false, onvoiceschanged: null,
      getVoices: function () { return [FAKE_VOICE]; },
      speak: fakeSpeak,
      cancel: function () {}, pause: function () {}, resume: function () {},
      addEventListener: function (t, f) { (listeners[t] = listeners[t] || []).push(f); },
      removeEventListener: function (t, f) {
        var a = listeners[t] || []; var i = a.indexOf(f); if (i >= 0) a.splice(i, 1);
      },
      dispatchEvent: function () { return true; }
    };
    try { Object.defineProperty(window, 'speechSynthesis', { value: fake, configurable: true, writable: true }); } catch (e) {}
    console.log('[tts-polyfill] fake speechSynthesis created');
    return;
  }

  var origGetVoices = real.getVoices.bind(real);
  var origSpeak = real.speak.bind(real);
  function noRealVoices() { try { return origGetVoices().length === 0; } catch (e) { return true; } }

  try {
    real.getVoices = function () { return noRealVoices() ? [FAKE_VOICE] : origGetVoices(); };
    real.speak = function (u) {
      if (noRealVoices()) { fakeSpeak(u); } else { origSpeak(u); }
    };
    console.log('[tts-polyfill] patched existing speechSynthesis');
  } catch (e) {
    console.log('[tts-polyfill] error patching: ' + e);
  }
})();
