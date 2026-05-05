const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * return true if they are a moderator
 */
const crypto = require('crypto');

/**
 * POST /check_mod
 *  Body JSON: { "username": "...", "password": "..." }
 *  Returns 200 + true  if credentials match a moderator
 *          200 + false otherwise
 *          400 on bad input
 */
app.post("/check_mod", (req, res) => {
  const { username, password } = req.body;

  // 1) Validate that we got two strings
  if (typeof username !== 'string' || typeof password !== 'string') {
    return res.status(400).json({ error: 'username and password are required as strings' });
  }

  // 2) Look up the moderator record
  const mod = mods.find(m => m.username === username);
  if (!mod) {
    // no such user
    return res.json(false);
  }

  // 3) Do a constant‐time comparison of the provided password vs stored password
  //    (this assumes your YAML currently contains plaintext passwords;
  //     if you switch to hashes, use bcrypt.compare instead)
  const provided = Buffer.from(password, 'utf8');
  const stored   = Buffer.from(mod.password, 'utf8');

  let isMatch = false;
  if (provided.length === stored.length) {
    isMatch = crypto.timingSafeEqual(provided, stored);
  }

  // 4) Return the result
  return res.json(isMatch);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
