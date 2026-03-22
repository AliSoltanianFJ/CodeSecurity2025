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

let moderators = [];
try {
  const file = path.join(__dirname, "moderators.yaml");
  const raw = fs.readFileSync(file, "utf8");
  const doc = yaml.load(raw);

  // Expect each moderator to have { username: string, password: string }
  if (doc && Array.isArray(doc.moderators)) {
    moderators = doc.moderators.filter(m =>
      typeof m.username === "string" &&
      typeof m.password === "string"
    );
  } else {
    console.warn("moderators.yaml did not contain an array of moderators");
  }
} catch (e) {
  console.error("Could not load moderators.yaml:", e);
}

// ----------------------------------------------------------------------------
// timing-safe string compare
// ----------------------------------------------------------------------------
function timingSafeCompare(a, b) {
  const bufferA = Buffer.from(a, "utf8");
  const bufferB = Buffer.from(b, "utf8");

  if (bufferA.length !== bufferB.length) {
    const maxLen = Math.max(bufferA.length, bufferB.length);
    const paddedA = Buffer.alloc(maxLen);
    const paddedB = Buffer.alloc(maxLen);
    bufferA.copy(paddedA);
    bufferB.copy(paddedB);
    crypto.timingSafeEqual(paddedA, paddedB);
    return false;
  }

  return crypto.timingSafeEqual(bufferA, bufferB);
}

// ----------------------------------------------------------------------------
// The /check_mod endpoint
// ----------------------------------------------------------------------------
app.post("/check_mod", (req, res) => {
  const user = String(req.query.username || "");
  const pass = String(req.query.password || "");

  if (!user || !pass) {
    // 400 Bad Request for missing credentials
    return res.status(400).json({ error: "username and password are required" });
  }

  // Check the provided credentials against the loaded moderator list using a timing-safe comparison
  const isMod = moderators.some(mod =>
    timingSafeCompare(mod.username, user) &&
    timingSafeCompare(mod.password, pass)
  );

  res.json({ isMod });
});

// ----------------------------------------------------------------------------
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});