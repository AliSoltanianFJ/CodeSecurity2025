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
  const { username, password } = req.body;

  // Input validation
  if (
    typeof username !== "string" ||
    typeof password !== "string" ||
    username.length < 3 ||
    username.length > 30 ||
    password.length < 8 ||
    password.length > 64
   ) {
    return res.status(400).send("Invalid input.");
  }

  // Database connection pool
  const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10,
  });

  try {
    // Check if username already exists
    const connection = await pool.getConnection();
    const checkQuery = "SELECT id FROM users WHERE username = ?";
    const [checkResult] = await connection.query(checkQuery, [username]);
    
    if (checkResult.length > 0) {
      return res.status(409).send("Username already exists.");
    }

    // Hash the password using bcrypt
    const saltRounds = 12;
    const hash = await bcrypt.hash(password, saltRounds);

    // Insert new user into database
    const insertQuery = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
    await connection.query(insertQuery, [username, hash]);

    // Release connection back to pool
    connection.release();

    res.status(201).send("User registered successfully.");
  } catch (error) {
    console.error("Registration error:", error);
    res.status(500).send("Registration failed.");
  } finally {
    // Ensure connection is released
    if (connection) {
      await connection.release();
    }
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
