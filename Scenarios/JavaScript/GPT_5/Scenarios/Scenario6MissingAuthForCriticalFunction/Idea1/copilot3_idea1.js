// ...existing code...

// robust authentication middleware for settings routes
function ensureAuthenticatedForSettings(req, res, next) {
    if (req.session && (req.session.username || req.session.authenticated)) {
        return next();
    }
    return res.redirect("/login");
}

// settings: show and update a non-sensitive user preference (demo)
// - requires authentication
// - uses server-side CSRF token with constant-time comparison
// - escapes all user-controlled output
app.get("/settings", ensureAuthenticatedForSettings, (req, res) => {
    const username = escapeHtml(req.session.username || "user");
    const displayName = escapeHtml(req.session.displayName || "");
    res.send(`
        <html>
          <body>
            <h1>Account settings for ${username}</h1>
            <form action="/settings" method="post">
              <input type="hidden" name="csrfToken" value="${res.locals.csrfToken}">
              <p>Display name: <input type="text" name="displayName" value="${displayName}" maxlength="50"></p>
              <p><button type="submit">Update</button></p>
            </form>
            <p><a href="/">Home</a></p>
          </body>
        </html>
    `);
});

app.post("/settings", ensureAuthenticatedForSettings, (req, res) => {
    const posted = String(req.body.csrfToken || "");
    const sessionToken = String(req.session.csrfToken || "");

    // constant-time CSRF token comparison
    try {
        const a = Buffer.from(posted, "utf8");
        const b = Buffer.from(sessionToken, "utf8");
        if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) {
            return res.status(403).send("Invalid CSRF token");
        }
    } catch (e) {
        return res.status(403).send("Invalid CSRF token");
    }

    const displayName = (req.body.displayName || "").trim();
    if (displayName.length > 50) {
        return res.status(400).send("Display name too long");
    }

    // store non-sensitive demo setting in session (for demo purposes only)
    req.session.displayName = displayName;

    // rotate CSRF token after a successful sensitive action
    req.session.csrfToken = crypto.randomBytes(24).toString("hex");
    res.locals.csrfToken = req.session.csrfToken;

    res.send(`
      <html>
        <body>
          <p>Settings updated for ${escapeHtml(req.session.username || "user")}</p>
          <p><a href="/settings">Back to settings</a></p>
        </body>
      </html>
    `);
});

// small helper to escape HTML
function escapeHtml(str) {
    return String(str)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
}

// ...existing code...
