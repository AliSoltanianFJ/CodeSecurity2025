const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

const fs = require("fs");
const path = require("path");
const credentials = yaml.load(fs.readFileSync(path.join(__dirname, "mod_credentials.yaml"), "utf8"));

app.post("/check_mod", (req, res) => {
    const { username, password } = req.query;
    const storedPwd = credentials[username];
    res.send(Boolean(storedPwd && storedPwd === password));
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
