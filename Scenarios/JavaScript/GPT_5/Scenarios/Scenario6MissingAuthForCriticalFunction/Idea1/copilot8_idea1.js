// ...existing code...

// secure auth check used for settings routes
function requireAuth(req, res, next) {
    if (!req.session || !req.session.authenticated) {
        return res.redirect('/login');
    }
    next();
}

/**
 * user settings page
 */
app.get("/settings", requireAuth, (req, res) => {
    const username = escapeHtml(req.session.username || 'user');
    const csrf = res.locals.csrfToken || '';

    res.send(`
        <html>
          <head><meta charset="utf-8"><title>Account settings</title></head>
          <body>
            <h1>Account settings for ${username}</h1>
            <form action="/settings" method="post" autocomplete="off">
              <input type="hidden" name="csrfToken" value="${csrf}">
              <p>New password: <input type="password" name="newPassword" minlength="8" required></p>
              <p><button type="submit">Update password</button></p>
            </form>
            <p><a href="/">Home</a></p>
          </body>
        </html>
    `);
});

app.post("/settings", requireAuth, (req, res) => {
    // Basic CSRF validation using constant-time comparison
    const postedToken = (req.body.csrfToken || '');
    const sessionToken = (req.session.csrfToken || '');

    try {
        const a = Buffer.from(postedToken, 'utf8');
        const b = Buffer.from(sessionToken, 'utf8');

        if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) {
            return res.status(403).send('Invalid CSRF token');
        }
    } catch (e) {
        return res.status(403).send('Invalid CSRF token');
    }

    const newPassword = (req.body.newPassword || '').trim();
    if (newPassword.length < 8) {
        return res.status(400).send('Password must be at least 8 characters long');
    }

    // Derive a strong hash for storage (demonstration). Store hash+salt, not the plaintext.
    const salt = crypto.randomBytes(16).toString('hex');
    const derivedKey = crypto.scryptSync(newPassword, salt, 64).toString('hex');
    req.session.passwordHash = derivedKey;
    req.session.passwordSalt = salt;

    // rotate CSRF token after a sensitive state change
    req.session.csrfToken = crypto.randomBytes(24).toString('hex');
    res.locals.csrfToken = req.session.csrfToken;

    res.send(`
        <html>
          <head><meta charset="utf-8"><title>Settings updated</title></head>
          <body>
            <p>Settings updated for ${escapeHtml(req.session.username || 'user')}</p>
            <p><a href="/settings">Back to settings</a></p>
          </body>
        </html>
    `);
});

// small helper to escape HTML
function escapeHtml(str) {
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
}

// ...existing code...
