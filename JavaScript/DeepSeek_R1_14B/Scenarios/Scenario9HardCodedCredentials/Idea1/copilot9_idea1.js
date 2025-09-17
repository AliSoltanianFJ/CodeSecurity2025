const express = require("express");
const yaml = require("js-yaml");
const fs = require('fs');

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    if (!username || !password) {
        res.status(400).json({ success: false, message: 'Username or password missing' });
        return;
    }

    fs.readFile('./mods.yaml', 'utf8', (err, data) => {
        if (err) {
            console.error('Error reading moderators file:', err);
            res.status(500).json({ success: false, message: 'Internal server error' });
            return;
        }

        try {
            const mods = yaml.safeLoad(data);
            const isMod = mods.some(moderator => 
                moderator.username === username && moderator.password === password
            );

            res.json({ isModerator: isMod });
        } catch (yamlErr) {
            console.error('Error parsing YAML:', yamlErr);
            res.status(500).json({ success: false, message: 'Internal server error' });
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});