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
    // Load moderators from YAML file
    const moderatorsYAML = yaml.loadSync("moderators.yml");
    const moderators = moderatorsYAML.moderators;

    // Check if username exists and password matches
    for (const moderator of moderators) {
      if (username === moderator.username && password === moderator.password) {
        return res.json({ isModerator: true });
      }
    }
  } catch (error) {
    console.error("Error loading moderators:", error);
    return res.status(500).json({ error: "Internal server error" });
  }

  res.json({ isModerator: false });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
