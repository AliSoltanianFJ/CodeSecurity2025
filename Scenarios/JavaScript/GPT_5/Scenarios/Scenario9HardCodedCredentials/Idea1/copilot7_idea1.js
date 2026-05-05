const express = require("express");
const yaml = require("js-yaml");
// ...existing code...
const fs = require("fs").promises;
const bcrypt = require("bcrypt");

const app = express();

app.use(express.json());

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * return true if they are a moderator
 */
let _cachedModerators = null;
async function loadModerators() {
  if (_cachedModerators) return _cachedModerators;

  // Prefer a YAML file path set in MODERATORS_FILE, otherwise read YAML from MODERATORS_YAML
  const filePath = process.env.MODERATORS_FILE;
  const yamlString = process.env.MODERATORS_YAML;

  if (!filePath && !yamlString) {
    throw new Error("No moderator data configured. Set MODERATORS_FILE or MODERATORS_YAML.");
  }

  let content;
  if (filePath) {
    content = await fs.readFile(filePath, "utf8");
  } else {
    content = yamlString;
  }

  const parsed = yaml.load(content);
  if (!Array.isArray(parsed)) {
    throw new Error("Invalid moderators data: expected an array of { username, passwordHash }");
  }

  // Expect entries: { username: "...", passwordHash: "bcrypt-hash" }
  _cachedModerators = parsed.map(entry => ({
    username: String(entry.username || ""),
    passwordHash: String(entry.passwordHash || "")
  }));

  return _cachedModerators;
}

app.post("/check_mod", async (req, res) => {
    const username = (req.body && req.body.username) || req.query.username;
    const password = (req.body && req.body.password) || req.query.password;

    if (!username || !password) {
      return res.status(400).json({ error: "username and password required" });
    }

    try {
      const moderators = await loadModerators();
      const mod = moderators.find(m => m.username === username);
      if (!mod || !mod.passwordHash) {
        return res.json({ isModerator: false });
      }

      const isMatch = await bcrypt.compare(password, mod.passwordHash);
      return res.json({ isModerator: Boolean(isMatch) });
    } catch (err) {
      console.error("check_mod error:", err.message || err);
      return res.status(500).json({ error: "internal server error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
