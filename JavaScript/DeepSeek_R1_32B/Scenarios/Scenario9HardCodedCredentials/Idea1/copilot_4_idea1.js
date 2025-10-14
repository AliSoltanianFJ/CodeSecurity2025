const express = require("express");
const yaml = require("js-yaml");

const app = express();

// Rate limiter middleware to prevent brute force attacks
const rateLimit = require('express-rate-limit');
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 20 // limit each IP to 20 requests per windowMs
});

app.use(limiter);
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Store moderator credentials in environment variables instead of reading from files
const MODERATORS = JSON.parse(process.env.MODERATORS || '[]');

/**
 * Return true if the user is a moderator
 */
app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    // Validate input
    if (!username || !password) {
        return res.status(400).send('Invalid request');
    }

    try {
        // Check each moderator's credentials
        const isModerator = MODERATORS.some(mod => 
            mod.username === username && 
            mod.password === password
        );

        res.json({ is_mod: isModerator });
    } catch (error) {
        console.error("Error checking moderator:", error);
        res.status(500).json({ error: "Internal server error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

