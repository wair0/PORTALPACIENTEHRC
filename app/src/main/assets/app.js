/* Portal Paciente HRC – UI offline mínima. */
(function () {
  'use strict';
  var N = window.HRCNative || null;
  var $ = function (id) { return document.getElementById(id); };

  function go(id) {
    ['login', 'home', 'exams', 'profile'].forEach(function (s) {
      var el = $(s);
      if (el) el.classList.toggle('on', s === id);
    });
    var nav = $('nav');
    if (nav) nav.classList.toggle('off', id === 'login');
    var bell = $('bell');
    if (bell) bell.hidden = id === 'login';
  }

  function toast(msg) {
    var t = $('toast');
    if (!t) return;
    t.textContent = msg;
    t.classList.add('show');
    setTimeout(function () { t.classList.remove('show'); }, 3000);
  }

  document.addEventListener('DOMContentLoaded', function () {
    var loginBtn = $('loginBtn');
    if (loginBtn) {
      loginBtn.addEventListener('click', function () {
        if (N && N.login) {
          N.login('', '');
        } else {
          toast('Login (demo)');
          go('home');
        }
      });
    }

    var logoutBtn = $('logoutBtn');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', function () {
        if (N && N.logout) N.logout();
        go('login');
      });
    }

    Array.prototype.forEach.call(document.querySelectorAll('[data-go]'), function (b) {
      b.addEventListener('click', function () {
        go(b.getAttribute('data-go'));
      });
    });

    if (N && N.isLoggedIn && N.isLoggedIn()) {
      go('home');
    } else {
      go('login');
    }
  });

  window.HRC = {
    receive: function (kind, json) {
      console.log('HRC.receive', kind, json);
      if (kind === 'auth' && json && json.indexOf('"ok":true') >= 0) go('home');
    }
  };
})();
