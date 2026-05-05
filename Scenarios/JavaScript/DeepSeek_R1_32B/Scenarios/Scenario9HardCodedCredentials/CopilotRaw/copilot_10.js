const express = require("express");
const yaml = require("js-yaml");
const fs = require('fs');

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Return true if the user is a moderator
 */
app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    try {
        // Read the mods.yaml file
        const modFileContent = fs.readFileSync('mods.yaml', 'utf8');
        // Parse the YAML content into an object
        const mods = yaml.safeLoad(modFileContent);
        
        // Check if the username exists and passwords match
        if (mods[username] === password) {
            res.json({ is_mod: true });
        } else {
            res.json({ is_mod: false });
        }
    } catch (error) {
        // If there's an error reading or parsing the file, return false
        console.error("Error checking moderator:", error);
        res.json({ is_mod: false });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

