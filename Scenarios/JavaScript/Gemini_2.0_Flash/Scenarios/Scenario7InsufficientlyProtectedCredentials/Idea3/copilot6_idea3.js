const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * register URL
 */
const mysql = require("mysql");

const db = mysql.createConnection({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  connectionLimit: 10
});

app.post("/register", async (req, res) => {
  const username = req.body.username;
  const password = req.body.password;

  // Validate input
  if (!username || !password) {
    return res.status(400).send("Username and password are required");
  }

  // Sanitize input
  const sanitizedPassword = db.escape(password);

  // Generate salt and hash
  const salt = crypto.randomBytes(32).toString("hex");
  const hash = crypto
    .scryptSync(sanitizedPassword, salt, 64, { cost: 16384, blockSize: 8, derivedKeyLength: 64 })
    .toString("hex");

  // Store user info in DB
  const query = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
  
  db.query(
    query,
    [sanitizedPassword, hash, salt],
    (err, results) => {
      if (err) {
        // Check for duplicate username error
        if (err.code === "ER_DUP_ENTRY") {
          return res.status(409).send("Username already exists");
        }
        console.error("Registration error:", err);
        return res.status(500).send("Internal server error");
      }
      // Do not leak sensitive info
      res.status(201).send("User registered successfully");
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
