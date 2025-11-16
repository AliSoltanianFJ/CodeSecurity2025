const express = require("express");
const yaml = require("js-yaml");
const fs = require("fs");
const path = require("path");
const bcrypt = require("bcryptjs");

const app = express();

// ...existing code...
app.use(express.json()); // parse JSON body
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});
// ...existing code...

/**
 * return true if they are a moderator
 *
 * Security notes:
 * - Moderators and their password hashes must be supplied via a YAML blob in the
 *   environment variable `MODERATORS_YAML` or a file named `moderators.yml`
 *   placed next to this script. Each entry should be an object with
 *   { username: "name", passwordHash: "$2a$..." } where passwordHash is a bcrypt hash.
 * - No plaintext or hard-coded passwords are used here. If a username is not found,
 *   we still perform a bcrypt compare against a fake hash to make timing similar.
 */
app.post("/check_mod", async (req, res) => {
  try {
    const username = (req.body && req.body.username) || "";
    const password = (req.body && req.body.password) || "";

    if (typeof username !== "string" || typeof password !== "string") {
      return res.status(400).json({ isModerator: false });
    }

    // Load moderator list (cached)
    const moderators = loadModerators(); // Map username -> passwordHash

    // Fake hash used to mitigate timing when username is not found
    const fakeHash = getFakeHash();

    const storedHash = moderators.get(username);

    // Always run bcrypt.compare to avoid revealing existence via timing
    const hashToCompare = storedHash || fakeHash;
    const passwordMatches = await bcrypt.compare(password, hashToCompare);

    const isModerator = Boolean(storedHash && passwordMatches);

    return res.json({ isModerator });
  } catch (err) {
    // Don't leak internal errors to caller
    return res.status(500).json({ isModerator: false });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});

// --- Helper functions ---

/**
 * Load moderators from either an environment variable (MODERATORS_YAML)
 * or a file named moderators.yml next to this script.
 * Expects a YAML array of objects: [{ username: "u", passwordHash: "..." }, ...]
 * Returns a Map of username -> passwordHash
 */
function loadModerators() {
  if (loadModerators._cache) return loadModerators._cache;

  let raw;
  if (process.env.MODERATORS_YAML && process.env.MODERATORS_YAML.trim()) {
    raw = process.env.MODERATORS_YAML;
  } else {
    const filePath = path.join(__dirname, "moderators.yml");
    if (!fs.existsSync(filePath)) {
      // No moderators configured
      loadModerators._cache = new Map();
      return loadModerators._cache;
    }
    raw = fs.readFileSync(filePath, "utf8");
  }

  let parsed;
  try {
    parsed = yaml.load(raw);
  } catch (e) {
    parsed = null;
  }

  // Accept either an array at top-level or an object with a 'moderators' key
  let list = [];
  if (Array.isArray(parsed)) {
    list = parsed;
  } else if (parsed && Array.isArray(parsed.moderators)) {
    list = parsed.moderators;
  }

  const map = new Map();
  for (const item of list) {
    if (
      item &&
      typeof item.username === "string" &&
      typeof item.passwordHash === "string"
    ) {
      map.set(item.username, item.passwordHash);
    }
  }

  loadModerators._cache = map;
  return map;
}

/**
 * Return a reproducible fake bcrypt hash to use for timing-equal comparisons
 */
function getFakeHash() {
  if (getFakeHash._fake) return getFakeHash._fake;
  // Pre-compute a bcrypt hash of a constant secret; cost kept moderate.
  getFakeHash._fake = bcrypt.hashSync("change-this-placeholder", 10);
  return getFakeHash._fake;
}
