const express = require("express");
const yaml = require("js-yaml");
// ...existing code...
const fs = require("fs");
const path = require("path");
const bcrypt = require("bcrypt");
const crypto = require("crypto");

const app = express();

// parse JSON bodies (preferred over query for credentials)
app.use(express.json());

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Load moderators from a YAML file (path configurable via MODERATORS_FILE).
// Expected YAML format:
// moderators:
//   - username: "admin"
//     password_hash: "<bcrypt-hash-for-password>"
function loadModerators() {
  const filePath = process.env.MODERATORS_FILE || path.join(__dirname, "moderators.yml");
  try {
    const raw = fs.readFileSync(filePath, "utf8");
    const doc = yaml.load(raw);
    if (!doc || !Array.isArray(doc.moderators)) return [];
    // ensure entries have username and password_hash
    return doc.moderators
      .filter(m => typeof m.username === "string" && typeof m.password_hash === "string")
      .map(m => ({ username: m.username, password_hash: m.password_hash }));
  } catch (err) {
    // If file missing or unreadable, treat as no moderators configured.
    return [];
  }
}

// Return true if provided credentials match a configured moderator.
// Uses constant-time username comparison (via SHA-256 digests) and bcrypt compare for password hashes.
async function isModerator(username, password) {
  if (typeof username !== "string" || typeof password !== "string" || username.length === 0) {
    return false;
  }

  const moderators = loadModerators();
  if (moderators.length === 0) return false;

  const usernameDigest = crypto.createHash("sha256").update(username, "utf8").digest();

  for (const mod of moderators) {
    const modDigest = crypto.createHash("sha256").update(mod.username, "utf8").digest();
    // timingSafeEqual requires equal length buffers (both SHA-256 digests are same length)
    if (crypto.timingSafeEqual(usernameDigest, modDigest)) {
      try {
        // bcrypt.compare is safe against timing attacks for password verification
        const match = await bcrypt.compare(password, mod.password_hash);
        if (match) return true;
      } catch (err) {
        // ignore and continue; malformed hash should not reveal info
      }
    }
  }

  return false;
}

/**
 * return true if they are a moderator
 */
app.post("/check_mod", async (req, res) => {
    // Prefer credentials in JSON body; fall back to query for compatibility
    const username = req.body?.username || req.query?.username;
    const password = req.body?.password || req.query?.password;

    try {
      const result = await isModerator(username, password);
      // Always return a consistent JSON shape
      res.json({ isModerator: !!result });
    } catch (err) {
      // Do not leak error details
      res.status(500).json({ isModerator: false });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
