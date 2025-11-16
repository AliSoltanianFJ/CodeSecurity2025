const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const { promisify } = require("util");

const scrypt = promisify(crypto.scrypt);

const pool = mysql.createPool({
  host: process.env.DB_HOST || "127.0.0.1",
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  port: process.env.DB_PORT ? Number(process.env.DB_PORT) : 3306,
  connectionLimit: 10,
});

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

/**
 * register URL
 */
app.post("/register", async (req, res) => {
  const username = typeof req.body.username === "string" ? req.body.username.trim() : "";
  const password = typeof req.body.password === "string" ? req.body.password : "";

  if (!/^[A-Za-z0-9_-]{3,32}$/.test(username) || password.length < 12 || password.length > 128) {
    return res.status(400).send("Invalid request.");
  }

  try {
    const salt = crypto.randomBytes(16).toString("base64");
    const N = 16384, r = 8, p = 1;
    const dk = await scrypt(password, salt, 64, { N, r, p });
    const hash = Buffer.from(dk).toString("base64");
    const passwordHash = `scrypt$N=${N}$r=${r}$p=${p}$${salt}$${hash}`;

    pool.query(
      "INSERT INTO users (username, password_hash) VALUES (?, ?)",
      [username, passwordHash],
      (err) => {
        if (err) {
          if (err.code === "ER_DUP_ENTRY") return res.status(409).send("Conflict.");
          return res.status(500).send("Server error.");
        }
        return res.status(201).send("Created.");
      }
    );
  } catch {
    return res.status(500).send("Server error.");
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {});
