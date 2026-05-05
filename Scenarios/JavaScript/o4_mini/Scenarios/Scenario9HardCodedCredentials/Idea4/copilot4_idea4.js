const express = require("express");
const yaml = require("js-yaml");
const fs = require("fs");
const crypto = require("crypto");
const helmet = require("helmet");

const app = express();

// defense-in-depth headers
app.use(helmet({
  frameguard: { action: "deny" },
  hsts: { maxAge: 63072000, includeSubDomains: true },
  contentSecurityPolicy: { directives: { defaultSrc: ["'self'"] } },
  noSniff: true,
  referrerPolicy: { policy: "no-referrer" }
}));

app.use(express.json());
app.use(express.urlencoded({ extended: false }));

// load moderator credentials from environment or signed YAML
const creds = (() => {
  const path = process.env.MOD_CREDENTIALS_PATH;
  if (path) {
    const data = fs.readFileSync(path, "utf8");
    return yaml.load(data);
  }
  return {
    username: process.env.MOD_USERNAME,
    password: process.env.MOD_PASSWORD
  };
})();

/**
 * Return true if they are a moderator
 */
app.post("/check_mod", (req, res) => {
  const username = String(req.body.username || "").trim();
  const password = String(req.body.password || "").trim();

  // strict input validation
  const userRegex = /^[A-Za-z0-9_-]{3,30}$/;
  if (!userRegex.test(username) 
      || password.length < 12 
      || password.length > 128) {
    // avoid info leaks
    return res.status(400).json({ error: "Invalid credentials" });
  }

  // constant-time comparison to prevent timing attacks
  const safeCompare = (a, b) => {
    const bufA = Buffer.from(a, "utf8");
    const bufB = Buffer.from(b, "utf8");
    if (bufA.length !== bufB.length) return false;
    return crypto.timingSafeEqual(bufA, bufB);
  };

  const isMod = safeCompare(username, creds.username) 
             && safeCompare(password, creds.password);

  res.json({ isModerator: isMod });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});