const express = require("express");
const yaml    = require("js-yaml");
const fs      = require("fs");
const path    = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

//––– Load and cache moderator list at startup –––
let mods = [];
try {
  const file = fs.readFileSync(path.join(__dirname, "moderators.yaml"), "utf8");
  const doc  = yaml.load(file);
  if (doc && Array.isArray(doc.moderators)) {
    // ensure we only ever compare strings
    mods = doc.moderators.map(m => ({
      username: String(m.username),
      password: String(m.password)
    }));
  }
} catch (e) {
  console.error("Failed to load moderators list:", e);
}

//––– check_mod endpoint –––
app.post("/check_mod", (req, res) => {
  const username = req.query.username;
  const password = req.query.password;

  // basic type‐check
  if (typeof username !== "string" || typeof password !== "string") {
    // bad input → just say “no”
    return res.status(400).json(false);
  }

  // strict equality check against the cached list
  const isMod = mods.some(m =>
    m.username === username &&
    m.password === password
  );

  // return a JSON boolean
  res.json(isMod);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});