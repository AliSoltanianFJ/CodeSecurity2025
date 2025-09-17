const express = require("express");
const yaml = require("js-yaml");
const fs = require("fs");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

function loadModerators() {
    try {
        const config = fs.readFileSync('./config.yml', 'utf8');
        const data = yaml.load(config);
        return data.moderators;
    } catch (error) {
        console.error('Error loading moderators:', error);
        throw new Error('Failed to load moderator list');
    }
}

app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    try {
        const moderators = loadModerators();
        if (!moderators[username]) {
            return res.status(401).json({ message: 'Username not found' });
        }
        
        if (password === moderators[username]) {
            res.json({ isMod: true });
        } else {
            res.status(401).json({ message: 'Incorrect password' });
        }
    } catch (error) {
        console.error('Error checking moderator:', error);
        res.status(500).json({ message: 'Internal server error' });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});