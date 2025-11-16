const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: false, limit: "10kb" }));

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  next();
});

const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST || "localhost",
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  ssl: process.env.DB_SSL === "true" ? { rejectUnauthorized: true } : undefined,
});

const KEYLEN = 64;
const SCRYPT_OPTS = { N: 16384, r: 8, p: 1, maxmem: 128 * 1024 * 1024 };

function isValidUsername(u) {
  return typeof u === "string" && /^[A-Za-z0-9_]{3,64}$/.test(u);
}
function isValidPassword(p) {
  return typeof p === "string" && p.length >= 8 && p.length <= 128;
}
function safeEqual(a, b) {
  if (!Buffer.isBuffer(a)) a = Buffer.from(a || "");
  if (!Buffer.isBuffer(b)) b = Buffer.from(b || "");
  if (a.length !== b.length) return false;
  try {
    return crypto.timingSafeEqual(a, b);
  } catch {
    return false;
  }
}

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
  const username = req.body?.username;
  const password = req.body?.password;

  if (!isValidUsername(username) || !isValidPassword(password)) {
    return res.status(400).json({ error: "Invalid request." });
  }

  pool.query(
    "SELECT id, password_salt, password_hash FROM users WHERE username = ? LIMIT 1",
    [username],
    (err, rows) => {
      if (err) return res.status(500).json({ error: "Unexpected error." });
      if (!rows || rows.length === 0) return res.status(401).json({ error: "Invalid credentials." });

      const user = rows[0];
      const salt = Buffer.isBuffer(user.password_salt)
        ? user.password_salt
        : Buffer.from(String(user.password_salt || ""), "hex");
      const storedHash = Buffer.isBuffer(user.password_hash)
        ? user.password_hash
        : Buffer.from(String(user.password_hash || ""), "hex");

      if (salt.length === 0 || storedHash.length === 0) {
        return res.status(401).json({ error: "Invalid credentials." });
      }

      crypto.scrypt(password, salt, KEYLEN, SCRYPT_OPTS, (e, derivedKey) => {
        if (e) return res.status(500).json({ error: "Unexpected error." });
        if (!safeEqual(derivedKey, storedHash)) {
          return res.status(401).json({ error: "Invalid credentials." });
        }
        return res.status(200).json({ success: true });
      });
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
