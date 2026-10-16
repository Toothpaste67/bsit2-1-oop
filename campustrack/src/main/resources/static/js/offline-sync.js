// Offline queue: scans that could not reach the server are kept in the browser's
// localStorage (first in, first out) and replayed with "Sync Queue Now".
(function () {
  const userId = document.body.getAttribute('data-user-id') || 'anonymous';
  const KEY = 'campustrack.offlineQueue.' + userId;
  const MAX_ITEMS = 50;

  function load() {
    try {
      const queue = JSON.parse(localStorage.getItem(KEY) || '[]');
      return Array.isArray(queue) ? queue : [];
    } catch (e) {
      return [];
    }
  }

  function save(queue) {
    try { localStorage.setItem(KEY, JSON.stringify(queue)); } catch (e) { /* storage full or blocked */ }
    render();
  }

  function render() {
    const count = load().length;
    const counter = document.getElementById('queue-count');
    const button = document.getElementById('sync-btn');
    if (counter) counter.textContent = count;
    if (button) button.disabled = count === 0;
  }

  function show(message, kind) {
    const box = document.getElementById('queue-status');
    if (!box) return;
    box.className = 'alert alert-' + kind;
    box.textContent = message;
    box.hidden = false;
  }

  window.OfflineQueue = {
    add: function (payload) {
      const queue = load();
      const alreadyQueued = queue.some(function (item) { return item.qrPayload === payload; });
      if (!alreadyQueued && queue.length < MAX_ITEMS) {
        queue.push({ qrPayload: payload, savedAt: new Date().toISOString() });
        save(queue);
      }
    },

    count: function () { return load().length; },

    sync: async function () {
      const queue = load();
      if (queue.length === 0) return;
      const button = document.getElementById('sync-btn');
      if (button) button.disabled = true;
      try {
        const body = queue.map(function (item) { return { qrPayload: item.qrPayload }; });
        const reply = await CampusTrack.post('/attendance/api/sync', body);
        // The server answered for every item, so the queue is done either way
        save([]);
        const rejected = (reply.data || []).filter(function (item) { return !item.success; });
        if (rejected.length === 0) {
          show(reply.message, 'success');
        } else {
          show(reply.message + ' Not recorded: ' + rejected.map(function (item) { return item.message; }).join(' / '), 'error');
        }
        if ((reply.data || []).length > rejected.length) setTimeout(function () { location.reload(); }, 1800);
      } catch (error) {
        show(error.offline ? 'Still offline. The queue is kept on this phone, try again later.' : error.message, 'error');
        render();
      }
    }
  };

  document.addEventListener('DOMContentLoaded', function () {
    render();
    const button = document.getElementById('sync-btn');
    if (button) button.addEventListener('click', function () { OfflineQueue.sync(); });
  });

  // Try on our own as soon as the network comes back
  window.addEventListener('online', function () {
    if (load().length > 0) OfflineQueue.sync();
  });
})();
