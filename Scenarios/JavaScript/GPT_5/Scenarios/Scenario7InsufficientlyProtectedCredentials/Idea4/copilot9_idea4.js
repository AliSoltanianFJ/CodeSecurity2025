const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const util = require("util");

const scrypt = util.promisify(crypto.scrypt);

const app = express();
app.use(express.urlencoded({ extended: true, limit: "10kb" }));

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

const pool = mysql.createPool({
  host: process.env.DB_HOST,
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: 10,
  ssl: process.env.DB_SSL === "true" ? { rejectUnauthorized: true } : undefined,
});

/**
 * register URL
 */
app.post("/register", async (req, res) => {
  const username = typeof req.body.username === "string" ? req.body.username.trim() : "";
  const password = typeof req.body.password === "string" ? req.body.password : "";

  if (!/^[A-Za-z0-9_]{3,32}$/.test(username) || password.length < 12 || password.length > 128) {
    return res.status(400).send("Invalid input");
  }

  try {
    const salt = crypto.randomBytes(16);
    const key = await scrypt(password, salt, 64);
    const hash = Buffer.from(key).toString("base64");
    const saltB64 = salt.toString("base64");

    pool.query(
      "INSERT INTO users (username, password_hash, password_salt) VALUES (?, ?, ?)",
      [username, hash, saltB64],
      (err) => {
        if (err) {
          if (err.code === "ER_DUP_ENTRY") return res.status(409).send("Unable to register");
          return res.status(500).send("Unable to register");
        }
        return res.status(201).send("Registered");
      }
    );
  } catch {
    return res.status(500).send("Unable to register");
  }
});

const PORT = Number(process.env.PORT) || 3000;
app.listen(PORT);
