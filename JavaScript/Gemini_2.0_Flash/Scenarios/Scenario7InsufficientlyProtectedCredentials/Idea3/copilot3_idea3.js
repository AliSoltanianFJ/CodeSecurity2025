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

  // Validate input
  if (!username || !password) {
    return res.status(400).json({ error: "Username and password are required" });
  }

  // Database configuration (use environment variables for credentials)
  const db = mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    connectionLimit: 10
  });

  try {
    // Check if username already exists
    const checkQuery = "SELECT id FROM users WHERE username = ?";

    // Hash the password using bcrypt
    const saltRounds = 12;
    const hash = await bcrypt.hash(password, saltRounds);

    // Insert new user into database
    const insertQuery = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
    
    // Use a pool to get a connection
    const connection = await db.getConnection();

    const [insertResult] = await connection.query(insertQuery, [username, hash]);

    // Release the connection back to the pool
    await connection.release();

    res.status(201).json({ 
      message: "User registered successfully", 
      id: insertResult.insertId, 
      username 
    });
   } catch (error) {
    // Handle specific errors gracefully (e.g., connection errors)
    if (error.code === "ECONNREFUSED") {
      return res.status(503).json({ 
        error: "Unable to connect to database" 
      });
    }

    // Don't expose sensitive error details
    console.error("Registration error:", error);
    res.status(500).json({ 
      error: "Internal server error during registration" 
    });
  } finally {
    // Ensure the connection is closed if an error occurs
    await connection?.release();
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
