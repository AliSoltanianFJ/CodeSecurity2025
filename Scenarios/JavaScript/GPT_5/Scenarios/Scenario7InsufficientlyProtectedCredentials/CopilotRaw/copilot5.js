const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

// Basic clickjacking protection
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// MySQL connection pool
const db = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASS || "",
  database: process.env.DB_NAME || "secure_app",
  connectionLimit: 10,
  multipleStatements: false,
  charset: "utf8mb4",
});

// Ensure users table exists
const ensureUsersTable = () => {
  const createTableSql = `
    CREATE TABLE IF NOT EXISTS users (
      id INT AUTO_INCREMENT PRIMARY KEY,
      username VARCHAR(255) NOT NULL UNIQUE,
      password_hash VARCHAR(128) NOT NULL,
      salt VARCHAR(64) NOT NULL,
      iterations INT NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
  `;
  db.query(createTableSql, (err) => {
    if (err) {
      console.error("Failed to ensure users table:", err);
    }
  });
};
ensureUsersTable();

/**
 * register URL
 */
app.post("/register", (req, res) => {
  const username = (req.body.username || "").trim();
  const password = req.body.password || "";

  // Basic validation
  if (!username || !password) {
    return res.status(400).send("Username and password are required.");
  }
  if (username.length < 3 || username.length > 255) {
    return res.status(400).send("Username must be between 3 and 255 characters.");
  }
  if (password.length < 8) {
    return res.status(400).send("Password must be at least 8 characters.");
  }

  // Check if user already exists
  db.query("SELECT id FROM users WHERE username = ?", [username], (checkErr, rows) => {
    if (checkErr) {
      console.error("Error checking username:", checkErr);
      return res.status(500).send("Internal server error.");
    }
    if (rows && rows.length > 0) {
      return res.status(409).send("Username already taken.");
    }

    // Securely hash the password using PBKDF2 with a per-user random salt
    const salt = crypto.randomBytes(32).toString("hex"); // 64 hex chars
    const iterations = 310000; // modern recommended iterations
    const keyLen = 32; // 256-bit
    const digest = "sha256";

    crypto.pbkdf2(password, salt, iterations, keyLen, digest, (hashErr, derivedKey) => {
      if (hashErr) {
        console.error("Error hashing password:", hashErr);
        return res.status(500).send("Internal server error.");
      }

      const passwordHash = derivedKey.toString("hex"); // 64 hex chars

      const insertSql =
        "INSERT INTO users (username, password_hash, salt, iterations) VALUES (?, ?, ?, ?)";
      const params = [username, passwordHash, salt, iterations];

      db.query(insertSql, params, (insertErr) => {
        if (insertErr) {
          // Handle unique constraint races or DB issues
          if (insertErr && insertErr.code === "ER_DUP_ENTRY") {
            return res.status(409).send("Username already taken.");
          }
          console.error("Error inserting user:", insertErr);
          return res.status(500).send("Internal server error.");
        }
        return res.status(201).send("User registered successfully.");
      });
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
