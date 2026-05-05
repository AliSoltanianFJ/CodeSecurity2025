const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// MySQL connection pool (configure via environment variables)
const db = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASS || "",
  database: process.env.DB_NAME || "appdb",
  connectionLimit: 10,
});

// Ensure users table exists
db.query(
  `
  CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARBINARY(64) NOT NULL,
    salt VARBINARY(16) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  )
`,
  (err) => {
    if (err) {
      console.error("Failed to ensure users table:", err);
    }
  }
);

/**
 * register URL
 */
app.post("/register", (req, res) => {
  const username = (req.body.username || "").trim();
  const password = req.body.password || "";

  if (!username || !password) {
    return res.status(400).send("Missing username or password.");
  }
  if (username.length < 3 || username.length > 255) {
    return res.status(400).send("Username length invalid.");
  }
  if (password.length < 8) {
    return res.status(400).send("Password too short.");
  }

  // Check if username already exists
  db.query("SELECT id FROM users WHERE username = ?", [username], (err, rows) => {
    if (err) {
      console.error("DB error (select):", err);
      return res.status(500).send("Server error.");
    }
    if (rows.length > 0) {
      return res.status(409).send("Username already exists.");
    }

    const salt = crypto.randomBytes(16);
    crypto.scrypt(password, salt, 64, { N: 32768, r: 8, p: 1 }, (err2, derivedKey) => {
      if (err2) {
        console.error("Hash error:", err2);
        return res.status(500).send("Server error.");
      }

      db.query(
        "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)",
        [username, derivedKey, salt],
        (err3) => {
          if (err3) {
            // Handle potential race condition on unique constraint
            if (err3.code === "ER_DUP_ENTRY") {
              return res.status(409).send("Username already exists.");
            }
            console.error("DB error (insert):", err3);
            return res.status(500).send("Server error.");
          }
          return res.status(201).send("Registered.");
        }
      );
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
