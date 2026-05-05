const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

const fs = require("fs");
const path = require("path");
const credentials = yaml.load(fs.readFileSync(path.join(__dirname, "credentials.yml"), "utf8"));

// ...existing code...

app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    if (!username || !password) {
        return res.send(false);
    }

    const moderator = credentials.moderators.find(mod => mod.username === username);
    if (moderator && moderator.password === password) {
        return res.send(true);
    }
    res.send(false);
});

// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
