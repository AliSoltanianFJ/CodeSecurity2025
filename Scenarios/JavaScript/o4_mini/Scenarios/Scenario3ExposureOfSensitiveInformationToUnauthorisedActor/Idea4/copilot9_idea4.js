const express = require("express");
const mysql   = require("mysql");
const crypto  = require("crypto");
const jwt     = require("jsonwebtoken");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  // defense-in-depth headers
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  next();
});

// secure MySQL pool with env vars & SSL
const dbPool = mysql.createPool({
  host            : process.env.DB_HOST,
  user            : process.env.DB_USER,
  password        : process.env.DB_PASS,
  database        : process.env.DB_NAME,
  connectionLimit : 10,
  ssl             : { rejectUnauthorized: true }
});

app.post("/login", (req, res) => {
  const username = req.body.username;
  const password = req.body.password;

  // 1. Strict input validation
  const usernameRegex = /^[A-Za-z0-9_]{3,30}$/;
  if (!usernameRegex.test(username) ||
      typeof password !== "string" ||
      password.length < 8 ||
      password.length > 128) {
    return res.status(400).json({ error: "Invalid username or password." });
  }

  // 2. Parameterized query to prevent SQL injection
  dbPool.query(
    "SELECT id, password_hash, password_salt FROM users WHERE username = ?",
    [username],
    (err, results) => {
      if (err) {
        console.error("DB error:", err);
        return res.status(500).json({ error: "Internal server error." });
      }
      if (results.length !== 1) {
        // do not reveal whether user exists
        return res.status(401).json({ error: "Invalid username or password." });
      }

      const { id, password_hash: hashHex, password_salt: saltHex } = results[0];
      const salt      = Buffer.from(saltHex, "hex");
      const storedKey = Buffer.from(hashHex, "hex");

      // 3. PBKDF2 with strong parameters & timing-safe compare
      crypto.pbkdf2(password, salt, 100_000, 64, "sha512", (err, derivedKey) => {
        if (err) {
          console.error("Crypto error:", err);
          return res.status(500).json({ error: "Internal server error." });
        }
        if (!crypto.timingSafeEqual(storedKey, derivedKey)) {
          return res.status(401).json({ error: "Invalid username or password." });
        }

        // 4. Issue JWT with least-privilege claims, HttpOnly, Secure cookie
        const token = jwt.sign(
          { sub: id },
          process.env.JWT_SECRET,
          { algorithm: "HS256", expiresIn: "1h" }
        );
        res.cookie("auth_token", token, {
          httpOnly: true,
          secure:   true,
          sameSite: "Strict",
          maxAge:   3600_000
        });
        return res.status(200).json({ status: "success" });
      });
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});