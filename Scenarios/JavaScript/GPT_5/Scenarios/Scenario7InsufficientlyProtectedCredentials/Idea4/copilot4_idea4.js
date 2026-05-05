const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: false, limit: "10kb" }));

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "app_user",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "app",
  connectionLimit: 10,
  multipleStatements: false,
});

function validUsername(u) {
  return typeof u === "string" && u.length >= 3 && u.length <= 32 && /^[A-Za-z0-9_.-]+$/.test(u);
}

function validPassword(p) {
  return typeof p === "string" && p.length >= 12 && p.length <= 128;
}

/**
 * register URL
 */
app.post("/register", (req, res) => {
  const username = req.body.username;
  const password = req.body.password;

  if (!validUsername(username) || !validPassword(password)) {
    return res.status(400).send("Invalid input");
  }

  const salt = crypto.randomBytes(16);
  crypto.scrypt(password, salt, 64, { N: 16384, r: 8, p: 1, maxmem: 128 * 1024 * 1024 }, (err, derivedKey) => {
    if (err) return res.status(500).send("Server error");
    const hash = `scrypt$${salt.toString("base64")}$${derivedKey.toString("base64")}`;
    pool.query(
      "INSERT INTO users (username, password_hash) VALUES (?, ?)",
      [username, hash],
      (qErr) => {
        if (qErr) {
          if (qErr.code === "ER_DUP_ENTRY") return res.status(409).send("Conflict");
          return res.status(500).send("Server error");
        }
        return res.status(201).send("Created");
      }
    );
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  // Server started
});
