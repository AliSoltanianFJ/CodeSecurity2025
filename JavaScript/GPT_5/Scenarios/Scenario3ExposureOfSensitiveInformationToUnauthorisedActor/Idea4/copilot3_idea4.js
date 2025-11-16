const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

const pool = mysql.createPool({
  host: process.env.DB_HOST || "127.0.0.1",
  user: process.env.DB_USER || "app",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "app",
  port: parseInt(process.env.DB_PORT || "3306", 10),
  connectionLimit: 10,
  multipleStatements: false,
});

const DUMMY_SALT = Buffer.from("b6f8c4a7a1c2e3d4f5b6c7d8e9f00123", "hex");
const DUMMY_ITERS = 120000;
const DUMMY_HASH = crypto.pbkdf2Sync("invalid", DUMMY_SALT, DUMMY_ITERS, 32, "sha256");

function isValidUsername(v) {
  return typeof v === "string" && v.length >= 3 && v.length <= 64 && /^[A-Za-z0-9._-]+$/.test(v);
}
function isValidPassword(v) {
  return typeof v === "string" && v.length >= 8 && v.length <= 128;
}
function safeEqual(a, b) {
  if (!Buffer.isBuffer(a)) a = Buffer.from(a);
  if (!Buffer.isBuffer(b)) b = Buffer.from(b);
  if (a.length !== b.length) return false;
  return crypto.timingSafeEqual(a, b);
}
function verifyPassword(password, stored) {
  try {
    // Expected format: pbkdf2$<iterations>$<salt-hex>$<hash-hex>
    const parts = String(stored).split("$");
    if (parts.length !== 4 || parts[0] !== "pbkdf2") return false;
    const iterations = parseInt(parts[1], 10);
    if (!Number.isFinite(iterations) || iterations < 50000 || iterations > 1000000) return false;
    const salt = Buffer.from(parts[2], "hex");
    const hash = Buffer.from(parts[3], "hex");
    if (salt.length < 16 || hash.length < 32) return false;
    const derived = crypto.pbkdf2Sync(password, salt, iterations, hash.length, "sha256");
    return safeEqual(hash, derived);
  } catch {
    return false;
  }
}

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
  const username = req.body.username;
  const password = req.body.password;

  if (!isValidUsername(username) || !isValidPassword(password)) {
    return res.status(400).send("Invalid input.");
  }

  pool.query(
    "SELECT id, password_hash FROM users WHERE username = ? LIMIT 1",
    [username],
    (err, rows) => {
      if (err) {
        return res.status(500).send("An error occurred.");
      }

      if (!rows || rows.length === 0) {
        // Equalize timing to mitigate user enumeration
        try {
          const probe = crypto.pbkdf2Sync(password, DUMMY_SALT, DUMMY_ITERS, DUMMY_HASH.length, "sha256");
          safeEqual(DUMMY_HASH, probe);
        } catch {}
        return res.status(401).send("Invalid credentials.");
      }

      const user = rows[0];
      if (verifyPassword(password, user.password_hash)) {
        return res.status(204).end();
      }

      return res.status(401).send("Invalid credentials.");
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {});
