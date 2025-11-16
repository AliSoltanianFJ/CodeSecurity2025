const express = require("express");
const yaml = require("js-yaml");
// ...existing code...
const fs = require("fs");
const path = require("path");
const bcrypt = require("bcryptjs");
const rateLimit = require("express-rate-limit");

const app = express();

app.use(express.json()); // accept JSON body for credentials

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Basic rate limiting for this endpoint to mitigate brute-force attempts
const checkModLimiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute window
  max: 10,             // limit to 10 requests per window per IP
  standardHeaders: true,
  legacyHeaders: false,
});

// Load moderators from external YAML config (should contain username + bcrypt hash)
// Example moderators.yaml:
// - username: "admin"
//   password_hash: "$2a$10$...."
const MODS_FILE = path.join(__dirname, "moderators.yaml");
let moderators = [];
try {
  if (fs.existsSync(MODS_FILE)) {
    const file = fs.readFileSync(MODS_FILE, "utf8");
    const parsed = yaml.load(file);
    if (Array.isArray(parsed)) moderators = parsed;
    else if (parsed && Array.isArray(parsed.moderators)) moderators = parsed.moderators;
  } else {
    console.warn("Moderators file not found:", MODS_FILE);
  }
} catch (err) {
  console.error("Failed to load moderators file:", err);
}

/**
 * return true if they are a moderator
 */
app.post("/check_mod", checkModLimiter, async (req, res) => {
    const username = req.body?.username;
    const password = req.body?.password;

    if (!username || !password) {
      return res.status(400).json({ error: "username and password are required" });
    }

    // Find moderator entry by username
    const mod = moderators.find(m => m.username === username);
    if (!mod || !mod.password_hash) {
      // Do not reveal whether the username exists
      return res.json({ isModerator: false });
    }

    try {
      const match = await bcrypt.compare(String(password), String(mod.password_hash));
      return res.json({ isModerator: !!match });
    } catch (err) {
      console.error("Error verifying password:", err);
      return res.status(500).json({ error: "internal error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
