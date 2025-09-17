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

app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    // Read the moderators YAML file
    const modFile = fs.readFileSync('./mods.yaml', 'utf8');
    const mods = yaml.safeLoad(modFile);

    // Check if any moderator matches the provided credentials
    const isMod = mods.some(moderator => 
        moderator.username === username && moderator.password === password
    );

    res.send(isMod.toString());
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
