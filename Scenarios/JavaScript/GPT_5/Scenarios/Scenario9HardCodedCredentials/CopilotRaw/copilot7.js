// ...existing code...
const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Simple hard-coded moderator credentials (for demo only — do NOT use in production)
const MODERATORS = {
  "alice": "alice_password_123",
  "bob": "bob_secure_pw"
};

/**
 * return true if they are a moderator
 */
app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    // basic validation
    if (!username || !password) {
      return res.status(400).json(false);
    }

    const isMod = Boolean(MODERATORS[username] && MODERATORS[username] === password);
    return res.json(isMod);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
