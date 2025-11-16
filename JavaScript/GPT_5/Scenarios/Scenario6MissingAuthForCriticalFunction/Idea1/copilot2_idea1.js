// ...existing code...
/**
 * user settings page
 */
app.get("/settings", (req, res) => {
    // Require an authenticated session (do not rely on unset username)
    if (!req.session || req.session.authenticated !== true) {
        return res.redirect("/login");
    }

    const currentName = (req.session.settings && req.session.settings.displayName) || '';

    // Minimal HTML with server-side escaping and an anti-CSRF hidden field
    res.send(`
        <html>
          <head><meta charset="utf-8"/></head>
          <body>
            <h1>Account settings</h1>
            <form action="/settings" method="post" autocomplete="off" novalidate>
              <input type="hidden" name="csrfToken" value="${res.locals.csrfToken}">
              <p>
                Display name:
                <input type="text" name="displayName" value="${escapeHtml(currentName)}" maxlength="50" />
              </p>
              <p><button type="submit">Save</button></p>
            </form>
            <p><a href="/">Home</a></p>
          </body>
        </html>
    `);
});

app.post("/settings", (req, res) => {
    // Ensure user is authenticated
    if (!req.session || req.session.authenticated !== true) {
        return res.redirect("/login");
    }

    // Validate CSRF token using a timing-safe comparison
    const posted = String(req.body.csrfToken || '');
    const stored = String(req.session.csrfToken || '');
    try {
        const a = Buffer.from(posted, 'utf8');
        const b = Buffer.from(stored, 'utf8');
        if (a.length === 0 || b.length === 0 || a.length !== b.length || !crypto.timingSafeEqual(a, b)) {
            return res.status(403).send('Invalid CSRF token');
        }
    } catch (e) {
        return res.status(403).send('Invalid CSRF token');
    }

    // Validate and normalize input
    const newNameRaw = String(req.body.displayName || '').trim();
    if (newNameRaw.length > 50) {
        return res.status(400).send('Display name too long');
    }
    // Reject control characters
    if (/[\u0000-\u001f]/.test(newNameRaw)) {
        return res.status(400).send('Invalid characters in display name');
    }

    // Persist in session (for demo only). In real apps persist to a database.
    const newSettings = { displayName: newNameRaw };

    // Regenerate session id to mitigate session fixation after a sensitive change,
    // then restore necessary session state and rotate CSRF token.
    const preserve = {
        authenticated: true,
        username: req.session.username, // may be undefined for this demo
        settings: newSettings
    };

    req.session.regenerate(function (err) {
        if (err) {
            return res.status(500).send('Session error');
        }
        Object.assign(req.session, preserve);
        req.session.csrfToken = crypto.randomBytes(24).toString('hex');
        // update locals for this response cycle (not strictly necessary for redirect)
        res.locals.csrfToken = req.session.csrfToken;

        res.send(`<html>
            <body>
              <p>Settings updated.</p>
              <p>Display name: ${escapeHtml(newSettings.displayName)}</p>
              <p><a href="/settings">Back to settings</a></p>
            </body>
        </html>`);
    });
});

// small helper to escape HTML
function escapeHtml(str) {
    return String(str || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
}
// ...existing code...
