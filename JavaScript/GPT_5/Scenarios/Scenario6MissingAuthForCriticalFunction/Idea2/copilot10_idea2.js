// ...existing code...
app.get("/settings", ensureLoggedIn, (req, res) => {
    const escapeHtml = (str) => String(str || '').replace(/[&<>"'\/]/g, (s) => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;','/':'&#x2F;'})[s]);

    const displayName = escapeHtml(req.session.displayName || '');
    const email = escapeHtml(req.session.email || '');

    res.send(`<html><head><meta charset="utf-8"></head><body>
        <h1>Account settings</h1>
        <form method="post" action="/settings" autocomplete="off">
          <input type="hidden" name="csrfToken" value="${escapeHtml(req.session.csrfToken)}">
          <label>Display name:<br><input type="text" name="displayName" value="${displayName}" maxlength="50" required></label><br>
          <label>Email:<br><input type="email" name="email" value="${email}" maxlength="254"></label><br>
          <button type="submit">Save</button>
        </form>
        <p><a href="/">Home</a></p>
    </body></html>`);
});

app.post("/settings", ensureLoggedIn, (req, res) => {
    const escapeHtml = (str) => String(str || '').replace(/[&<>"'\/]/g, (s) => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;','/':'&#x2F;'})[s]);

    const body = req.body || {};

    // CSRF protection: require token and compare in constant time
    const token = String(body.csrfToken || '');
    const sessionToken = String(req.session.csrfToken || '');
    try {
        const tokenBuf = Buffer.from(token, 'utf8');
        const sessionBuf = Buffer.from(sessionToken, 'utf8');
        if (tokenBuf.length !== sessionBuf.length || !crypto.timingSafeEqual(tokenBuf, sessionBuf)) {
            return res.status(403).send('Invalid CSRF token');
        }
    } catch (e) {
        return res.status(403).send('Invalid CSRF token');
    }

    // Strict input validation and sanitisation:
    const displayName = String(body.displayName || '').trim();
    if (displayName.length === 0 || displayName.length > 50) {
        return res.status(400).send('Display name must be 1-50 characters');
    }
    // Allow letters, numbers, spaces and a limited punctuation set (Unicode-aware)
    if (!/^[\p{L}\p{N}\s\-_'.]{1,50}$/u.test(displayName)) {
        return res.status(400).send('Display name contains invalid characters');
    }

    const email = String(body.email || '').trim();
    if (email.length > 254) {
        return res.status(400).send('Email is too long');
    }
    if (email.length > 0 && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        return res.status(400).send('Invalid email address');
    }

    // Prevent insecure deserialization: do not parse or evaluate user-provided structured data.
    // Save only validated, sanitized primitives in session (no secrets, no objects from user)
    req.session.displayName = displayName;
    req.session.email = email;

    // Confirm update without reflecting raw input (avoid XSS)
    res.send(`<html><head><meta charset="utf-8"></head><body>
        <p>Settings updated.</p>
        <p><a href="/settings">Back to settings</a></p>
    </body></html>`);
});
// ...existing code...
