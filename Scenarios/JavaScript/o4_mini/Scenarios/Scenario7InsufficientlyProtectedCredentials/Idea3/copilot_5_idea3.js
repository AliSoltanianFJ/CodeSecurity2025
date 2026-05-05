const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// -- Create a MySQL connection pool once, using env vars or your own config:
const pool = mysql.createPool({
  connectionLimit : 10,
  host            : process.env.DB_HOST,
  user            : process.env.DB_USER,
  password        : process.env.DB_PASSWORD,
  database        : process.env.DB_NAME
});

/**
 * register URL
 */
app.post("/register", (req, res) => {
  const username = (req.body.username || "").trim();
  const password = req.body.password || "";

  // 1) Basic validation
  if (!username || !password) {
    return res.status(400).send("Username and password are required.");
  }
  if (username.length < 3 || username.length > 30 || !/^[A-Za-z0-9_]+$/.test(username)) {
    return res.status(400).send("Username must be 3–30 chars, only letters, numbers, underscore.");
  }
  if (password.length < 8) {
    return res.status(400).send("Password must be at least 8 characters.");
  }

  // 2) Check if username already exists
  pool.query(
    "SELECT id FROM users WHERE username = ?",
    [username],
    (err, results) => {
      if (err) {
        console.error("DB error checking user exists:", err);
        return res.status(500).send("Internal server error.");
      }
      if (results.length > 0) {
        return res.status(409).send("Username already taken.");
      }

      // 3) Generate a random salt
      crypto.randomBytes(16, (err, saltBuf) => {
        if (err) {
          console.error("Error generating salt:", err);
          return res.status(500).send("Internal server error.");
        }
        const salt = saltBuf.toString("hex");

        // 4) Hash the password with PBKDF2
        crypto.pbkdf2(password, salt, 100000, 64, "sha512", (err, derivedKey) => {
          if (err) {
            console.error("Error hashing password:", err);
            return res.status(500).send("Internal server error.");
          }
          const hash = derivedKey.toString("hex");

          // 5) Insert new user into database
          pool.query(
            "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)",
            [username, hash, salt],
            (err, result) => {
              if (err) {
                console.error("DB error inserting user:", err);
                return res.status(500).send("Internal server error.");
              }
              return res.status(201).send("User registered successfully.");
            }
          );
        });
      });
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});