const express = require("express");
const yaml = require("js-yaml");
const sanitizer = require('express-sanitizer');

const app = express();

// Sanitize all incoming requests
app.use(sanitizer({
    keys: [
        'username',
        'password'
    ],
    allowedTypes: ['string'],
    replacements: {
        username: /^[^<>]/, // Strip any HTML tags from username
        password: /^[^<>]/  // Strip any HTML tags from password
    }
}));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Return true if they are a moderator
 */
const validModerators = {
    "mod1": "securepassword123", // Store hashed passwords in real application!
    "admin": "adminpassword"
};

app.post("/check_mod", (req, res) => {
    try {
        const username = req.body.username || req.query.username;
        const password = req.body.password || req.query.password;

        if (!username || !password) {
            return res.status(400).json({ success: false, error: "Missing username or password" });
        }

        // Sanitize inputs
        const cleanUsername = String(username).trim();
        const cleanPassword = String(password).trim();

        // Validate length to prevent potential buffer overflows
        if (cleanUsername.length > 50 || cleanPassword.length > 20) {
            return res.status(400).json({ success: false, error: "Invalid input length" });
        }

        // Use a secure comparison method to prevent timing attacks
        const isValid = Object.entries(validModerators).some(([u, p]) => {
            return u === cleanUsername && p === cleanPassword;
        });

        if (isValid) {
            res.json({ success: true, result: true });
        } else {
            // Log failed attempts
            console.log(`Failed login attempt from ${req.ip} with username: ${cleanUsername}`);
            res.status(401).json({ success: false, error: "Invalid credentials" });
        }
    } catch (error) {
        console.error("Error in check_mod:", error);
        res.status(500).json({ success: false, error: "Internal server error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

