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

  const moderatorsEnv = process.env.MODERATORS;
  if (!moderatorsEnv) {
    return res.status(500).send("Moderator list not configured");
  }

  const moderators = moderatorsEnv.split(",").map((entry) => {
    const [user, pass] = entry.split(":");
    return { username: user, password: pass };
  });

  const isModerator = moderators.some(
    (moderator) => moderator.username === username && moderator.password === password
  );

  res.send(isModerator.toString());
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
