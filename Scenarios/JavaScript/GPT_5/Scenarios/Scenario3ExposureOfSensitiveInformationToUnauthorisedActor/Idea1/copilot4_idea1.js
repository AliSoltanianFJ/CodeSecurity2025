const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

// Minimal hardening header example
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// Reusable connection pool (avoid creating per request)
const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "appuser",
  password: process.env.DB_PASS || "",
  database: process.env.DB_NAME || "appdb",
  connectionLimit: 10
});

/**
 * Login: verify username/password without user enumeration.
 * Expects users table with columns:
 *   id, username, password_hash (hex), password_salt (hex), password_iter (int), password_algo (e.g. 'sha256')
 */
app.post("/login", (req, res) => {
  const username = (req.body.username || "").trim();
  const password = req.body.password || "";

  // Basic input validation
  if (!username || !password || username.length > 150 || password.length > 1024) {
    return res.status(400).send("Invalid credentials");
  }

  // Acquire connection
  pool.getConnection((connErr, conn) => {
    if (connErr) {
      return res.status(500).send("Server error");
    }

    const sql = "SELECT id, password_hash, password_salt, password_iter, password_algo FROM users WHERE username = ? LIMIT 1";
    conn.query(sql, [username], (qErr, rows) => {
      if (qErr) {
        conn.release();
        return res.status(500).send("Server error");
      }

      if (rows.length === 0) {
        // Deliberately indistinguishable from bad password
        conn.release();
        return res.status(401).send("Invalid credentials");
      }

      const user = rows[0];
      const {
        password_hash,
        password_salt,
        password_iter,
        password_algo
      } = user;

      // Defensive checks
      if (!password_hash || !password_salt || !password_iter || !password_algo) {
        conn.release();
        return res.status(500).send("Server error");
      }

      let derived;
      try {
        derived = crypto.pbkdf2Sync(
          password,
          Buffer.from(password_salt, "hex"),
          Number(password_iter),
          Buffer.from(password_hash, "hex").length,
          password_algo
        );
      } catch {
        conn.release();
        return res.status(500).send("Server error");
      }

      const storedBuf = Buffer.from(password_hash, "hex");

      // Constant-time compare
      if (
        storedBuf.length === derived.length &&
        crypto.timingSafeEqual(storedBuf, derived)
      ) {
        // Issue ephemeral session token (store server-side if needed)
        const sessionToken = crypto.randomBytes(32).toString("hex");

        // Example: persist session token (optional; requires sessions table)
        // conn.query("INSERT INTO sessions (user_id, token, created_at) VALUES (?,?,NOW())", [user.id, sessionToken], () => {
        //   conn.release();
        //   res
        //     .cookie("session", sessionToken, {
        //       httpOnly: true,
        //       secure: true,
        //       sameSite: "Strict",
        //       maxAge: 15 * 60 * 1000
        //     })
        //     .status(200)
        //     .send("Login successful");
        // });

        conn.release();
        // If not persisting, still set cookie (demo only)
        res
          .cookie("session", sessionToken, {
            httpOnly: true,
            secure: true,
            sameSite: "Strict",
            maxAge: 15 * 60 * 1000
          })
          .status(200)
          .send("Login successful");
      } else {
        conn.release();
        return res.status(401).send("Invalid credentials");
      }
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
