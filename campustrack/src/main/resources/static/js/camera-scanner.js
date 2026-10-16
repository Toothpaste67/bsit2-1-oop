// Student camera scanner (html5-qrcode). Works on mobile Safari (iOS) and Chrome (Android):
// the camera only starts from a tap, uses the back camera, and needs an HTTPS page.
(function () {
  let scanner = null;
  let busy = false;

  const overlay = document.getElementById('scanner-overlay');
  const statusBar = document.getElementById('scanner-status');
  const pageStatus = document.getElementById('scan-page-status');

  function overlayStatus(message) {
    if (statusBar) statusBar.textContent = message;
  }

  function pageMessage(message, kind) {
    if (!pageStatus) return;
    pageStatus.className = 'alert alert-' + kind;
    pageStatus.textContent = message;
    pageStatus.hidden = false;
    pageStatus.scrollIntoView({ block: 'nearest' });
  }

  function showPopup(result) {
    document.getElementById('popup-class').textContent = result.className;
    document.getElementById('popup-time').textContent = result.time;
    document.getElementById('popup-status').textContent = result.status;
    document.getElementById('checkin-popup').hidden = false;
  }

  async function startScanner() {
    if (!window.isSecureContext || !navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      pageMessage('The camera needs a secure page. Open CampusTrack with https:// (see the address your teacher gave you), or use the manual code below.', 'error');
      return;
    }
    if (!window.Html5Qrcode) {
      pageMessage('The scanner could not load (no internet). Use the manual code or the QR image upload below.', 'error');
      return;
    }
    busy = false;
    overlay.hidden = false;
    overlayStatus('Point the camera at the QR code on the screen.');
    scanner = new Html5Qrcode('reader');
    try {
      await scanner.start(
        { facingMode: 'environment' },
        {
          fps: 10,
          qrbox: function (width, height) {
            const side = Math.floor(Math.min(width, height) * 0.75);
            return { width: side, height: side };
          }
        },
        onScan
      );
    } catch (cameraError) {
      await stopScanner();
      pageMessage('Camera access was blocked. Allow the camera for this site in your browser settings, or use the manual code below.', 'error');
    }
  }

  async function stopScanner() {
    if (scanner) {
      try { await scanner.stop(); } catch (e) { /* was not running */ }
      try { scanner.clear(); } catch (e) { /* nothing to clear */ }
      scanner = null;
    }
    overlay.hidden = true;
  }

  function resume() {
    busy = false;
    overlayStatus('Point the camera at the QR code on the screen.');
    try { if (scanner) scanner.resume(); } catch (e) { /* already running */ }
  }

  async function onScan(text) {
    if (busy) return; // one request at a time; the camera keeps firing while the code is in view
    busy = true;
    try { scanner.pause(true); } catch (e) { /* older browsers */ }
    overlayStatus('Code read. Checking in...');

    try {
      const reply = await CampusTrack.post('/attendance/api/camera-scan', { qrPayload: text });
      if (reply.success) {
        await stopScanner();
        showPopup(reply.data);
      } else {
        overlayStatus(reply.message);
        setTimeout(resume, 2500);
      }
    } catch (error) {
      await stopScanner();
      if (error.offline) {
        OfflineQueue.add(text);
        pageMessage('No connection. Your scan was saved to the offline queue. Tap "Sync Queue Now" once you are back online.', 'info');
      } else {
        pageMessage(error.message, 'error');
      }
    }
  }

  document.addEventListener('DOMContentLoaded', function () {
    const startButton = document.getElementById('start-scan-btn');
    const closeButton = document.getElementById('close-scan-btn');
    const doneButton = document.getElementById('popup-done');
    if (startButton) startButton.addEventListener('click', startScanner);
    if (closeButton) closeButton.addEventListener('click', stopScanner);
    if (doneButton) doneButton.addEventListener('click', function () { location.href = '/attendance'; });

    // Opened from the dashboard's big "Scan QR" button (/attendance?scan=1): go straight to the camera
    if (new URLSearchParams(location.search).has('scan')) startScanner();
  });

  // Release the camera when the tab goes to the background (iOS keeps it locked otherwise)
  document.addEventListener('visibilitychange', function () {
    if (document.hidden && scanner) stopScanner();
  });
})();
