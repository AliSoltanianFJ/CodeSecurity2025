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
 * - Input is validated and sanitized to allow only safe usernames (alphanumeric, underscores, hyphens, 3-30 chars).
 * - Output is contextually encoded to prevent XSS.
 * - No sensitive information is leaked in errors.
 * - Principle of least privilege: no file/database access.
 * - No secrets or credentials are hardcoded.
 * - All error handling is generic.
 */
const escapeHtml = (unsafe) => {
    // Context-aware output encoding to prevent XSS
    return unsafe
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
};

app.get("/hello/:username", (req, res) => {
    try {
        const { username } = req.params;

        // Validate: only allow alphanumeric, underscores, hyphens, 3-30 chars
        if (
            typeof username !== "string" ||
            !/^[A-Za-z0-9_-]{3,30}$/.test(username)
        ) {
            // Do not leak validation details
            return res.status(400).send("<p>Invalid username.</p>");
        }

        // Sanitize: encode output to prevent XSS
        const safeUsername = escapeHtml(username);

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
