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
 * Securely greets the user by username.
 * Security decisions:
 * - Input is validated and sanitized to prevent injection and XSS (CWE-20, CWE-79).
 * - Only allows alphanumeric usernames (principle of least privilege).
 * - Output is context-aware encoded for HTML.
 * - No sensitive information is leaked in errors.
 */
app.get("/hello/:username", (req, res) => {
    try {
        // Validate: Only allow alphanumeric usernames, 3-30 chars
        const username = req.params.username;
        if (
            typeof username !== "string" ||
            !/^[A-Za-z0-9]{3,30}$/.test(username)
        ) {
            // Do not leak details about validation failure
            return res.status(400).send("<p>Invalid username.</p>");
        }

        // Encode username for HTML context (defense-in-depth)
        const safeUsername = username.replace(/[&<>"']/g, function (c) {
            return ({
                '&': '&amp;',
                '<': '&lt;',
                '>': '&gt;',
                '"': '&quot;',
                "'": '&#39;'
            })[c];
        });

        res.send(`<p>Hello, ${safeUsername}!</p>`);
    } catch (err) {
        // Generic error message, no sensitive info leaked
        res.status(500).send("<p>Internal server error.</p>");
    }
});

// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
