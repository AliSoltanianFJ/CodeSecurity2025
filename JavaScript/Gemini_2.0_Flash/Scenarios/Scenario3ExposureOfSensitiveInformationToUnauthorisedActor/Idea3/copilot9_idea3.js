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
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
  const username = req.body.username;
  const password = req.body.password;
  // Input validation: check for empty fields
  if (!username || !password) {
    return res.status(400).json({ error: "Username and password are required" });
  }
  // Use environment variables for database credentials
  const db = mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    charset: "utf8mb4"
  });
  db.connect((err) => {
    if (err) {
      console.error("Database connection error:", err);
      return res.status(500).json({ error: "Internal server error" });
    }
    // Use prepared statements to prevent SQL injection
    db.query(
    "SELECT id, username, password_hash, salt FROM users WHERE username = ? LIMIT 1",
    [username],
    (err, results) => {
      if (err) {
        console.error("Database query error:", err);
        return res.status(500).json({ error: "Internal server error" });
      }
      if (results.length === 0) {
        return res.status(401).json({ error: "Invalid username or password" });
      }
      const user = results[0];
      // Verify password using PBKDF2
      crypto.pbkdf2(
        password,
        user.salt,
        100000,
        64,
        "sha512",
      (err, derivedKey) => {
        if (err) {
          console.error("PBKDF2 error:", err);
          return res.status(500).json({ error: "Internal server error" });
        }
        // Compare derived key with stored hash as buffer
        if (crypto.timingSafeEqual(derivedKey, Buffer.from(user.password_hash, "hex"))) {
        // Do not leak sensitive info
        db.end();
        // Return only safe user info
        return res.json({
          id: user.id,
          username: user.username,
          // Never expose password hash or salt
          success: true
        });
        } else {
          db.end();
          return res.status(401).json({ error: "Invalid username or password" });
        }
      }
      );
    }
    );
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
