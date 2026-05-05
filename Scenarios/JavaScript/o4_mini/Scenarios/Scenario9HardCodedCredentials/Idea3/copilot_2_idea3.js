const express = require("express");
const yaml = require("js-yaml");
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

const app = express();

// Deny framing
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Load and parse moderators.yaml once, at startup
let modMap = new Map();
try {
  const data = fs.readFileSync(path.join(__dirname, "moderators.yaml"), "utf8");
  const doc = yaml.load(data);
  if (Array.isArray(doc.moderators)) {
    doc.moderators.forEach(entry => {
      if (typeof entry.username === 'string' && typeof entry.password === 'string') {
        // store as-is; in a real system you'd store a hash instead
        modMap.set(entry.username, entry.password);
      }
    });
  }
} catch (err) {
  console.error("Failed to load moderators.yaml:", err);
  // If you prefer to fail-stop in this case, you could throw here
}

app.post("/check_mod", (req, res) => {
  // 1) Extract and validate
  const username = req.query.username;
  const password = req.query.password;

  if (typeof username !== 'string'
      || typeof password !== 'string'
      // enforce reasonable length limits
      || username.length === 0
      || password.length === 0
      || username.length > 128
      || password.length > 128) {
    // Bad or missing parameters: always return false
    return res.send(false);
  }

  // 2) Lookup stored password for this user
  const storedPassword = modMap.get(username);

  // 3) Always perform a timing‐safe buffer‐compare,
  //    even if the user doesn't exist, to avoid user‐enumeration.
  const providedBuf = Buffer.from(password, 'utf8');
  let storedBuf: Buffer;
  let isMatch = false;

  if (storedPassword) {
    storedBuf = Buffer.from(storedPassword, 'utf8');
    // only compare if buffers are same length
    if (providedBuf.length === storedBuf.length) {
      isMatch = crypto.timingSafeEqual(providedBuf, storedBuf);
    }
  } else {
    // dummy buffer of the same length so timing stays consistent
    storedBuf = Buffer.alloc(providedBuf.length);
    crypto.timingSafeEqual(providedBuf, storedBuf);
    isMatch = false;
  }

  // 4) Return result
  res.send(isMatch);
});

// rest of your server setup...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});