const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const util = require("util");

const scrypt = util.promisify(crypto.scrypt);

const app = express();
app.use(express.urlencoded({ extended: false, limit: "10kb" }));

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

const pool = mysql.createPool({
  host: process.env.DB_HOST,
  port: Number(process.env.DB_PORT) || 3306,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: 10,
  multipleStatements: false,
  timezone: "Z",
});

app.post("/register", async (req, res) => {
  const username = typeof req.body.username === "string" ? req.body.username.trim() : "";
  const password = typeof req.body.password === "string" ? req.body.password : "";

  if (!/^[A-Za-z0-9._-]{3,32}$/.test(username) || password.length < 12 || password.length > 128) {
    return res.status(400).send("Bad Request");
  }

  try {
    const salt = crypto.randomBytes(16);
    const N = 16384, r = 8, p = 1;
    const key = await scrypt(password, salt, 64, { N, r, p, maxmem: 32 * 1024 * 1024 });
    const hash = `scrypt$${N}$${r}$${p}$${salt.toString("base64")}$${Buffer.from(key).toString("base64")}`;

    pool.query(
      "INSERT INTO users (username, password_hash) VALUES (?, ?)",
      [username, hash],
      (err) => {
        if (err) {
          if (err.code === "ER_DUP_ENTRY") return res.status(409).send("Conflict");
          return res.status(500).send("Internal Server Error");
        }
        return res.status(201).send("Created");
      }
    );
  } catch {
    return res.status(500).send("Internal Server Error");
  }
});

const PORT = Number(process.env.PORT) || 3000;
app.listen(PORT);
