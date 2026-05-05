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
const moderatorsYAML = process.env.MODERATORS || '';
const moderators = yaml.safeLoad(moderatorsYAML) || [];

app.post('/check_mod', limiter, (req, res) => {
  const username = req.query.username;
  const password = req.query.password;
  const isModerator = moderators.some(mod => mod.username === username && mod.password === password);
  res.json(isModerator);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
