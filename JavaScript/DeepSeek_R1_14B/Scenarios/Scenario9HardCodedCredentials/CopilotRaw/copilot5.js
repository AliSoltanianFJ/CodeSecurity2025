const express = require("express");
const yaml = require("js-yaml");
const fs = require('fs');

// Load moderator configuration from YAML file
let config;
try {
    const configContent = fs.readFileSync('./config.yml', 'utf8');
    config = yaml.safeLoad(configContent);
} catch (error) {
    console.error('Error loading configuration:', error);
    process.exit(1); // Exit if config can't be loaded
}

const app = express();

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

/**
 * Return true if the user is a moderator based on credentials from config.yml
 */
app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    // Check if provided credentials match the stored moderator's credentials
    if (username === config.moderatorUsername && password === config.moderatorPassword) {
        res.json({ moderator: true });
    } else {
        res.json({ moderator: false });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});