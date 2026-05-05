const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Reusable pool (avoid creating connections per request)
const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASS || "",
  database: process.env.DB_NAME || "appdb"
});

// Simple in-memory session store (placeholder; replace with redis/db in production)
const sessions = new Map();

// Precomputed dummy values to equalize timing for unknown users
const DUMMY_ITERATIONS = 310000;
const DUMMY_SALT = crypto.randomBytes(16).toString("hex");
const DUMMY_HASH = crypto.pbkdf2Sync("ChangeMe_Dummy_Base", DUMMY_SALT, DUMMY_ITERATIONS, 32, "sha256");

/**
 * Log in the user if the username and password are correct.
 * Uses constant-time comparison and avoids user enumeration.
 */
app.post("/login", (req, res) => {
  const username = (req.body.username || "").trim();
  const password = req.body.password || "";

  // Basic input validation
  if (
    !username ||
    !password ||
    username.length > 128 ||
    password.length > 1024 ||
    !/^[A-Za-z0-9_.-]+$/.test(username)
  ) {
    return res.status(400).send("Invalid request");
  }

  pool.query(
    "SELECT id, password_hash, password_salt, password_iterations FROM users WHERE username = ? LIMIT 1",
    [username],
    (err, rows) => {
      if (err) {
        return res.status(500).send("Server error");
      }

      const userFound = rows.length === 1;
      // Use real user data if found; otherwise dummy values
      const record = userFound
        ? rows[0]
        : {
            id: 0,
            password_hash: DUMMY_HASH.toString("hex"),
            password_salt: DUMMY_SALT,
            password_iterations: DUMMY_ITERATIONS
          };

      const salt = record.password_salt;
      const iterations =
        typeof record.password_iterations === "number" && record.password_iterations > 100000
          ? record.password_iterations
          : DUMMY_ITERATIONS;

      let storedBuffer;
      try {
        storedBuffer = Buffer.from(record.password_hash, "hex");
        if (storedBuffer.length !== 32) {
          // Enforce expected length; fall back to dummy buffer to avoid leaking info
          storedBuffer = DUMMY_HASH;
        }
      } catch {
        storedBuffer = DUMMY_HASH;
      }

      crypto.pbkdf2(password, salt, iterations, storedBuffer.length, "sha256", (e, derived) => {
        if (e) {
          return res.status(500).send("Server error");
        }

        let valid = false;
        try {
          if (storedBuffer.length === derived.length && crypto.timingSafeEqual(storedBuffer, derived) && userFound) {
            valid = true;
          }
        } catch {
          valid = false;
        }

        if (!valid) {
          // Generic failure response
            return res.status(401).send("Invalid credentials");
        }

        // Issue ephemeral session token
        const sessionToken = crypto.randomBytes(32).toString("hex");
        sessions.set(sessionToken, {
          userId: record.id,
          username,
          createdAt: Date.now()
        });

        // Set secure cookie (adjust 'Secure' when using HTTPS)
        res.setHeader(
          "Set-Cookie",
          `session=${sessionToken}; HttpOnly; SameSite=Strict; Path=/; Max-Age=1800`
        );

        return res.status(200).send("Login successful");
      });
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
