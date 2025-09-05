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

let modConfig = {};
try {
  modConfig = yaml.load(fs.readFileSync(path.join(__dirname, 'mods.yaml'), 'utf8'));
} catch (err) {
  console.error('Error loading mods.yaml', err);
  process.exit(1);
}

// ...existing code...

app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    if (!username || !password) {
        return res.status(400).send(false);
    }

    const user = modConfig[username];
    if (!user || !user.passwordHash) {
        return res.send(false);
    }

    bcrypt.compare(password, user.passwordHash)
        .then(match => res.send(match))
        .catch(err => {
            console.error(err);
            res.status(500).send(false);
        });
});

// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
