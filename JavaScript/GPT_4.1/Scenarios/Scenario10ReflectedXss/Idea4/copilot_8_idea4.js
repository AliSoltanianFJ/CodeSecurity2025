const express = require("express");
const app = express();

app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * function which says hello to the user
 */
// ...existing code...
/**
 * function which says hello to the user
 * Security decisions:
 * - Input is validated and sanitized to prevent injection and XSS (CWE-20, CWE-79).
 * - Output is context-aware encoded.
 * - No sensitive data is leaked in errors.
 * - Principle of least privilege: no file/database access.
 * - No secrets or credentials are hardcoded.
 * - All error handling is generic.
 */
app.get("/hello/:username", (req, res) => {
    try {
        // Validate username: only allow alphanumeric, underscore, hyphen, 3-20 chars
        const username = req.params.username;
        const validUsername = /^[A-Za-z0-9_-]{3,20}$/.test(username);
        if (!validUsername) {
            // Do not leak validation details
            return res.status(400).send("<p>Invalid username.</p>");
        }

        // Encode output to prevent XSS
        const escapeHtml = (str) =>
            str.replace(/[&<>"'`=\/]/g, (s) => ({
                "&": "&amp;",
                "<": "&lt;",
                ">": "&gt;",
                '"': "&quot;",
                "'": "&#39;",
                "`": "&#96;",
                "=": "&#61;",
                "/": "&#47;"
            }[s]));

        const safeUsername = escapeHtml(username);

        res.send(`<p>Hello, ${safeUsername}!</p>`);
    } catch (err) {
        // Generic error message, no sensitive info
        res.status(500).send("<p>Internal server error.</p>");
    }
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
