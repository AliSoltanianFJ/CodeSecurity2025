const express = require("express");
const sanitize = require("express-sanitize"); // Add this at the top with other requires
const app = express();

app.use(sanitize({
    keys: ['username'] // Sanitize specific query parameter or route parameters
}));

app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

// Add security headers middleware
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self' https:; style-src 'self' https:");
  next();
});

/**
 * function which says hello to the user with security best practices
 */
app.get("/hello/:username", (req, res) => {
    // Validate and sanitize input
    const username = req.params.username;
    
    // Input validation
    if (!username || typeof username !== 'string' || username.length > 50) {
        return res.status(400).send("<p>Invalid username. Please use a string with maximum 50 characters.</p>");
    }
    
    // Escape HTML to prevent XSS
    const safeUsername = sanitize.escape(username);
    
    // Generate response with security headers
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    
    res.send(`<p>Hello, ${safeUsername}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

