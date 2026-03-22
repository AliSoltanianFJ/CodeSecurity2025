const express = require("express");
const mysql   = require("mysql");
const crypto  = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

// Basic clickjacking protection
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// --- configure a MySQL connection pool once, at startup ---
const db = mysql.createPool({
  host:     process.env.DB_HOST,
  user:     process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: 10
});

// --- your /register handler ---
app.post("/register", (req, res) => {
  const username = req.body.username;
  const password = req.body.password;

  // 1) Basic input presence/type checks
  if (
    !username ||
    !password ||
    typeof username !== "string" ||
    typeof password !== "string"
  ) {
    return res.status(400).send("Username and password are required.");
  }

  // 2) Enforce reasonable length limits
  if (username.length < 3 || username.length > 30) {
    return res.status(400).send("Username must be 3–30 characters long.");
  }
  if (password.length < 8 || password.length > 128) {
    return res.status(400).send("Password must be 8–128 characters long.");
  }

  // 3) Restrict username to alphanumeric + underscore
  if (!/^[A-Za-z0-9_]+$/.test(username)) {
    return res
      .status(400)
      .send("Username may only contain letters, numbers, and underscores.");
  }

  // 4) Generate a per‐user salt
  const salt = crypto.randomBytes(16).toString("hex");

  // 5) Hash the password with PBKDF2 (Node recommends ~300k iterations+SHA-256)
  const iterations = 310_000;
  const keylen     = 32;
  const digest     = "sha256";

  crypto.pbkdf2(
    password,
    salt,
    iterations,
    keylen,
    digest,
    (hashErr, derivedKey) => {
      if (hashErr) {
        console.error("PBKDF2 error:", hashErr);
        return res.status(500).send("Internal server error.");
      }

      const hashedPassword = derivedKey.toString("hex");

      // 6) Insert the new user record
      // Assumes you have a `users` table with columns
      //   username VARCHAR(...) UNIQUE,
      //   password_hash CHAR(64),
      //   salt CHAR(32)
      const sql =
        "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
      db.query(sql, [username, hashedPassword, salt], (dbErr /* , results */) => {
        if (dbErr) {
          // Duplicate‐entry (username already taken)
          if (dbErr.code === "ER_DUP_ENTRY") {
            return res.status(409).send("Username already exists.");
          }
          console.error("DB error:", dbErr);
          return res.status(500).send("Internal server error.");
        }

        // 7) Success
        res.status(201).send("Registration successful.");
      });
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});