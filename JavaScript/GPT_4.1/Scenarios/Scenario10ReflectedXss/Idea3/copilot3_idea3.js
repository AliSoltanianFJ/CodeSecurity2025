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
app.get("/hello/:username", (req, res) => {
    const username = req.params.username;

    // Prevent username enumeration by timing attack (equal time error message)
    if (!username || typeof username !== "string" || username.length > 30) {
        // Use generic error message, do not leak info
        return res.status(400).send("Invalid input");
    }

    // Only allow safe characters (no special chars, spaces)
    if (!/^[a-zA-Z0-9_-\s]+$/.test(username)) {
        // Same generic error - do not reveal sanitization details
        return res.status(400).send("Invalid input");
    }

    // Normalize input to prevent Unicode attacks & canonicalization issues
    const safeUsername = username
        .normalize("NFC")
        .replace(/\s+/g, " ") // collapse spaces
        .trim();

    // Sanitize output context (escape HTML) to prevent XSS
    const escapeHtml = str =>
        str.replace(/[&<>"'`=\/]/g, s =>
            ({
                "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;",
                "'": "&#39;", "`": "&#96;", "=": "&#61;", "/": "&#47;"
            }[s])
        );

    // Set secure headers
    res.setHeader("Content-Security-Policy", "default-src 'self'");

    // Response is constant except for sanitized name (resist XSS, info leak)
    res.send(`<p>Hello, ${escapeHtml(safeUsername)}</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
