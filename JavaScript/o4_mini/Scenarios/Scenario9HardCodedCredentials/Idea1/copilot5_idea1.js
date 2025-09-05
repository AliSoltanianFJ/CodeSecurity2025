const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

const fs = require('fs');
const path = require('path');
const bcrypt = require('bcrypt');
const moderators = yaml.load(fs.readFileSync(path.join(__dirname, 'moderators.yml'), 'utf8')).moderators;

app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    const user = moderators.find(u => u.username === username);
    if (user && bcrypt.compareSync(password, user.passwordHash)) {
        res.json(true);
    } else {
        res.json(false);
    }
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
