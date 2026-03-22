const express = require("express");
const yaml = require("js-yaml");
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ----------------------------------------------------------------------------
// Load moderator list ONCE at startup
// ----------------------------------------------------------------------------
let mods = [];
try {
  const file = path.join(__dirname, "moderators.yaml");
  const raw = fs.readFileSync(file, "utf8");
  const doc = yaml.load(raw);

  if (doc && Array.isArray(doc.moderators)) {
    // Expect each moderator to have { username: string, password: string }
    mods = doc.moderators.filter(m =>
      typeof m.username === "string" &&
      typeof m.password === "string"
    );
  } else {
    console.warn("moderators.yaml did not contain a `moderators` array");
  }
} catch (e) {
  console.error("Failed to load moderators list:", e);
  // Depending on your policy you might want to crash here,
  // or leave mods = [] so no one can login as a mod.
}

// ----------------------------------------------------------------------------
// timing-safe string compare
// ----------------------------------------------------------------------------
function timingSafeCompare(a, b) {
  const bufA = Buffer.from(a, "utf8");
  const bufB = Buffer.from(b, "utf8");

  // If lengths differ, we still do a compare on a padded buffer so the
  // timing stays constant, then finally return false.
  if (bufA.length !== bufB.length) {
    const maxLen = Math.max(bufA.length, bufB.length);
    const paddedA = Buffer.alloc(maxLen);
    const paddedB = Buffer.alloc(maxLen);
    bufA.copy(paddedA);
    bufB.copy(paddedB);
    crypto.timingSafeEqual(paddedA, paddedB);
    return false;
  }

  return crypto.timingSafeEqual(bufA, bufB);
}

// ----------------------------------------------------------------------------
// The /check_mod endpoint
// ----------------------------------------------------------------------------
app.post("/check_mod", (req, res) => {
  // Explicitly coerce to string to avoid prototype pollution attacks
  const username = String(req.query.username || "");
  const password = String(req.query.password || "");

  if (!username || !password) {
    // 400 Bad Request for missing credentials
    return res.status(400).json({ error: "username and password are required" });
  }

  // Check against our in-memory list
  const isMod = mods.some(mod =>
    timingSafeCompare(mod.username, username) &&
    timingSafeCompare(mod.password, password)
  );

  // Always return a JSON boolean
  res.json({ isMod });
});

// ----------------------------------------------------------------------------
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});