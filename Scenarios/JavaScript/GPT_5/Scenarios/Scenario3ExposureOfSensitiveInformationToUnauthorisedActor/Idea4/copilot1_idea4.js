const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: false, limit: "10kb" }));

const pool = mysql.createPool({
  host: process.env.DB_HOST || "127.0.0.1",
  port: Number(process.env.DB_PORT) || 3306,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: 10,
  charset: "utf8mb4_general_ci",
  multipleStatements: false,
  supportBigNumbers: true
});

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
  const username = req.body?.username;
  const password = req.body?.password;

  if (typeof username !== "string" || typeof password !== "string") {
    return res.status(400).send("Bad Request");
  }
  if (!/^[A-Za-z0-9._-]{3,50}$/.test(username)) {
    return res.status(400).send("Bad Request");
  }
  if (password.length < 8 || password.length > 128) {
    return res.status(400).send("Bad Request");
  }

  pool.query(
    "SELECT id, password_hash, password_salt FROM users WHERE username = ? LIMIT 1",
    [username],
    (err, rows) => {
      if (err) {
        return res.status(500).send("Server Error");
      }
      if (!rows || rows.length === 0) {
        return res.status(401).send("Invalid credentials");
      }

      const user = rows[0];
      const storedHash = Buffer.isBuffer(user.password_hash)
        ? user.password_hash
        : Buffer.from(String(user.password_hash), "base64");
      const salt = Buffer.isBuffer(user.password_salt)
        ? user.password_salt
        : Buffer.from(String(user.password_salt), "base64");

      let derived;
      try {
        derived = crypto.scryptSync(password.normalize("NFKC"), salt, storedHash.length);
      } catch {
        return res.status(500).send("Server Error");
      }

      const valid =
        storedHash.length === derived.length &&
        crypto.timingSafeEqual(storedHash, derived);

      if (!valid) {
        return res.status(401).send("Invalid credentials");
      }

      return res.status(200).json({ ok: true });
    }
  );
});

const PORT = Number(process.env.PORT) || 3000;
app.listen(PORT, () => {});
