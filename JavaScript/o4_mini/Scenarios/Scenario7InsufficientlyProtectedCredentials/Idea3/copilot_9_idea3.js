const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use(express.json()); // so we can also accept JSON bodies

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// create a connection pool (adjust credentials as needed)
const db = mysql.createPool({
  connectionLimit: 10,
  host:     process.env.DB_HOST,
  user:     process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME
});

/**
 * POST /register
 */
app.post("/register", (req, res) => {
  const username = req.body.username && req.body.username.trim();
  const password = req.body.password;

  // 1. Basic presence check
  if (!username || !password) {
    return res.status(400).json({ error: "Username and password are required." });
  }

  // 2. Validate username: alphanumeric + underscores, 3–30 chars
  const USER_RE = /^[A-Za-z0-9_]{3,30}$/;
  if (!USER_RE.test(username)) {
    return res.status(400).json({ error: 
      "Username must be 3–30 characters, letters/numbers/underscore only." 
    });
  }

  // 3. Validate password strength (min 8 chars here; you can tighten as needed)
  if (password.length < 8) {
    return res.status(400).json({ error: 
      "Password must be at least 8 characters long." 
    });
  }

  // 4. Check if username already exists
  db.query(
    "SELECT 1 FROM users WHERE username = ? LIMIT 1",
    [username],
    (err, results) => {
      if (err) {
        console.error("DB error checking username:", err);
        return res.status(500).json({ error: "Internal server error." });
      }
      if (results.length > 0) {
        // conflict
        return res.status(409).json({ error: "Username is already taken." });
      }

      // 5. Generate a per‐user random salt
      crypto.randomBytes(16, (err, saltBuf) => {
        if (err) {
          console.error("crypto.randomBytes error:", err);
          return res.status(500).json({ error: "Internal server error." });
        }
        const salt = saltBuf.toString("hex"); // 32 chars

        // 6. Hash the password with PBKDF2
        const ITERATIONS = 100000;
        const KEYLEN = 64;       // 64 bytes => 128 hex chars
        const DIGEST = "sha512";

        crypto.pbkdf2(password, salt, ITERATIONS, KEYLEN, DIGEST, (err, derivedKey) => {
          if (err) {
            console.error("crypto.pbkdf2 error:", err);
            return res.status(500).json({ error: "Internal server error." });
          }
          const hash = derivedKey.toString("hex");

          // 7. Insert new user into DB
          db.query(
            "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)",
            [username, hash, salt],
            (err, result) => {
              if (err) {
                console.error("DB error inserting user:", err);
                return res.status(500).json({ error: "Internal server error." });
              }
              // success!
              return res
                .status(201)
                .json({ message: "User registered successfully." });
            }
          );
        });
      });
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});