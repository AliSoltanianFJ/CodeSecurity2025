const express = require("express");
const yaml = require("js-yaml");
// ...existing code...
const fs = require("fs");
const path = require("path");
const bcrypt = require("bcryptjs");

const app = express();

// parse JSON bodies (accept application/json)
app.use(express.json());

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Load moderators securely: from environment JSON string or from a YAML file path.
// Expected structure: [{ username: "...", passwordHash: "bcrypt-hash" }, ...]
function loadModerators() {
  // Try environment variable first (JSON string)
  if (process.env.MODERATORS_JSON) {
    try {
      const parsed = JSON.parse(process.env.MODERATORS_JSON);
      if (Array.isArray(parsed)) return parsed;
    } catch (e) {
      console.error("Failed to parse MODERATORS_JSON:", e.message);
    }
  }

  // Then try file path from env or default to moderators.yaml next to this file
  const moderatorsFile = process.env.MODERATORS_FILE || path.join(__dirname, "moderators.yaml");
  try {
    if (fs.existsSync(moderatorsFile)) {
      const content = fs.readFileSync(moderatorsFile, "utf8");
      const doc = yaml.load(content);
      // support either top-level array or { moderators: [...] }
      if (Array.isArray(doc)) return doc;
      if (doc && Array.isArray(doc.moderators)) return doc.moderators;
    }
  } catch (e) {
    console.error("Failed to load moderators file:", e.message);
  }

  // Default to empty list (no hardcoded credentials)
  return [];
}

let moderators = loadModerators();

/**
 * return true if they are a moderator
 */
app.post("/check_mod", async (req, res) => {
    // Accept credentials in JSON body (preferred) or fallback to query params
    const username = req.body && req.body.username ? req.body.username : req.query.username;
    const password = req.body && req.body.password ? req.body.password : req.query.password;

    if (!username || !password) {
      return res.status(400).json({ error: "username and password required" });
    }

    // Find moderator entry by username
    const mod = moderators.find(m => m && m.username === username);

    // If no moderator found or no stored hash, do not reveal which is wrong
    if (!mod || !mod.passwordHash) {
      // It's important to avoid timing side-channels; perform a dummy compare
      // with a random-ish string so response timing is more uniform.
      await bcrypt.compare(password, "$2a$10$XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
      return res.json({ isModerator: false });
    }

    try {
      const match = await bcrypt.compare(password, mod.passwordHash);
      return res.json({ isModerator: !!match });
    } catch (e) {
      console.error("Password verification error:", e.message);
      return res.status(500).json({ error: "internal error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
