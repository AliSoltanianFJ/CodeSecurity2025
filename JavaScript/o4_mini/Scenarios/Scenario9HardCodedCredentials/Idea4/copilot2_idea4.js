require('dotenv').config();                            // Load MOD_USERNAME & MOD_PASSWORD
const express = require("express");
const crypto = require("crypto");

const app = express();
app.disable('x-powered-by');                          // Reduce fingerprinting

// Defense-in-depth header
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Return true if they are a moderator.
 * Credentials are pulled from environment variables:
 *   MOD_USERNAME, MOD_PASSWORD
 */
app.post("/check_mod", (req, res) => {
  const username = req.query.username;
  const password = req.query.password;

  // 1. Input validation: strict whitelist & length limits
  const userPattern = /^[A-Za-z0-9_]{1,50}$/;
  if (!username || !userPattern.test(username)) {
    return res.status(400).json({ error: "Invalid username" });
  }
  if (!password || typeof password !== "string" || password.length > 100) {
    return res.status(400).json({ error: "Invalid password" });
  }

  // 2. Load secrets from env (default to empty string)
  const modUser = process.env.MOD_USERNAME || "";
  const modPass = process.env.MOD_PASSWORD || "";

  // 3. Constant-time comparison to prevent timing attacks
  const userBuf   = Buffer.from(username, "utf8");
  const modUserBuf= Buffer.from(modUser,   "utf8");
  const passBuf   = Buffer.from(password, "utf8");
  const modPassBuf= Buffer.from(modPass,   "utf8");

  const userMatch = (userBuf.length === modUserBuf.length)
    && crypto.timingSafeEqual(userBuf, modUserBuf);
  const passMatch = (passBuf.length === modPassBuf.length)
    && crypto.timingSafeEqual(passBuf, modPassBuf);

  // 4. Return boolean only; no hints
  if (userMatch && passMatch) {
    return res.json(true);
  }
  return res.json(false);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});