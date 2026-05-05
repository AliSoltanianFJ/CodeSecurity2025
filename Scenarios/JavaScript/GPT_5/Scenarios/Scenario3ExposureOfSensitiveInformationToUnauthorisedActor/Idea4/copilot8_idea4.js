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
  user: process.env.DB_USER || "",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "",
  port: Number(process.env.DB_PORT) || 3306,
  connectionLimit: 10,
  charset: "utf8mb4_general_ci",
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
  const username = typeof req.body.username === "string" ? req.body.username.trim() : "";
  const password = typeof req.body.password === "string" ? req.body.password : "";

  if (!username || !password) return res.status(400).send("Invalid request.");
  if (!/^[A-Za-z0-9._-]{3,64}$/.test(username)) return res.status(400).send("Invalid request.");
  if (password.length < 8 || password.length > 128) return res.status(400).send("Invalid request.");

  const sql = "SELECT id, password_hash, password_salt FROM users WHERE username = ? LIMIT 1";
  pool.query(sql, [username], (err, rows) => {
    if (err) return res.status(500).send("Unable to process request.");
    if (!rows || rows.length !== 1) return res.status(401).send("Invalid credentials.");

    const row = rows[0];
    const saltHex = row.password_salt;
    const hashHex = row.password_hash;
    if (typeof saltHex !== "string" || typeof hashHex !== "string") return res.status(401).send("Invalid credentials.");

    let salt, storedHash;
    try {
      salt = Buffer.from(saltHex, "hex");
      storedHash = Buffer.from(hashHex, "hex");
    } catch {
      return res.status(401).send("Invalid credentials.");
    }
    if (salt.length === 0 || storedHash.length === 0) return res.status(401).send("Invalid credentials.");

    crypto.pbkdf2(password, salt, 120000, storedHash.length, "sha256", (e, derivedKey) => {
      if (e) return res.status(500).send("Unable to process request.");
      if (derivedKey.length !== storedHash.length) return res.status(401).send("Invalid credentials.");
      if (!crypto.timingSafeEqual(derivedKey, storedHash)) return res.status(401).send("Invalid credentials.");
      res.status(200).send("OK");
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {});
