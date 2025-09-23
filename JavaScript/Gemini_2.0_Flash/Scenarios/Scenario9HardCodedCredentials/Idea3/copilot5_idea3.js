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

function loadConfigSync(filePath) {
  try {
    const fileContent = fs.readFileSync(filePath, "utf8");
    return yaml.load(fileContent);
  } catch (error) {
    console.error("Error loading config:", error);
    throw new Error("Failed to load moderator list");
  }
}

app.post("/check_mod", (req, res) => {
  const username = req.query.username;
  const password = req.query.password;

  let moderators = [];
  try {
    moderators = loadConfigSync("moderators.yaml");
  } catch (error) {
    console.error("Error loading moderator list:", error);
    return res.status(500).send("Failed to load moderators");
  }

  const isModerator = moderators.some(mod => mod.username === username && mod.password === password);

  res.send(isModerator.toString());
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
