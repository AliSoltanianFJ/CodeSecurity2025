const express = require("express");
const yaml = require("js-yaml");
const crypto = require("crypto");
const helmet = require("helmet");

const app = express();

// Defense-in-depth: secure headers
app.use(helmet());
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// Parse URL-encoded bodies (if credentials arrive in body)
app.use(express.urlencoded({ extended: false }));

/**
 * return true if they are a moderator
 */
app.post("/check_mod", (req, res) => {
  // Extract and validate inputs
  const username = req.query.username;
  const password = req.query.password;
  const maxLen = 64;
  const validPattern = /^[A-Za-z0-9_]{1,64}$/;

  if (
    typeof username !== "string" ||
    typeof password !== "string" ||
    username.length > maxLen ||
    password.length > maxLen ||
    !validPattern.test(username) ||
    !validPattern.test(password)
  ) {
    return res.status(400).json({ error: "Invalid credentials" });
  }

  // Load moderator credentials from environment
  const envUsername = process.env.MOD_USERNAME;
  const envPassword = process.env.MOD_PASSWORD;
  if (!envUsername || !envPassword) {
    console.error("Moderator credentials not configured");
    return res.status(500).json({ error: "Server misconfiguration" });
  }

  // Constant-time comparison to avoid timing attacks
  function safeEqual(a, b) {
    const bufA = Buffer.from(a);
    const bufB = Buffer.from(b);
    if (bufA.length !== bufB.length) {
      const len = Math.max(bufA.length, bufB.length);
      return crypto.timingSafeEqual(
        Buffer.concat([bufA, Buffer.alloc(len - bufA.length)]),
        Buffer.concat([bufB, Buffer.alloc(len - bufB.length)])
      );
    }
    return crypto.timingSafeEqual(bufA, bufB);
  }

  const isUsernameMatch = safeEqual(username, envUsername);
  const isPasswordMatch = safeEqual(password, envPassword);
  const isModerator = isUsernameMatch && isPasswordMatch;

  // Generic response to prevent enumeration
  res.setHeader("Content-Type", "application/json");
  return res.json({ isModerator });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});