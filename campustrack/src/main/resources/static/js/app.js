// Shared helpers for every page.
(function () {
  function meta(name) {
    const tag = document.querySelector("meta[name='" + name + "']");
    return tag ? tag.getAttribute('content') : '';
  }

  window.CampusTrack = {
    // Headers for fetch(): JSON + the Spring Security CSRF token from the <meta> tags
    headers: function (json) {
      const headers = {};
      if (json !== false) headers['Content-Type'] = 'application/json';
      const token = meta('_csrf');
      const header = meta('_csrf_header');
      if (token && header) headers[header] = token;
      return headers;
    },

    // POST and parse the JSON reply. A network failure rejects with error.offline = true.
    post: async function (url, body, asForm) {
      let response;
      try {
        response = await fetch(url, {
          method: 'POST',
          credentials: 'same-origin',
          headers: asForm
            ? Object.assign(this.headers(false), { 'Content-Type': 'application/x-www-form-urlencoded' })
            : this.headers(),
          body: asForm ? new URLSearchParams(body).toString() : JSON.stringify(body)
        });
      } catch (networkError) {
        const error = new Error('No connection to the server.');
        error.offline = true;
        throw error;
      }
      const type = response.headers.get('content-type') || '';
      if (response.redirected || type.indexOf('application/json') < 0) {
        throw new Error(response.status === 403 || response.redirected
          ? 'Your session has expired. Reload the page and sign in again.'
          : 'The server could not process the request (' + response.status + ').');
      }
      return response.json();
    },

    escape: function (text) {
      const div = document.createElement('div');
      div.textContent = text == null ? '' : String(text);
      return div.innerHTML;
    }
  };

  document.addEventListener('DOMContentLoaded', function () {
    // Stop oversized uploads in the browser (the server limit is 5 MB)
    document.querySelectorAll('input[type=file]').forEach(function (input) {
      input.addEventListener('change', function () {
        const file = input.files && input.files[0];
        if (file && file.size > 5 * 1024 * 1024) {
          alert('That file is larger than 5 MB. Please choose a smaller one.');
          input.value = '';
        }
      });
    });

    // Ask before destructive actions
    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
      form.addEventListener('submit', function (event) {
        if (!confirm(form.getAttribute('data-confirm'))) event.preventDefault();
      });
    });
  });
})();
