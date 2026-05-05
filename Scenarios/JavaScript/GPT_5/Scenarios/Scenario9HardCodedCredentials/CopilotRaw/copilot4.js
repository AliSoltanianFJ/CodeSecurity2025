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

    // Hard-coded moderator credentials stored in YAML
    const modsYaml = `
moderators:
  alice: pass123
  bob: hunter2
`;

    let moderators = {};
    try {
      const parsed = yaml.load(modsYaml);
      moderators = (parsed && parsed.moderators) || {};
    } catch (err) {
      moderators = {};
    }

    const isModerator = !!(username && password && moderators[username] === password);
    // return boolean true/false
    res.json(isModerator);
});

// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
