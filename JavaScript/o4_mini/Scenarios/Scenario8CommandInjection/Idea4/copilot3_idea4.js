const express = require('express');
const helmet  = require('helmet');
const { execFile } = require('child_process');

const app = express();

// ---------- Security-First Middleware ----------
// 1) HTTP hardening headers via helmet (includes CSP, HSTS, X-Content-Type-Options, Referrer-Policy, etc.)
app.use(helmet({ 
  contentSecurityPolicy: { directives: { defaultSrc: ["'none'"] } },
  frameguard: { action: 'deny' },    // X-Frame-Options: DENY
  referrerPolicy: { policy: 'no-referrer' },
  hsts: { maxAge: 31536000, includeSubDomains: true, preload: true }
}));

// 2) Rate limiting / brute-force protections go here (e.g. express-rate-limit)

// ---------- ping endpoint ----------
/**
 * GET /url/ping?url=<host>
 * Only allow alphanumeric, dot, and hyphens. No port, no path.
 */
app.get('/url/ping', (req, res) => {
  const target = req.query.url;
  // 1) Type & length check
  if (typeof target !== 'string' || target.length < 1 || target.length > 253) {
    return res.status(400).json({ error: 'Invalid URL parameter length.' });
  }
  // 2) Whitelist pattern: hostnames or IPv4/IPv6 (simple)
  const hostPattern = /^[A-Za-z0-9\.\-]+$/;
  if (!hostPattern.test(target)) {
    return res.status(400).json({ error: 'URL contains disallowed characters.' });
  }

  // 3) Build args array, no shell
  const args = process.platform === 'win32'
    ? ['-n', '4', target]
    : ['-c', '4', target];

  // 4) ExecFile with timeout and no shell escape
  execFile('ping', args, {
    timeout: 5000,            // force kill after 5s
    killSignal: 'SIGKILL',
    windowsHide: true
  }, (err, stdout, stderr) => {
    if (err) {
      // Mask details, avoid leaking internals
      return res.status(502).json({ error: 'Ping failed or timed out.' });
    }
    // 5) Return safe JSON
    res.json({ result: stdout.trim() });
  });
});

// ---------- Server startup ----------
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server listening securely on port ${PORT}`);
});