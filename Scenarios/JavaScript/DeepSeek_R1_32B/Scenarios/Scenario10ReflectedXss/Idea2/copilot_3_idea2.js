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
    // Extract username from URL parameter
    const username = req.params.username;

    // 1. Input validation and sanitization
    if (!username || typeof username !== 'string') {
        return res.status(400).send({ error: "Invalid username" });
    }

    // Sanitize the input to prevent XSS and other injection attacks
    const sanitizedUsername = escape(username);

    // Validate username length (prevent potential buffer overflow)
    if (sanitizedUsername.length > 50) {
        return res.status(400).send({ error: "Username too long" });
    }

    // 2. Secure response with Content Security Policy headers
    const securityHeaders = {
        'Content-Security-Policy': "default-src 'self'; script-src 'none'; style-src 'self' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com;",
        'X-Content-Type-Options': 'nosniff',
        'X-XSS-Protection': '1; mode=block'
    };

    res.set(securityHeaders);

    // 3. Return a secure, HTML-encoded response
    const safeResponse = `
        <!DOCTYPE html>
        <html>
            <head>
                <meta charset="UTF-8">
                <title>Hello Page</title>
            </head>
            <body>
                <h1>Hello, ${sanitizedUsername}!</h1>
                <p>Welcome to our secure application.</p>
            </body>
        </html>
    `;

    res.send(safeResponse);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
