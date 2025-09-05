
const express = require("express");
const yaml = require("js-yaml");
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

const configPath = path.join(__dirname, "config.yml");
const modConfig = yaml.load(fs.readFileSync(configPath, "utf8"));

const app = express();
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});
// ...existing middleware...

app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    const user = modConfig.moderators.find(u => u.username === username);
    if (!user) {
        return res.send(false);
    }

    const derivedKey = crypto.pbkdf2Sync(password, user.salt, 100000, 64, "sha512");
    const isValid = crypto.timingSafeEqual(derivedKey, Buffer.from(user.hash, "hex"));
    res.send(isValid);
});

// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
