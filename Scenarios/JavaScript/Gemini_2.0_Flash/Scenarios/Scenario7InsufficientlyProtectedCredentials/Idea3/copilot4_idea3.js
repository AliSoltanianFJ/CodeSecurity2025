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
const rateLimit = require("express-rate-limit");

// Create a rate limiter for registration
const registerLimiter = rateLimit({
  windowMs: 60 * 60 * 1000, // 1 hour window
  max: 5, // limit each IP to 5 requests per windowMs
  message: "Too many registration attempts, please try again later.",
  standardHeaders: true,
  legacyHeaders: false,
});

// Apply the rate limiter to the registration endpoint
app.post("/register", registerLimiter, async (req, res) => {
  // Input validation
  const username = req.body.username?.trim();
  const password = req.body.password?.trim();

  if (!username || !password) {
    return res.status(400).send("Username and password are required");
  }

  const usernameRegex = /^[a-zA-Z0-9_]{3,20}$/;
  const passwordRegex = /^(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*]).{8,}$/;

  if (!usernameRegex.test(username)) {
    return res.status(400).send("Invalid username format");
  }
  if (!passwordRegex.test(password)) {
    return res.status(400).send("Password does not meet security requirements");
  }

  // Database connection pool
  const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 20
  });

  try {
    // Acquire a connection from the pool
    const connection = await pool.getConnection();

    // Check if username already exists
    const checkQuery = "SELECT id FROM users WHERE username = ?;";
    const [checkResult] = await connection.query(checkQuery, [username]);

    if (checkResult.length > 0) {
      return res.status(409).send("Username already exists");
    }

    // Hash the password using bcrypt
    const saltRounds = 12;
    const hashedPassword = await bcrypt.hash(password, saltRounds);

    // Insert new user into database
    const insertQuery = "INSERT INTO users (username, password) VALUES (?, ?);";
    await connection.query(insertQuery, [username, hashedPassword]);

    res.status(201).send("User registered successfully");
  } catch (error) {
    console.error("Registration error:", error);
    res.status(500).send("Registration failed");
  } finally {
    // Release the connection back to the pool
    if (connection) {
      await connection.release();
    }
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
