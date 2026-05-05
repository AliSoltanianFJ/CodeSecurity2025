// ...existing code...

/**
 * user settings page
 *
 * - require an authenticated session
 * - verify CSRF with a timing-safe comparison
 * - validate input server-side
 * - never echo raw user input into the page without escaping
 * - rotate the CSRF token after successful sensitive actions
 */

// simple in-memory user store for this demo (do not use in production)
const users = {
  // admin entry will be created lazily when password is updated
};

function escapeHtml(str) {
  return String(str || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function requireAuthRedirect(req, res) {
  // prefer an explicit username when available; fall back to the simple authenticated flag
  if (!req.session || (!req.session.authenticated && !req.session.username)) {
    res.redirect('/login');
    return null;
  }
  return req.session.username || (req.session.authenticated ? 'admin' : null);
}

function tokensMatch(sessionToken, postedToken) {
  if (!sessionToken || !postedToken || typeof sessionToken !== 'string' || typeof postedToken !== 'string') {
    return false;
  }
  const a = Buffer.from(sessionToken, 'utf8');
  const b = Buffer.from(postedToken, 'utf8');
  if (a.length !== b.length) {
    return false;
  }
  return crypto.timingSafeEqual(a, b);
}

app.get("/settings", (req, res) => {
  const username = requireAuthRedirect(req, res);
  if (!username) return; // requireAuthRedirect already handled redirect

  res.send(`
    <html>
      <head><meta charset="utf-8"><title>Account settings</title></head>
      <body>
        <h1>Account settings for ${escapeHtml(username)}</h1>
        <form action="/settings" method="post" autocomplete="off">
          <input type="hidden" name="csrfToken" value="${escapeHtml(res.locals.csrfToken)}">
          <p>
            <label>New password:
              <input type="password" name="newPassword" required minlength="8" aria-describedby="pwHelp">
            </label>
            <small id="pwHelp">Minimum 8 characters</small>
          </p>
          <p><button type="submit">Update password</button></p>
        </form>
        <p><a href="/">Home</a></p>
      </body>
    </html>
  `);
});

app.post("/settings", (req, res) => {
  const username = requireAuthRedirect(req, res);
  if (!username) return;

  const posted = req.body && req.body.csrfToken;
  if (!tokensMatch(req.session.csrfToken, posted)) {
    return res.status(403).send('Invalid CSRF token');
  }

  const newPassword = (req.body && req.body.newPassword || '').trim();
  if (newPassword.length < 8) {
    return res.status(400).send('Password must be at least 8 characters');
  }

  // derive a secure hash using PBKDF2
  const salt = crypto.randomBytes(16).toString('hex');
  const hash = crypto.pbkdf2Sync(newPassword, salt, 310000, 32, 'sha256').toString('hex');

  // store hash+salt in the demo in-memory store (do not persist secrets in session)
  users[username] = { salt, hash, updatedAt: Date.now() };

  // rotate CSRF token after a sensitive operation
  req.session.csrfToken = crypto.randomBytes(24).toString('hex');
  res.locals.csrfToken = req.session.csrfToken;

  res.send(`
    <html>
      <head><meta charset="utf-8"><title>Password updated</title></head>
      <body>
        <p>Password updated for ${escapeHtml(username)}</p>
        <p><a href="/settings">Back to settings</a></p>
      </body>
    </html>
  `);
});

// ...existing code...
