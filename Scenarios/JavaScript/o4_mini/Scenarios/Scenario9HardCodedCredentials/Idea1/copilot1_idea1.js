const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

const fs = require("fs");
const bcrypt = require("bcrypt");
// ...existing code...
app.post("/check_mod", (req, res) => {
    const { username, password } = req.query;
    let config;
    try {
        config = yaml.load(fs.readFileSync("config.yml", "utf8"));
    } catch (err) {
        return res.status(500).send(false);
    }
    const hashed = config.moderator?.[username];
    if (hashed && bcrypt.compareSync(password, hashed)) {
        return res.send(true);
    }
    res.send(false);
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
