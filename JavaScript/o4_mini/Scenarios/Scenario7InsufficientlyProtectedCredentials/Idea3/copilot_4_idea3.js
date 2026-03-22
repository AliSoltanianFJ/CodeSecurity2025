const express = require("express");
const mysql   = require("mysql");
const crypto  = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

// Prevent clickjacking
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create a MySQL connection pool
const pool = mysql.createPool({
  host            : process.env.DB_HOST,     // e.g. 'localhost'
  user            : process.env.DB_USER,     // e.g. 'my_user'
  password        : process.env.DB_PASSWORD, // e.g. 's3cret'
  database        : process.env.DB_NAME,     // e.g. 'my_app'
  connectionLimit : 10
});

/**
 * register URL
 */
app.post("/register", (req, res) => {
  const username = (req.body.username || "").trim();
  const password = (req.body.password || "");

  // 1) Basic input validation
  if (!username || !password) {
    return res.status(400).send("Username and password are required.");
  }

  // Username: 3–30 chars, letters, numbers and underscore only
  if (!/^[A-Za-z0-9_]{3,30}$/.test(username)) {
    return res.status(400).send("Username must be 3–30 characters and contain only letters, numbers, or underscores.");
  }

  // Password length check (you can add stronger rules here)
  if (password.length < 8) {
    return res.status(400).send("Password must be at least 8 characters long.");
  }

  // 2) Check if user already exists
  pool.query(
    "SELECT id FROM users WHERE username = ?",
    [username],
    (selectErr, selectResults) => {
      if (selectErr) {
        console.error("DB error checking user existence:", selectErr);
        return res.status(500).send("Internal server error");
      }

      if (selectResults.length > 0) {
        // Username is taken
        return res.status(409).send("Username already in use.");
      }

      // 3) Generate a random salt
      crypto.randomBytes(16, (rbErr, saltBuf) => {
        if (rbErr) {
          console.error("Error generating salt:", rbErr);
          return res.status(500).send("Internal server error");
        }
        const salt = saltBuf.toString("hex");

        // 4) Hash the password with PBKDF2
        const iterations = 310000;
        const keylen = 32;
        const digest = "sha256";
        crypto.pbkdf2(password, salt, iterations, keylen, digest, (pbkdf2Err, derivedKey) => {
          if (pbkdf2Err) {
            console.error("Error hashing password:", pbkdf2Err);
            return res.status(500).send("Internal server error");
          }
          const hash = derivedKey.toString("hex");

          // 5) Insert the new user
          pool.query(
            "INSERT INTO users (username, password_hash, salt, created_at) VALUES (?, ?, ?, NOW())",
            [username, hash, salt],
            (insertErr, insertResults) => {
              if (insertErr) {
                // Handle race condition where username was inserted between SELECT and INSERT
                if (insertErr.code === "ER_DUP_ENTRY") {
                  return res.status(409).send("Username already in use.");
                }
                console.error("DB error inserting new user:", insertErr);
                return res.status(500).send("Internal server error");
              }

              // 6) Success!
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