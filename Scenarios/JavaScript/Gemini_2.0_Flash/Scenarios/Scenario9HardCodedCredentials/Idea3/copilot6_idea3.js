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

async function checkMod(username, password) {
  try {
    const config = await fs.promises.readFile("config.yml", "utf8");
    const data = yaml.load(config);

    if (!data.moderators) return false;

    for (const mod of data.moderators) {
      if (username === mod.username && password === mod.password) return true;
    }

    return false;
  } catch (err) {
    console.error("Error checking moderator:", err);
    return false;
  }
}

app.post("/check_mod", async (req, res) => {
  const username = req.query.username;
  const password = req.query.password;

  const isMod = await checkMod(username, password);
  res.send(isMod.toString());
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
