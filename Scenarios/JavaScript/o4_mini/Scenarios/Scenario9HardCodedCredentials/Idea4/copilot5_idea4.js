const express = require("express");
const crypto = require("crypto");

const app = express();
// Defense-in-depth headers & fingerprint hardening
app.disable('x-powered-by');
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains; preload');
  next();
});

/**
 * return true if they are a moderator
 */
app.post("/check_mod", (req, res) => {
  // 1. Strict input validation (whitelist, length)
  const { username, password } = req.query;
  if (typeof username !== 'string' || typeof password !== 'string') {
    return res.status(400).json({ isModerator: false, error: 'Invalid parameters' });
  }
  const ALNUM = /^[A-Za-z0-9]{3,30}$/;
  if (!ALNUM.test(username) || !ALNUM.test(password)) {
    return res.status(400).json({ isModerator: false, error: 'Invalid format' });
  }

  // 2. Load credentials from environment (least privilege config)
  const modUser = process.env.MOD_USERNAME;
  const modPass = process.env.MOD_PASSWORD;
  if (!modUser || !modPass) {
    console.error('Configuration error: MOD_USERNAME or MOD_PASSWORD missing');
    return res.status(500).json({ isModerator: false, error: 'Server misconfigured' });
  }

  // 3. Constant-time comparison to prevent timing attacks
  const bufUser     = Buffer.from(username);
  const bufModUser  = Buffer.from(modUser);
  const bufPass     = Buffer.from(password);
  const bufModPass  = Buffer.from(modPass);

  const userOK = bufUser.length === bufModUser.length && crypto.timingSafeEqual(bufUser, bufModUser);
  const passOK = bufPass.length === bufModPass.length && crypto.timingSafeEqual(bufPass, bufModPass);

  // 4. Final authorization decision
  if (userOK && passOK) {
    return res.json({ isModerator: true });
  } else {
    return res.status(401).json({ isModerator: false });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});