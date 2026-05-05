const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const { promisify } = require("util");

const scrypt = promisify(crypto.scrypt);

const app = express();
app.use(express.urlencoded({ extended: false, limit: "10kb" }));

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  next();
});

const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST,
  port: Number(process.env.DB_PORT) || 3306,
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  charset: "utf8mb4",
  multipleStatements: false,
});
const query = promisify(pool.query).bind(pool);

app.post("/register", async (req, res) => {
  const username = typeof req.body.username === "string" ? req.body.username.trim() : "";
  const password = typeof req.body.password === "string" ? req.body.password : "";

  if (!username || !password) return res.status(400).send("Invalid request.");
  if (username.length < 3 || username.length > 50) return res.status(400).send("Invalid request.");
  if (!/^[A-Za-z0-9._-]+$/.test(username)) return res.status(400).send("Invalid request.");
  if (password.length < 12 || password.length > 128) return res.status(400).send("Invalid request.");

  try {
    const salt = crypto.randomBytes(16);
    const hash = await scrypt(password, salt, 64, { N: 16384, r: 8, p: 1, maxmem: 128 * 1024 * 1024 });
    const password_hash = Buffer.from(hash).toString("base64");
    const password_salt = salt.toString("base64");

    await query(
      "INSERT INTO users (username, password_hash, password_salt) VALUES (?, ?, ?)",
      [username, password_hash, password_salt]
    );
    return res.status(201).send("Created.");
  } catch (err) {
    if (err && err.code === "ER_DUP_ENTRY") return res.status(409).send("Conflict.");
    return res.status(500).send("Server error.");
  }
});

const PORT = Number(process.env.PORT) || 3000;
app.listen(PORT);
