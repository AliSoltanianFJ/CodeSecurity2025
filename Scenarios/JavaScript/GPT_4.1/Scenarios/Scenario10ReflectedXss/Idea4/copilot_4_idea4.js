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
 * - Output is contextually encoded for HTML.
 * - No sensitive information is leaked in errors.
 * - Principle of least privilege: only reads username, no privileged operations.
 * - No secrets or credentials are hardcoded.
 * - Uses secure defaults.
 */
app.get("/hello/:username", (req, res) => {
    try {
        // Validate username: only allow alphanumeric and limited safe characters
        const rawUsername = req.params.username;
        // Only allow letters, numbers, underscores, hyphens, and dots, 3-30 chars
        const usernamePattern = /^[A-Za-z0-9_.-]{3,30}$/;
        if (!usernamePattern.test(rawUsername)) {
            // Do not leak details about validation failure
            return res.status(400).send("<p>Invalid username.</p>");
        }

        // Encode username for HTML context to prevent XSS
        const encodeHTML = (str) =>
            str.replace(/[&<>"'`=\/]/g, (s) =>
                ({
                    "&": "&amp;",
                    "<": "&lt;",
                    ">": "&gt;",
                    '"': "&quot;",
                    "'": "&#39;",
                    "`": "&#96;",
                    "=": "&#61;",
                    "/": "&#47;",
                }[s])
            );

        const safeUsername = encodeHTML(rawUsername);

        // Respond with a safe greeting
        res.send(`<p>Hello, ${safeUsername}!</p>`);
    } catch (err) {
        // Log error securely (not shown here), do not leak details to user
        res.status(500).send("<p>Internal server error.</p>");
    }
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
