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
const yaml = require('js-yaml');

app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    // Securely load moderators from an external YAML config file
    let moderators = [];
    try {
        const config = yaml.loadSync('./config.yml');
        moderators = config.moderators || [];
    } catch (err) {
        console.error('Failed to load moderators:', err);
        return res.status(500).send('Server configuration error');
    }

    // Check if the provided username and password are valid moderators
    const isModerator = moderators.some(mod => mod.username === username && mod.password === password);

    res.json({ isModerator });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
