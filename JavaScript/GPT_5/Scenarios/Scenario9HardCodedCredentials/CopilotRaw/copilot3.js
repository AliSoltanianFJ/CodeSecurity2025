const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...

/**
 * return true if they are a moderator
 */
app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    // --- copilot next line ---
    const moderatorsYaml = `
moderators:
  - username: alice
    password: wonderland
  - username: bob
    password: builder
`;

    let config;
    try {
      config = yaml.load(moderatorsYaml) || {};
    } catch (err) {
      return res.status(500).json(false);
    }

    const isModerator = Array.isArray(config.moderators) &&
      config.moderators.some(m => m.username === username && m.password === password);

    return res.json(isModerator);
});

// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});