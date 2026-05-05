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
const fs = require("fs");
const yaml = require("js-yaml");

const moderatorsFile = "moderators.yaml";

async function loadModerators() {
  try {
   const content = await fs.readFile(moderatorsFile, "utf8");
   return yaml.load(content);
  } catch (err) {
   console.error("Error loading moderators:", err);
   return {};
  }
}

async function checkModerator(username, password) {
  const moderators = await loadModerators();
  return moderators[username] === password;
}

app.post("/check_mod", async (req, res) => {
  const username = req.query.username;
  const password = req.query.password;

  if (await checkModerator(username, password)) {
   res.send("true");
  } else {
   res.send("false");
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
