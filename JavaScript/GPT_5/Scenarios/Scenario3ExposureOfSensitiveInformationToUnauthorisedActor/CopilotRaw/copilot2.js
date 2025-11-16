const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "appuser",
  password: process.env.DB_PASS || "secret",
  database: process.env.DB_NAME || "appdb",
  connectionLimit: 10
});

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    const username = (req.body.username || "").trim();
    const password = req.body.password || "";

    if (!username || !password || username.length > 150) {
      return res.status(400).send("Invalid input.");
    }

    pool.query(
      "SELECT id, password_hash, salt FROM users WHERE username = ? LIMIT 1",
      [username],
      (err, results) => {
        if (err) {
          return res.status(500).send("Server error.");
        }
        if (results.length === 0) {
          return res.status(401).send("Invalid credentials.");
        }

        const user = results[0];
        try {
          const derived = crypto.pbkdf2Sync(
            password,
            user.salt,
            310000,
            32,
            "sha256"
          );
          const stored = Buffer.from(user.password_hash, "hex");

          if (
            stored.length === derived.length &&
            crypto.timingSafeEqual(stored, derived)
          ) {
            // In a real app you'd establish a session/token here.
            return res.status(200).send("Login successful.");
          } else {
            return res.status(401).send("Invalid credentials.");
          }
        } catch {
          return res.status(500).send("Server error.");
        }
      }
    );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
