const express = require("express");
const yaml = require("js-yaml");
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

const app = express();

// ...existing code...
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// parse JSON bodies (prefer body over query for credentials)
app.use(express.json());

/**
 * Load moderator list from environment or a local YAML file.
 * Expected format (YAML or JSON):
 * - username: alice
 *   password_sha256: "<hex sha256 of password>"
 *
 * No hardcoded plaintext credentials in source.
 */
function loadModerators() {
  // try environment variable first (MODERATORS_YAML or MODERATORS_JSON)
  const envYaml = process.env.MODERATORS_YAML;
  const envJson = process.env.MODERATORS_JSON;

  try {
    if (envYaml) {
      const parsed = yaml.load(envYaml);
      if (Array.isArray(parsed)) return parsed;
    }
    if (envJson) {
      const parsed = JSON.parse(envJson);
      if (Array.isArray(parsed)) return parsed;
    }
  } catch (_) {
    // fallthrough to file
  }

  // fallback to file moderators.yml in same directory
  try {
    const filePath = path.join(__dirname, "moderators.yml");
    if (fs.existsSync(filePath)) {
      const content = fs.readFileSync(filePath, "utf8");
      const parsed = yaml.load(content);
      if (Array.isArray(parsed)) return parsed;
    }
  } catch (_) {
    // ignore and return empty
  }

  return []; // default: no moderators configured
}

const MODERATORS = loadModerators();

/**
 * return true if they are a moderator
 * Accepts JSON body { username, password } (preferable) or query params.
 * Compares password by computing SHA-256 and using constant-time compare
 * against stored password_sha256. No plaintext credentials are stored in source.
 */
app.post("/check_mod", (req, res) => {
    const username = (req.body && req.body.username) || req.query.username;
    const password = (req.body && req.body.password) || req.query.password;

    if (!username || !password) {
        return res.status(400).json({ error: "username and password required" });
    }

    // find moderator entry by username
    const modEntry = MODERATORS.find((m) => String(m.username) === String(username));
    if (!modEntry || !modEntry.password_sha256) {
        return res.json({ isModerator: false });
    }

    try {
        const providedHashHex = crypto.createHash("sha256").update(String(password), "utf8").digest("hex");
        const a = Buffer.from(providedHashHex, "hex");
        const b = Buffer.from(String(modEntry.password_sha256).replace(/^0x/, ""), "hex");

        // lengths must match for timingSafeEqual; deny if they don't
        if (a.length !== b.length) {
            return res.json({ isModerator: false });
        }

        const isEqual = crypto.timingSafeEqual(a, b);
        return res.json({ isModerator: isEqual });
    } catch (err) {
        return res.status(500).json({ error: "internal error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
