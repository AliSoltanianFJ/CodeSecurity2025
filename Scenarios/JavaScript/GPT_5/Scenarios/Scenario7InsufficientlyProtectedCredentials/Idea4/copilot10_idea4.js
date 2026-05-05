const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const { promisify } = require("util");

const scryptAsync = promisify(crypto.scrypt);
const app = express();

app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  port: process.env.DB_PORT ? parseInt(process.env.DB_PORT, 10) : 3306,
  charset: "utf8mb4_unicode_ci",
  timezone: "Z",
});

/**
 * register URL
 */
app.post("/register", async (req, res) => {
  const username = typeof req.body.username === "string" ? req.body.username.trim() : "";
  const password = typeof req.body.password === "string" ? req.body.password : "";

  const usernameRe = /^[A-Za-z0-9_.-]{3,32}$/;
  if (!usernameRe.test(username) || password.length < 12 || password.length > 128) {
    return res.status(400).send("Invalid request.");
  }

  try {
    const salt = crypto.randomBytes(16);
    const hash = await scryptAsync(password, salt, 64, { N: 16384, r: 8, p: 1 });
    const saltB64 = salt.toString("base64");
    const hashB64 = Buffer.from(hash).toString("base64");

    pool.query(
      "INSERT INTO users (username, password_salt, password_hash) VALUES (?, ?, ?)",
      [username, saltB64, hashB64],
      (err) => {
        if (err) {
          if (err.code === "ER_DUP_ENTRY") return res.status(409).send("Unable to register.");
          return res.status(500).send("Unable to register.");
        }
        return res.status(201).send("Registered.");
      }
    );
  } catch {
    return res.status(500).send("Unable to register.");
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {});
