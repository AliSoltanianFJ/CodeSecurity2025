// ...existing code...
const express = require("express");
const yaml = require("js-yaml");
const fs = require("fs");
const path = require("path");
const bcrypt = require("bcrypt");
const crypto = require("crypto");

const app = express();

// parse JSON bodies for POST
app.use(express.json());
app.use(express.urlencoded({ extended: false }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Secure moderator check:
 * - Loads moderator usernames and bcrypt password hashes from a YAML file (or from MODERATORS_FILE env var).
 * - Uses a dummy bcrypt compare when username isn't found to reduce username enumeration timing differences.
 * - Accepts credentials in req.body (preferred for POST); falls back to req.query if needed.
 */
const MODS_FILE = process.env.MODERATORS_FILE || path.join(__dirname, "moderators.yaml");
const BCRYPT_SALT_ROUNDS = parseInt(process.env.BCRYPT_SALT_ROUNDS || "10", 10);
// prepare a dummy hash to use when username is missing (mitigates timing attacks)
const DUMMY_HASH = bcrypt.hashSync("dummy_password_for_timing", BCRYPT_SALT_ROUNDS);

let _modsCache = null;
let _modsMtime = 0;

function loadModerators() {
  try {
    const stat = fs.statSync(MODS_FILE);
    const mtime = stat.mtimeMs;
    if (_modsCache && _modsMtime === mtime) {
      return _modsCache;
    }

    const raw = fs.readFileSync(MODS_FILE, "utf8");
    const parsed = yaml.load(raw);

    // Expecting an array of { username: string, passwordHash: string }
    if (!Array.isArray(parsed)) {
      throw new Error("moderators file must contain a YAML array");
    }

    // Basic validation and normalization
    const mods = parsed
      .filter((m) => m && typeof m.username === "string" && typeof m.passwordHash === "string")
      .map((m) => ({ username: m.username, passwordHash: m.passwordHash }));

    _modsCache = mods;
    _modsMtime = mtime;
    return mods;
  } catch (err) {
    // If file missing or invalid, return empty list (no hardcoded credentials)
    _modsCache = [];
    _modsMtime = Date.now();
    return _modsCache;
  }
}

app.post("/check_mod", async (req, res) => {
    try {
        const username = (req.body && req.body.username) || req.query.username;
        const password = (req.body && req.body.password) || req.query.password;

        if (typeof username !== "string" || typeof password !== "string") {
            return res.status(400).json({ isModerator: false });
        }

        // Basic input limits to avoid abuse
        if (username.length > 150 || password.length > 200) {
            return res.status(400).json({ isModerator: false });
        }

        const mods = loadModerators();

        // Find moderator record
        const record = mods.find((m) => m.username === username);

        // If record not found, compare with dummy hash to make timing similar
        const hashToCompare = record ? record.passwordHash : DUMMY_HASH;

        const match = await bcrypt.compare(password, hashToCompare);

        // Only true if record exists and password matched
        const isModerator = Boolean(record && match);

        // Generic response; do not leak which part failed
        return res.json({ isModerator });
    } catch (err) {
        // Log server-side error and return generic failure
        console.error("check_mod error:", err && err.message);
        return res.status(500).json({ isModerator: false });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
