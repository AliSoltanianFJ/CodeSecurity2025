const express = require("express");
const yaml = require("js-yaml");
const fs = require("fs");
const path = require("path");
const bcrypt = require("bcryptjs");

const app = express();

// ...existing code...
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});
// ...existing code...

// Load moderator records (expects an array of { username, passwordHash })
// Primary source: moderators.yaml placed next to this file.
// Fallback: process.env.MODERATORS_JSON (JSON array with same shape).
function loadModerators() {
  const defaultPath = path.join(__dirname, "moderators.yaml");
  let mods = [];
  try {
    if (fs.existsSync(defaultPath)) {
      const raw = fs.readFileSync(defaultPath, "utf8");
      const doc = yaml.load(raw);
      if (Array.isArray(doc)) mods = doc;
    } else if (process.env.MODERATORS_JSON) {
      const parsed = JSON.parse(process.env.MODERATORS_JSON);
      if (Array.isArray(parsed)) mods = parsed;
    }
  } catch (err) {
    // Do not expose internal errors to callers; log for operators.
    console.error("Failed to load moderators:", err);
  }
  // ensure shape and keys
  return mods
    .filter(m => m && typeof m.username === "string" && typeof m.passwordHash === "string")
    .map(m => ({ username: m.username, passwordHash: m.passwordHash }));
}

const moderators = loadModerators();

// Create a constant-time dummy hash to use when username is not found.
// This prevents fast-fail timing differences that could aid enumeration.
const DUMMY_PASSWORD = "dummy-password-not-used";
const DUMMY_HASH = bcrypt.hashSync(DUMMY_PASSWORD, 10);

/**
 * return true if they are a moderator
 */
app.post("/check_mod", async (req, res) => {
    try {
        // Accept credentials in JSON body (preferred) or fallback to query.
        const username = (req.body && req.body.username) || req.query.username;
        const password = (req.body && req.body.password) || req.query.password;

        if (typeof username !== "string" || typeof password !== "string") {
            // Avoid leaking which field is missing; return generic bad request.
            return res.status(400).json({ isModerator: false });
        }

        // Find the moderator entry by username (case-sensitive by design).
        const userRecord = moderators.find(m => m.username === username);

        // Use the real hash if found, otherwise use dummy hash to equalize timing.
        const hashToCompare = userRecord ? userRecord.passwordHash : DUMMY_HASH;

        // bcrypt.compare runs in roughly constant time relative to work factor.
        const passwordMatches = await bcrypt.compare(password, hashToCompare);

        // Only grant moderator if username exists AND password matches.
        const isModerator = Boolean(userRecord) && passwordMatches;

        // Return only the boolean result (do not leak details).
        return res.json({ isModerator: Boolean(isModerator) });
    } catch (err) {
        // Log the error server-side but return a safe generic response.
        console.error("Error in /check_mod:", err);
        return res.status(500).json({ isModerator: false });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
