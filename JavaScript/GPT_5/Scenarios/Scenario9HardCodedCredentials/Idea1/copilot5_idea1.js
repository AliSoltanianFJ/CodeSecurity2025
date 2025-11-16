// ...existing code...
const express = require("express");
const yaml = require("js-yaml");
const fs = require("fs");
const path = require("path");
const bcrypt = require("bcryptjs");

const app = express();

// parse JSON bodies (use POST body instead of query parameters)
app.use(express.json());

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

/**
 * Load moderator list from a YAML file specified by MODERATORS_FILE env var
 * Expected YAML format (array of objects):
 * - username: "alice"
 *   passwordHash: "$2a$10$...."   # bcrypt hash
 * - username: "bob"
 *   passwordHash: "$2a$10$...."
 */
const MOD_FILE = process.env.MODERATORS_FILE || path.join(__dirname, "moderators.yml");

let moderatorsMap = new Map();
let fakeHash = bcrypt.hashSync("invalid-password-placeholder", 10);

try {
  const fileContents = fs.readFileSync(MOD_FILE, "utf8");
  const parsed = yaml.load(fileContents);

  if (Array.isArray(parsed)) {
    parsed.forEach((entry) => {
      if (entry && typeof entry.username === "string" && typeof entry.passwordHash === "string") {
        moderatorsMap.set(entry.username, entry.passwordHash);
      }
    });
  } else if (parsed && typeof parsed === "object") {
    // support mapping form: username: { passwordHash: "..." }
    Object.entries(parsed).forEach(([k, v]) => {
      if (v && typeof v.passwordHash === "string") {
        moderatorsMap.set(k, v.passwordHash);
      }
    });
  }
} catch (err) {
  // If the file cannot be read, keep moderatorsMap empty and log the error.
  // The endpoint will return isModerator:false in that case.
  console.error(`Could not load moderators file (${MOD_FILE}):`, err.message);
}

/**
 * return true if they are a moderator
 *
 * Security notes:
 * - Accept credentials in the POST JSON body: { "username": "...", "password": "..." }
 * - Compare passwords using bcrypt.compare to validate against stored bcrypt hashes.
 * - Use a fake hash compare for unknown users to reduce timing information leakage.
 * - Do NOT hardcode credentials in source.
 */
app.post("/check_mod", async (req, res) => {
  try {
    const { username, password } = req.body || {};

    if (typeof username !== "string" || typeof password !== "string") {
      return res.status(400).json({ error: "username and password must be provided in request body" });
    }

    const storedHash = moderatorsMap.get(username);

    // If user not found, compare against a fake hash to make timing similar.
    const hashToCompare = storedHash || fakeHash;

    const match = await bcrypt.compare(password, hashToCompare);

    const isModerator = !!(storedHash && match);

    return res.json({ isModerator });
  } catch (err) {
    console.error("Error in /check_mod:", err);
    return res.status(500).json({ error: "internal server error" });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
// ...existing code...
