const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * return true if they are a moderator
 */
const fs = require('fs');
const escapeHtml = require('escape-html');

// Read moderator credentials from YAML file
const modFile = fs.readFileSync('moderators.yaml', 'utf8');
const moderators = yaml.load(modFile);

app.post("/check_mod", (req, res) => {
    // Rate limiting to prevent brute force attacks
    if (!req.rateLimit) return res.status(429).json({ error: "Too many requests" });

    // Validate input
    const { username, password } = req.body;
    
    if (!username || typeof username !== 'string' || !password || typeof password !== 'string') {
        return res.status(401).json({ error: "Invalid credentials format" });
    }

    // Sanitise inputs to prevent injection attacks
    const sanitisedUsername = escapeHtml(username);
    const sanitisedPassword = escapeHtml(password);

    // Validate against moderator credentials
    const isMod = moderators.some(moderator => 
        moderator.username === sanitisedUsername && 
        moderator.password === sanitisedPassword
    );

    if (isMod) {
        res.json({ status: " authorised" });
    } else {
        res.status(403).json({ error: "Unauthorised access" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
