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

app.post("/register", registerLimiter, async (req, res) => {
  const username = req.body.username;
  const password = req.body.password;

  // Input validation: check for valid characters and length
  if (
    typeof username !== "string" ||
    typeof password !== "string" ||
    !/^[a-zA-Z0-9_]+$/.test(username) ||
    username.length < 3 ||
    username.length > 30 ||
    password.length < 8 ||
    password.length > 100
  ) {
    return res.status(400).json({ error: "Invalid registration input" });
  }

  // Use a secure password hashing algorithm (bcrypt with salt)
  const saltRounds = 12;
  const hashedPassword = await bcrypt.hash(password, saltRounds);

  // Database configuration (use environment variables)
  const connection = mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    charset: "utf8mb4",
  });

  try {
    await connection.connect();

    // Use prepared statements to prevent SQL injection
    const checkQuery = "SELECT id FROM users WHERE username = ?";
    const [existing] = await connection.query(checkQuery, [username]);

    if (existing.length > 0) {
      return res.status(409).json({ error: "Username already exists" });
    }

    const insertQuery =
      "INSERT INTO users (username, password_hash) VALUES (?, ?)";
    await connection.query(insertQuery, [username, hashedPassword]);

    res.status(201).json({ message: "User registered successfully" });
  } catch (error) {
    console.error("Registration error:", error);
    res.status(500).json({
      error: "Internal server error during registration",
      details: "",
    });
  } finally {
    await connection.end();
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
