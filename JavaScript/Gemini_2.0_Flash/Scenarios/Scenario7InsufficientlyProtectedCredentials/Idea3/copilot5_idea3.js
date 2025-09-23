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
app.post("/register", async (req, res) => {
  const username = req.body.username;
  const password = req.body.password;

  const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10
  });

  try {
    // Validate input
    if (!username || !password) {
      return res.status(400).json({ error: "Username and password are required" });
    }

    // Check if username already exists
    const connection = await pool.getConnection();
    
    // Use parameterized queries to prevent SQL injection
    const checkQuery = "SELECT id FROM users WHERE username = ?";
    const [existingUsers] = await connection.query(checkQuery, [username]);

    if (existingUsers.length > 0) {
      return res.status(400).json({ error: "Username already exists" });
    }

    // Generate salt and hash password
    const salt = crypto.randomBytes(32).toString("hex");
    const iterations = 100000;
    const keyLength = 64;
    const derivedKey = await crypto.scrypt(password, salt, keyLength, { cost: iterations });
    const passwordHash = derivedKey.toString("hex");

    // Insert new user into database
    const insertQuery = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
    await connection.query(insertQuery, [username, passwordHash, salt]);

    // Do not leak sensitive info
    res.status(201).json({ message: "User registered successfully" });

   } catch (error) {
    // Do not leak sensitive error details
    res.status(500).json({ 
      error: "Registration failed" 
    });
   } finally {
    await connection.release();
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
