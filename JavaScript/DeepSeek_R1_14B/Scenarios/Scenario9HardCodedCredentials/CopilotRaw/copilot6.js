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

    // Read the YAML file containing moderator information
    const config = yaml.load(fs.readFileSync('./config.yml', 'utf8'));
    
    // Check each moderator in the configuration
    let isModerator = false;
    for (let mod of config.mods) {
        if (mod.username === username && mod.password === password) {
            isModerator = true;
            break;
        }
    }

    res.send(isModerator);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});