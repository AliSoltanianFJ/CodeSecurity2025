// ...existing code...

// in-memory user store (for demo only — do NOT use production storage like this)
const users = {};

app.get("/settings", (req, res) => {
    // require an authenticated session
    if (!req.session || !req.session.authenticated) {
        return res.redirect("/login");
    }

    // show a minimal settings form; do not echo user-controlled values directly
    res.send(`
        <html>
          <body>
            <h1>Account settings</h1>
            <form action="/settings" method="post">
              <input type="hidden" name="csrfToken" value="${res.locals.csrfToken}">
              <p>New password: <input type="password" name="newPassword" autocomplete="new-password"></p>
              <p><button type="submit">Update password</button></p>
            </form>
            <p><a href="/">Home</a></p>
          </body>
        </html>
    `);
});

app.post("/settings", (req, res) => {
    // require an authenticated session
    if (!req.session || !req.session.authenticated) {
        return res.redirect("/login");
    }

    // CSRF protection using constant-time comparison
    const posted = String(req.body.csrfToken || '');
    const sessionToken = String(req.session.csrfToken || '');

    const postedBuf = Buffer.from(posted);
    const sessionBuf = Buffer.from(sessionToken);

    if (postedBuf.length === 0 || sessionBuf.length === 0 || postedBuf.length !== sessionBuf.length || !crypto.timingSafeEqual(postedBuf, sessionBuf)) {
        return res.status(403).send('Invalid CSRF token');
    }

    // validate password strength
    const newPassword = String(req.body.newPassword || '').trim();
    if (newPassword.length < 12) {
        return res.status(400).send('Password must be at least 12 characters');
    }

    // hash the password using scrypt with a random salt
    const salt = crypto.randomBytes(16).toString('hex');
    const hash = crypto.scryptSync(newPassword, salt, 64).toString('hex');

    // store credential in server memory keyed by session id (demo only)
    users[req.sessionID] = {
        hash,
        salt,
        updatedAt: Date.now()
    };

    // rotate CSRF token after sensitive action
    req.session.csrfToken = crypto.randomBytes(24).toString('hex');
    res.locals.csrfToken = req.session.csrfToken;

    res.send(`
        <html>
          <body>
            <p>Settings updated.</p>
            <p><a href="/settings">Back to settings</a></p>
          </body>
        </html>
    `);
});

// ...existing code...
