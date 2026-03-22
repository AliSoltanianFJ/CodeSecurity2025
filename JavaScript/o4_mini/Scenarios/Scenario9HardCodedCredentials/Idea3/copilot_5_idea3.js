const express = require("express");
const yaml = require("js-yaml");
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

const app = express();

// we’ll accept JSON or urlencoded bodies for a POST
app.use(express.json());
app.use(express.urlencoded({ extended: false }));

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// --- START: load moderator list once at startup ---
let mods = [];
try {
  const data = fs.readFileSync(
    path.join(__dirname, "moderators.yaml"),
    "utf8"
  );
  const doc = yaml.load(data);
  if (doc && Array.isArray(doc.moderators)) {
    // ensure we only keep simple strings
    mods = doc.moderators
      .filter(m => typeof m.username === "string" && typeof m.password === "string")
      .map(m => ({
        username: m.username.trim(),
        password: m.password
      }));
  } else {
    console.warn("moderators.yaml did not contain a `moderators` array");
  }
} catch (e) {
  console.error("Failed to load moderators list:", e);
}
// --- END: load moderator list ---

/**
 * Return true if the user is a moderator.
 * We use a constant‐time comparison for passwords to avoid
 * leaking password‐length/timing information.
 */
app.post("/check_mod", (req, res) => {
  const username = (req.body.username || "").trim();
  const password = req.body.password || "";

  // Basic input validation
  if (!username || !password) {
    // 400 Bad Request but do not leak “which” field was wrong
    return res.status(400).json({ isMod: false });
  }

  let isMod = false;

  // Find the moderator entry with a matching username
  for (const mod of mods) {
    if (mod.username === username) {
      // convert both to buffers
      const pwBuf = Buffer.from(password, "utf8");
      const storedBuf = Buffer.from(mod.password, "utf8");

      // only compare if lengths match
      if (pwBuf.length === storedBuf.length) {
        if (crypto.timingSafeEqual(pwBuf, storedBuf)) {
          isMod = true;
        }
      }
      break; // we found the username; no need to keep looping
    }
  }

  // return a simple boolean response
  res.json({ isMod });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});