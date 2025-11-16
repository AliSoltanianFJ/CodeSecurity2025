// ...existing code...

// simple in-memory user store for demo (do NOT use in production)
const users = (() => {
    // initialize 'admin' user to match existing login check ("admin"/"password")
    const salt = crypto.randomBytes(16).toString('hex');
    const hash = crypto.scryptSync("password", salt, 64).toString('hex');
    return {
        admin: { salt, hash }
    };
})();

function hashPassword(password) {
    const salt = crypto.randomBytes(16).toString('hex');
    const hash = crypto.scryptSync(password, salt, 64).toString('hex');
    return { salt, hash };
}

function verifyPassword(stored, password) {
    if (!stored || !stored.salt || !stored.hash) return false;
    const hash = crypto.scryptSync(password, stored.salt, 64).toString('hex');
    // constant-time comparison
    const a = Buffer.from(hash, 'hex');
    const b = Buffer.from(stored.hash, 'hex');
    if (a.length !== b.length) return false;
    return crypto.timingSafeEqual(a, b);
}

function escapeHtml(str) {
    return String(str || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

// settings routes with proper auth, CSRF, and secure password handling
app.get("/settings", (req, res) => {
    // require authentication
    if (!req.session || !req.session.authenticated) {
        return res.redirect("/login");
    }

    const username = escapeHtml(req.session.username || 'admin');

    res.send(`
        <html>
          <body>
            <h1>Account settings for ${username}</h1>
            <form action="/settings" method="post">
              <input type="hidden" name="csrfToken" value="${escapeHtml(res.locals.csrfToken)}">
              <p>Current password: <input type="password" name="currentPassword" autocomplete="current-password"></p>
              <p>New password: <input type="password" name="newPassword" autocomplete="new-password"></p>
              <p><button type="submit">Update password</button></p>
            </form>
            <p><a href="/">Home</a></p>
          </body>
        </html>
    `);
});

app.post("/settings", (req, res) => {
    // require authentication
    if (!req.session || !req.session.authenticated) {
        return res.redirect("/login");
    }

    // CSRF protection - use timing-safe comparison
    const posted = String(req.body.csrfToken || '');
    const stored = String(req.session.csrfToken || '');
    const postedBuf = Buffer.from(posted);
    const storedBuf = Buffer.from(stored);
    if (postedBuf.length === 0 || storedBuf.length === 0 || postedBuf.length !== storedBuf.length ||
        !crypto.timingSafeEqual(postedBuf, storedBuf)) {
        return res.status(403).send('Forbidden');
    }

    const current = String(req.body.currentPassword || '');
    const next = String(req.body.newPassword || '').trim();

    // basic validation
    if (!current || !next) {
        return res.status(400).send('Missing password fields');
    }
    if (next.length < 12) {
        return res.status(400).send('New password must be at least 12 characters');
    }

    const usernameKey = req.session.username || 'admin'; // fallback to 'admin' for this demo
    const user = users[usernameKey];

    // verify current password
    if (!verifyPassword(user, current)) {
        // do not reveal which part failed
        return res.status(400).send('Invalid credentials');
    }

    // update stored password (securely hashed with new salt)
    const newStored = hashPassword(next);
    users[usernameKey] = newStored;

    // rotate CSRF token after sensitive change
    req.session.csrfToken = crypto.randomBytes(24).toString('hex');
    res.locals.csrfToken = req.session.csrfToken;

    res.send(`
        <html>
          <body>
            <p>Password updated.</p>
            <p><a href="/settings">Back to settings</a></p>
          </body>
        </html>
    `);
});

// ...existing code...
