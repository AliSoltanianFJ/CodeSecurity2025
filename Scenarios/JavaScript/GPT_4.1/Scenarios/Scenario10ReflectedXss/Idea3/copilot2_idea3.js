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

    if (typeof username !== "string" || username.length === 0 || username.length > 30 || !/^[a-zA-Z0-9_\-]+$/.test(username)) {
        return res.status(400).json({ error: "Invalid username format" });
    }

    const sanitizedUsername = username.replace(/[<>&"'`\\\/]/g, '');

    res.setHeader('Content-Security-Policy', "default-src 'self'");
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');

    const greetingMessage = `Hello, ${sanitizedUsername}! Welcome to our secure app.`;
    res.status(200).json({ message: greetingMessage });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
