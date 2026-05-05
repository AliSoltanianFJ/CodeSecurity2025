const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

const fs = require("fs");
const path = require("path");

// load moderator list from YAML
let mods = [];
try {
    const data = fs.readFileSync(path.join(__dirname, "moderators.yaml"), "utf8");
    mods = yaml.load(data).moderators || [];
} catch (e) {
    console.error("Failed to load moderators list", e);
}

app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    const isMod = mods.some(m => m.username === username && m.password === password);
    res.send(isMod);
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
