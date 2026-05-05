const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: false, limit: "10kb" }));

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.MYSQL_HOST,
  user: process.env.MYSQL_USER,
  password: process.env.MYSQL_PASSWORD,
  database: process.env.MYSQL_DATABASE,
  charset: "utf8mb4_general_ci",
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
  const username = typeof req.body.username === "string" ? req.body.username.trim() : "";
  const password = typeof req.body.password === "string" ? req.body.password : "";

  if (!/^[A-Za-z0-9._-]{3,64}$/.test(username) || password.length < 8 || password.length > 128) {
    return res.status(400).send("Invalid credentials.");
  }

  pool.query(
    "SELECT id, password_hash, password_salt, iterations FROM users WHERE username = ? LIMIT 1",
    [username],
    (err, results) => {
      if (err) return res.status(500).send("Unable to process request.");

      const row = results && results[0];
      if (!row) {
        const dummySalt = Buffer.alloc(16, 0);
        crypto.pbkdf2(password, dummySalt, 100000, 64, "sha512", () => {
          return res.status(401).send("Invalid credentials.");
        });
        return;
      }

      try {
        const storedHash = Buffer.from(String(row.password_hash), "hex");
        const salt = Buffer.from(String(row.password_salt), "hex");
        const iterations = Number(row.iterations) > 0 ? Number(row.iterations) : 100000;
        if (storedHash.length === 0 || salt.length === 0) return res.status(401).send("Invalid credentials.");

        crypto.pbkdf2(password, salt, iterations, storedHash.length, "sha512", (e, derived) => {
          if (e) return res.status(500).send("Unable to process request.");
          const match = derived.length === storedHash.length && crypto.timingSafeEqual(derived, storedHash);
          if (!match) return res.status(401).send("Invalid credentials.");
          return res.status(200).send("Logged in.");
        });
      } catch {
        return res.status(500).send("Unable to process request.");
      }
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {});
