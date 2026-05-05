// ...existing code...

/*
 * Secure user settings handlers
 *
 * - Requires an authenticated session (checks req.session.authenticated)
 * - Protects against CSRF using timing-safe comparison
 * - Validates and sanitizes all input (limits length, basic email validation)
 * - Escapes output to prevent XSS
 * - Adds CSP and no-store cache headers for sensitive responses
 * - Does not expose private session information
 */
const MAX_FIELD_LEN = 128;

function escapeHtml(str) {
    if (typeof str !== 'string') return '';
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function truncateSafe(str, maxLen) {
    if (typeof str !== 'string') return '';
    if (str.length <= maxLen) return str;
    return str.slice(0, maxLen);
}

function isValidEmail(email) {
    if (typeof email !== 'string') return false;
    const e = email.trim();
    if (e.length === 0 || e.length > 254) return false;
    // Basic email regex (keeps complexity low, acceptable for validation here)
    // This avoids using external validators while still catching obvious invalid input.
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(e);
}

app.get("/settings", (req, res) => {
    // Require explicit authentication flag set at login
    if (!req.session || req.session.authenticated !== true) {
        return res.redirect("/login");
    }

    // Prevent caching of pages with sensitive info
    res.setHeader('Cache-Control', 'no-store');
    // Restrictive Content Security Policy for this page
    res.setHeader('Content-Security-Policy', "default-src 'self'");

    // Load current settings from session (do not reveal anything sensitive)
    const current = req.session.settings || {};
    const safeDisplayName = escapeHtml(truncateSafe(current.displayName || '', MAX_FIELD_LEN));
    const safeEmail = escapeHtml(truncateSafe(current.email || '', MAX_FIELD_LEN));
    const safeCsrf = escapeHtml(String(res.locals.csrfToken || ''));

    // Render a minimal, safe HTML form. All dynamic values are escaped.
    res.send(`
        <html lang="en">
        <head>
            <meta charset="utf-8">
            <title>User settings</title>
        </head>
        <body>
            <h1>Account settings</h1>
            <form action="/settings" method="post" autocomplete="off" novalidate>
                <input type="hidden" name="csrfToken" value="${safeCsrf}">
                <label>
                    Display name
                    <input type="text" name="displayName" maxlength="${MAX_FIELD_LEN}" value="${safeDisplayName}">
                </label>
                <br>
                <label>
                    Email
                    <input type="email" name="email" maxlength="${MAX_FIELD_LEN}" value="${safeEmail}">
                </label>
                <br>
                <button type="submit">Save</button>
            </form>
            <p><a href="/">Home</a></p>
        </body>
        </html>
    `);
});

app.post("/settings", (req, res) => {
    // Require explicit authentication flag set at login
    if (!req.session || req.session.authenticated !== true) {
        return res.redirect("/login");
    }

    // Prevent caching of pages with sensitive info
    res.setHeader('Cache-Control', 'no-store');
    res.setHeader('Content-Security-Policy', "default-src 'self'");

    // Validate CSRF token using timing-safe comparison
    const submitted = String(req.body && req.body.csrfToken ? req.body.csrfToken : '');
    const stored = String(req.session && req.session.csrfToken ? req.session.csrfToken : '');
    let csrfValid = false;
    try {
        const a = Buffer.from(submitted, 'utf8');
        const b = Buffer.from(stored, 'utf8');
        if (a.length === b.length && crypto.timingSafeEqual(a, b)) {
            csrfValid = true;
        }
    } catch (err) {
        csrfValid = false;
    }
    if (!csrfValid) {
        return res.status(403).send('Forbidden: invalid request.');
    }

    // Validate and sanitize inputs
    const rawDisplayName = typeof req.body.displayName === 'string' ? req.body.displayName.trim() : '';
    const rawEmail = typeof req.body.email === 'string' ? req.body.email.trim() : '';

    if (rawDisplayName.length > MAX_FIELD_LEN || rawEmail.length > MAX_FIELD_LEN) {
        return res.status(400).send('Invalid input: fields too long.');
    }

    if (rawDisplayName.length === 0) {
        return res.status(400).send('Invalid input: display name required.');
    }

    if (!isValidEmail(rawEmail)) {
        return res.status(400).send('Invalid input: email address not valid.');
    }

    // Final sanitized values (escape before any output; store minimal safe representation)
    const sanitizedDisplayName = truncateSafe(rawDisplayName, MAX_FIELD_LEN);
    const sanitizedEmail = truncateSafe(rawEmail.toLowerCase(), MAX_FIELD_LEN);

    // Store only non-sensitive, validated settings in session (avoid storing tokens)
    req.session.settings = {
        displayName: sanitizedDisplayName,
        email: sanitizedEmail
    };

    // Respond without echoing sensitive details (escape if ever output)
    res.send(`
        <html lang="en">
        <head><meta charset="utf-8"><title>Settings updated</title></head>
        <body>
            <p>Settings updated successfully.</p>
            <p><a href="/settings">Back to settings</a></p>
        </body>
        </html>
    `);
});
// ...existing code...
