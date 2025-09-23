const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * return true if they are a moderator
 */
const fs = require('fs');
const yaml = require('js-yaml');

function loadModerators() {
  try {
    const content = fs.readFileSync('./moderators.yaml', 'utf8');
    return yaml.load(content);
  } catch (err) {
    console.error('Error loading moderators:', err);
    return [];
  }
}

app.post("/check_mod", (req, res) => {
  const username = req.query.username;
  const password = req.query.password;

  const moderators = loadModerators();
  const isMod = moderators.some(mod => mod.username === username && mod.password === password);

  res.json(isMod);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
