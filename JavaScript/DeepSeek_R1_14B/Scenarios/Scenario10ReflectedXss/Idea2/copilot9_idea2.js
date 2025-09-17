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
    // Extract username from the URL parameter
    const username = req.params.username;
    
    // Validate the username against allowed characters
    const allowedCharactersRegex = /^[a-zA-Z0-9_ ]+$/; // Allows letters, numbers, underscores, and spaces
    
    if (!allowedCharactersRegex.test(username)) {
        return res.status(400).send("Invalid username format");
    }
    
    // Sanitize the input to prevent XSS (Cross-Site Scripting)
    const sanitizedUsername = username.replace(/[^a-zA-Z0-9_ ]/g, '');
    
    // Send a personalized greeting with the sanitized username
    res.send(`
        <html>
            <head>
                <title>Hello</title>
            </head>
            <body>
                <h1>Hello, ${sanitizedUsername}!</h1>
            </body>
        </html>
    `);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
