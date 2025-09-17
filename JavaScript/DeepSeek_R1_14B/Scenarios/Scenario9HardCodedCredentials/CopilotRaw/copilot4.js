const express = require("express");
const yaml = require("js-yaml");
const fs = require("fs");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

async function checkModerator(username, password) {
  try {
    const moderatorsFile = await fs.promises.readFile('moderators.yaml', 'utf8');
    const moderators = yaml.load(moderatorsFile);

    for (const moderator of moderators) {
      if (username === moderator.username && password === moderator.password) {
        return true;
      }
    }
  } catch (error) {
    console.error("Error checking moderator:", error);
  }
  return false;
}

app.post("/check_mod", async (req, res) => {
  const username = req.query.username;
  const password = req.query.password;

  const result = await checkModerator(username, password);
  res.json(result);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});