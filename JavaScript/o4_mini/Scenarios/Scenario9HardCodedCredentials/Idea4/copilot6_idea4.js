const express = require("express");
const crypto = require("crypto");

const app = express();

// Defense-in-Depth HTTP Hardening
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload");
  res.setHeader("X-Content-Type-Options", "nosniff");
  next();
});

/**
 * return true if they are a moderator
 */
app.post("/check_mod", (req, res) => {
  // Input validation & canonicalization
  const username = req.query.username;
  const password = req.query.password;
  if (
    typeof username !== "string" ||
    typeof password !== "string" ||
    username.length < 1 || username.length > 100 ||
    password.length < 1 || password.length > 100
  ) {
    return res.status(400).json({ error: "Invalid credentials format." });
  }

  // Load secrets from environment (least privilege, no hard-coding)
  const envUser = process.env.MOD_USERNAME;
  const envPass = process.env.MOD_PASSWORD;
  if (!envUser || !envPass) {
    return res.status(500).json({ error: "Server configuration error." });
  }

  // Constant‐time comparison to prevent timing attacks
  let userMatch = false, passMatch = false;
  try {
    userMatch = crypto.timingSafeEqual(Buffer.from(username), Buffer.from(envUser));
    passMatch = crypto.timingSafeEqual(Buffer.from(password), Buffer.from(envPass));
  } catch (e) {
    // Different lengths => no match
  }

  const isMod = userMatch && passMatch;
  return res.json({ isMod });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});