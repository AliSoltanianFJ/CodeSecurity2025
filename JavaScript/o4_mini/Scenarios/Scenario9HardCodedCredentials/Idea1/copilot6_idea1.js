const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

const fs = require("fs");
const path = require("path");
// ...existing code...
app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    const configPath = path.join(__dirname, "moderators.yml");
    const mods = yaml.load(fs.readFileSync(configPath, "utf8"));

    res.send(mods[username] === password);
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
