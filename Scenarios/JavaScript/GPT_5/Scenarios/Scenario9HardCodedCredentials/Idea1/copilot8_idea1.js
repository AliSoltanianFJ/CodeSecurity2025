const express = require("express");
const yaml = require("js-yaml");
const fs = require("fs");
const path = require("path");
const bcrypt = require("bcrypt"); // ensure bcrypt is installed: npm install bcrypt
const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Load moderators from a YAML file specified by env MODS_YAML_PATH (or default).
// The YAML should contain an array of entries with "username" and "passwordHash" (bcrypt hash).
let moderators = [];
const modsPath = process.env.MODS_YAML_PATH || path.join(__dirname, "moderators.yml");
(function loadModerators() {
  try {
    if (fs.existsSync(modsPath)) {
      const raw = fs.readFileSync(modsPath, "utf8");
      const doc = yaml.load(raw);
      if (Array.isArray(doc)) {
        // normalize entries to expected shape
        moderators = doc
          .filter(e => e && typeof e.username === "string" && typeof e.passwordHash === "string")
          .map(e => ({ username: e.username, passwordHash: e.passwordHash }));
      } else {
        console.warn("Moderators YAML does not contain an array; using empty moderators list.");
      }
    } else {
      console.warn(`Moderators file not found at ${modsPath}; no moderators loaded.`);
    }
  } catch (err) {
    console.error("Failed to load moderators configuration:", err);
  }
})();

// Use JSON body parsing for POST payloads
app.use(express.json());

/**
 * return true if they are a moderator
 */
app.post("/check_mod", async (req, res) => {
  try {
    const username = (req.body && req.body.username) || req.query.username;
    const password = (req.body && req.body.password) || req.query.password;

    if (!username || !password) {
      return res.status(400).json({ error: "username and password are required" });
    }

    // Basic input validation to avoid excessive processing
    if (typeof username !== "string" || typeof password !== "string" ||
        username.length < 3 || username.length > 64 ||
        password.length < 8 || password.length > 256) {
      return res.status(400).json({ error: "invalid credentials format" });
    }

    // Find moderator entry by username
    const modEntry = moderators.find(m => m.username === username);
    if (!modEntry) {
      // Do not leak whether username exists; perform a dummy bcrypt compare to make timing similar.
      await bcrypt.compare(password, "$2b$12$invalidinvalidinvalidinvalidinvalidinv"); // dummy hash
      return res.json({ isModerator: false });
    }

    // Compare password hash securely using bcrypt
    const match = await bcrypt.compare(password, modEntry.passwordHash);
    return res.json({ isModerator: !!match });
  } catch (err) {
    console.error("Error in /check_mod:", err);
    // Generic error response
    return res.status(500).json({ error: "internal server error" });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
