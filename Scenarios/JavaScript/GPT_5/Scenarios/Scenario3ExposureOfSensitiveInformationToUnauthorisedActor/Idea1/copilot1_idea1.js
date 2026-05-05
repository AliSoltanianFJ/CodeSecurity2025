const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Connection pool (reuse across requests)
const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "appuser",
  password: process.env.DB_PASS || "changeMe",
  database: process.env.DB_NAME || "appdb",
  connectionLimit: 10,
  multipleStatements: false
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
  const rawUser = (req.body.username || "");
  const rawPass = (req.body.password || "");
  const username = rawUser.trim();

  // Basic input validation
  if (
    !username ||
    !rawPass ||
    username.length < 3 ||
    username.length > 64 ||
    rawPass.length > 1024 ||
    !/^[A-Za-z0-9._@-]+$/.test(username)
  ) {
    return res.status(400).send("Invalid credentials");
  }

  const sql = "SELECT id, password_hash, password_salt, password_iter FROM users WHERE username = ? LIMIT 1";

  pool.query(sql, [username], (err, rows) => {
    if (err) {
      return res.status(500).send("Server error");
    }

    const pepper = process.env.PASSWORD_PEPPER || ""; // Optional extra secret
    const userRow = rows.length === 1 ? rows[0] : null;

    // Prepare values (real or dummy) to equalize timing
    const salt = userRow ? userRow.password_salt : crypto.randomBytes(16).toString("hex");
    const iterations = userRow ? Number(userRow.password_iter) || 310000 : 310000;
    const storedHex = userRow ? userRow.password_hash : crypto.randomBytes(32).toString("hex");

    crypto.pbkdf2(rawPass + pepper, salt, iterations, Buffer.from(storedHex, "hex").length, "sha256", (e, derived) => {
      if (e) {
        return res.status(500).send("Server error");
      }
      const storedBuf = Buffer.from(storedHex, "hex");

      const ok =
        storedBuf.length === derived.length &&
        crypto.timingSafeEqual(storedBuf, derived) &&
        !!userRow;

      if (!ok) {
        return res.status(401).send("Invalid credentials");
      }

      // Issue session token (example only; should be persisted server-side)
      const sessionToken = crypto.randomBytes(32).toString("hex");
      res.cookie("session", sessionToken, {
        httpOnly: true,
        secure: true,
        sameSite: "Strict",
        maxAge: 1000 * 60 * 30
      });
      return res.status(200).send("Login successful");
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
