/* Portal Paciente HRC – UI offline. Estilo High-Tech Corporativo. */
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
    Array.prototype.forEach.call(document.querySelectorAll('.nav button'), function (b) {
      b.classList.toggle('on', b.getAttribute('data-go') === id);
    });
  }

  function toast(msg) {
    var t = $('toast');
    if (!t) return;
    t.textContent = msg;
    t.classList.add('show');
    setTimeout(function () { t.classList.remove('show'); }, 3000);
  }

  function closeDrawer() {
    var d = $('drawer');
    if (d) d.hidden = true;
  }

  document.addEventListener('DOMContentLoaded', function () {
    var loginBtn = $('loginBtn');
    if (loginBtn) {
      loginBtn.addEventListener('click', function () {
        if (N && N.login) {
          N.login('', '');
        } else {
          toast('Modo demo');
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
    var menuBtn = $('menuBtn');
    if (menuBtn) {
      menuBtn.addEventListener('click', function () {
        var d = $('drawer');
        if (d) d.hidden = !d.hidden;
      });
    }
    var drawer = $('drawer');
    if (drawer) {
      drawer.addEventListener('click', function (e) {
        if (e.target === drawer) closeDrawer();
      });
    }
    Array.prototype.forEach.call(document.querySelectorAll('[data-go]'), function (b) {
      b.addEventListener('click', function () {
        var act = b.getAttribute('data-act');
        if (act === 'logout' && N && N.logout) N.logout();
        go(b.getAttribute('data-go'));
        closeDrawer();
      });
    });
    if (N && N.isLoggedIn && N.isLoggedIn()) go('home');
    else go('login');
  });

  window.HRC = {
    receive: function (kind, json) {
      if (kind === 'auth' && json && json.indexOf('"ok":true') >= 0) go('home');
    }
  };
})();
