const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  next();
});

// Configure MySQL connection pool via environment variables
const pool = mysql.createPool({
  host: process.env.DB_HOST || "127.0.0.1",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "app_db",
  port: Number(process.env.DB_PORT || 3306),
  connectionLimit: 10,
});

/**
 * register URL
 */
app.post("/register", (req, res) => {
  const username = typeof req.body.username === "string" ? req.body.username.trim() : "";
  const password = typeof req.body.password === "string" ? req.body.password : "";

  // Basic input validation
  const usernameOk = /^[A-Za-z0-9_]{3,32}$/.test(username);
  const passwordOk = typeof password === "string" && password.length >= 12;

  if (!usernameOk || !passwordOk) {
    return res.status(400).json({
      error: "Invalid input.",
    });
  }

  // Check if username already exists
  pool.query(
    "SELECT id FROM users WHERE username = ? LIMIT 1",
    [username],
    (selectErr, rows) => {
      if (selectErr) {
        return res.status(500).json({ error: "Server error." });
      }
      if (rows && rows.length > 0) {
        // Avoid leaking too much detail, but signal conflict
        return res.status(409).json({ error: "Username already taken." });
      }

      // Generate salt and hash password using scrypt
      const salt = crypto.randomBytes(16).toString("hex");
      const scryptOpts = { N: 16384, r: 8, p: 1 };
      crypto.scrypt(password, salt, 64, scryptOpts, (err, derivedKey) => {
        if (err) {
          return res.status(500).json({ error: "Server error." });
        }

        const hashHex = derivedKey.toString("hex");
        const passwordHash = `scrypt:${scryptOpts.N}:${scryptOpts.r}:${scryptOpts.p}:${salt}:${hashHex}`;

        // Store username and password hash using a parameterized query
        pool.query(
          "INSERT INTO users (username, password_hash) VALUES (?, ?)",
          [username, passwordHash],
          (insertErr) => {
            if (insertErr) {
              // Handle potential race with duplicate username
              if (insertErr && insertErr.code === "ER_DUP_ENTRY") {
                return res.status(409).json({ error: "Username already taken." });
              }
              return res.status(500).json({ error: "Server error." });
            }
            return res.status(201).json({ message: "Registered successfully." });
          }
        );
      });
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
