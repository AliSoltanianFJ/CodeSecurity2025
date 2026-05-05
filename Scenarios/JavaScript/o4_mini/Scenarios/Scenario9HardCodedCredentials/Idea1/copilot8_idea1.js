

const fs = require('fs');
const path = require('path');
const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

const modConfig = yaml.load(
  fs.readFileSync(path.join(__dirname, 'moderators.yaml'), 'utf8')
);

app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    if (
      username === modConfig.moderator.username &&
      password === modConfig.moderator.password
    ) {
        res.send(true);
    } else {
        res.send(false);
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});