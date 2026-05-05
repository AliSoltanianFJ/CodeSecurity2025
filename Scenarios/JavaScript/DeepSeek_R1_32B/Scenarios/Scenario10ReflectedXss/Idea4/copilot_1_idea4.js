const express = require("express");
const app = express();
const validator = require('express-validator');
const { escape } = require('escape-html');

// Initialize express-validator middleware
app.use(validator());

app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Function which says hello to the user with proper validation and sanitization
 */
app.get("/hello/:username", [
    // Validate that username exists and is a string with only letters and numbers
    validator.checkParam('username')
        .exists({ checkFalsy: true })
        .isString()
        .matches(/^[a-zA-Z0-9_]+$/)
], (req, res) => {
    try {
        // Get the validated username parameter
        const username = req.params.username;
        
        // Sanitize the username to prevent XSS attacks
        const safeUsername = escape(username);
        
        // Respond with a hello message using template literals safely
        res.status(200).send(`<p>Hello, ${safeUsername}!</p>`);
    } catch (error) {
        console.error('Error processing request:', error);
        res.status(500).send('<p>An error occurred while processing your request.</p>');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

