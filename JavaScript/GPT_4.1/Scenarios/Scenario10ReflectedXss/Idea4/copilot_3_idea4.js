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
 * - Input is validated and sanitized to allow only safe usernames (alphanumeric, underscores, hyphens, 3-30 chars).
 * - Output is contextually encoded to prevent XSS.
 * - No sensitive information is leaked in errors.
 * - No secrets or credentials are hardcoded.
 * - Principle of least privilege: no file/database access.
 * - All headers set securely elsewhere.
 */
app.get("/hello/:username", (req, res) => {
    try {
        const username = req.params.username;

        // Validate username: only allow alphanumeric, underscores, hyphens, 3-30 chars
        const USERNAME_REGEX = /^[a-zA-Z0-9_-]{3,30}$/;
        if (!USERNAME_REGEX.test(username)) {
            // Do not leak validation details
            return res.status(400).send("Invalid username.");
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
        // Log error securely (not shown here), do not leak details to user
        res.status(500).send("An unexpected error occurred.");
    }
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
