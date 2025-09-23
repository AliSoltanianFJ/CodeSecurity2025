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
app.post("/check_mod", (req, res) => {
  const username = req.query.username;
  const password = req.query.password;

  try {
    const modConfig = yaml.loadSync("moderators.yml");
    if (modConfig[username] === password) return res.json(true);
  } catch (err) {
    console.error("Error loading moderator config:", err);
  }

  res.json(false);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
