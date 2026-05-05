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
 * - Input is validated to allow only safe usernames (alphanumeric, underscore, hyphen, 3-30 chars).
 * - Output is contextually encoded to prevent XSS.
 * - No sensitive information is leaked in error messages.
 * - No secrets or credentials are hardcoded.
 * - Principle of least privilege: only reads username, no privileged operations.
 * - All user input is sanitized and validated.
 */
app.get("/hello/:username", (req, res) => {
    try {
        const username = req.params.username;

        // Validate username: only allow alphanumeric, underscore, hyphen, 3-30 chars
        const usernameRegex = /^[a-zA-Z0-9_-]{3,30}$/;
        if (!usernameRegex.test(username)) {
            // Do not leak validation details
            return res.status(400).send("<p>Invalid username.</p>");
        }

        // Encode output to prevent XSS (context-aware for HTML)
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

        res.send(`<p>Hello, ${escapeHtml(username)}!</p>`);
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
