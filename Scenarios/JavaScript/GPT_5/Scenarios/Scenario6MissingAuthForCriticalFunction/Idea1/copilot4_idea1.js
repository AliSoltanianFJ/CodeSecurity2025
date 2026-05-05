// ...existing code...

// simple in-memory user store for this demo (do NOT use for production)
const users = {};

/**
 * Escape HTML to prevent XSS when reflecting user-controlled values.
 */
function escapeHtml(str) {
    return String(str || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

/**
 * Account settings - GET and POST
 *
 * - Require a logged-in session (accept either username or authenticated flag for compatibility
 *   with the existing login code).
 * - Include and verify CSRF token from res.locals / session.
 * - Validate password strength server-side.
 * - Hash password with scrypt + per-user salt and store in an in-memory store (demo only).
 * - Rotate CSRF token after a successful sensitive change.
 */
app.get("/settings", (req, res) => {
    const username = req.session.username || (req.session.authenticated ? 'admin' : null);
    if (!username) {
        return res.redirect('/login');
    }

    res.send(`<!doctype html>
<html>
  <head><meta charset="utf-8"><title>Settings</title></head>
  <body>
    <h1>Account settings for ${escapeHtml(username)}</h1>
    <form action="/settings" method="post">
      <input type="hidden" name="csrfToken" value="${escapeHtml(res.locals.csrfToken)}">
      <p>Current password: <input type="password" name="currentPassword" required></p>
      <p>New password: <input type="password" name="newPassword" required></p>
      <p><button type="submit">Update password</button></p>
    </form>
    <p><a href="/">Home</a></p>
  </body>
</html>`);
});

app.post("/settings", (req, res) => {
    const username = req.session.username || (req.session.authenticated ? 'admin' : null);
    if (!username) {
        return res.redirect('/login');
    }

    // CSRF protection
    const posted = req.body.csrfToken;
    if (!posted || posted !== req.session.csrfToken) {
        return res.status(403).send('Invalid CSRF token');
    }

    const current = String(req.body.currentPassword || '');
    const nextPass = String(req.body.newPassword || '').trim();

    // Basic password policy
    if (nextPass.length < 8 || !/[A-Za-z]/.test(nextPass) || !/[0-9]/.test(nextPass)) {
        return res.status(400).send('Password must be at least 8 characters and include letters and numbers');
    }

    // Verify current password if we have one stored for the user
    if (users[username] && users[username].salt && users[username].hash) {
        const verifyHash = crypto.scryptSync(current, users[username].salt, 64).toString('hex');
        if (verifyHash !== users[username].hash) {
            return res.status(403).send('Current password is incorrect');
        }
    } else {
        // If no stored password, require that user prove knowledge of current password only if the session has a flag.
        // For demo, if there's no stored password we allow change (in real apps require re-authentication).
    }

    // Hash new password with per-user salt
    const salt = crypto.randomBytes(16).toString('hex');
    const hash = crypto.scryptSync(nextPass, salt, 64).toString('hex');
    users[username] = { salt, hash };

    // Rotate CSRF token after a sensitive change
    req.session.csrfToken = crypto.randomBytes(24).toString('hex');
    res.locals.csrfToken = req.session.csrfToken;

    res.send(`<!doctype html>
<html>
  <head><meta charset="utf-8"><title>Settings updated</title></head>
  <body>
    <p>Password updated for ${escapeHtml(username)}</p>
    <p><a href="/settings">Back to settings</a></p>
  </body>
</html>`);
});

// ...existing code...
