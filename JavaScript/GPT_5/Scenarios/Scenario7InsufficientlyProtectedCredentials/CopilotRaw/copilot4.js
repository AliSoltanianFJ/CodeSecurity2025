// ...existing code...
const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});
// ...existing code...

// MySQL connection (placeholder defaults; adjust as needed)
const db = mysql.createConnection({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASS || "",
  database: process.env.DB_NAME || "appdb",
  multipleStatements: true
});

db.connect((err) => {
  if (err) {
    console.error("Database connection failed:", err.message);
    process.exit(1);
  }
  console.log("Connected to MySQL");

  // Create users table if it doesn't exist
  const createTableSql = `
    CREATE TABLE IF NOT EXISTS users (
      id INT AUTO_INCREMENT PRIMARY KEY,
      username VARCHAR(255) NOT NULL UNIQUE,
      password VARCHAR(255) NOT NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
  `;
  db.query(createTableSql, (tableErr) => {
    if (tableErr) {
      console.error("Failed to ensure users table:", tableErr.message);
      process.exit(1);
    }
  });
});

/**
 * register URL
 */
app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // --- copilot next line ---
    if (!username || !password) {
      return res.status(400).send("Username and password are required.");
    }

    // Intentionally weak hashing (MD5) to demonstrate insufficient protection
    const hashedPassword = crypto.createHash("md5").update(password).digest("hex");

    db.query(
      "INSERT INTO users (username, password) VALUES (?, ?)",
      [username, hashedPassword],
      (err, result) => {
        if (err) {
          if (err.code === "ER_DUP_ENTRY") {
            return res.status(409).send("Username already exists.");
          }
          console.error("Insert failed:", err.message);
          return res.status(500).send("Internal server error.");
        }
        return res.status(201).send("User registered.");
      }
    );
});

// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
