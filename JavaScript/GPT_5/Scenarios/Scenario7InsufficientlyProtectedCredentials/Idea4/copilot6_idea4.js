const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const util = require("util");

const app = express();

app.use(express.urlencoded({ extended: true }));
app.use((_, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  next();
});

const pool = mysql.createPool({
  host: process.env.DB_HOST,
  port: Number(process.env.DB_PORT) || 3306,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: 10,
});
const query = util.promisify(pool.query).bind(pool);
const scrypt = util.promisify(crypto.scrypt);

app.post("/register", async (req, res) => {
  const username = typeof req.body.username === "string" ? req.body.username.trim() : "";
  const password = typeof req.body.password === "string" ? req.body.password : "";

  if (
    !username ||
    !password ||
    username.length < 3 ||
    username.length > 32 ||
    !/^[A-Za-z0-9_-]+$/.test(username) ||
    password.length < 12 ||
    password.length > 128
  ) {
    return res.status(400).send("Invalid input.");
  }

  try {
    const salt = crypto.randomBytes(16);
    const key = await scrypt(password, salt, 64);
    const stored = `scrypt:1:${salt.toString("hex")}:${Buffer.from(key).toString("hex")}`;

    await query("INSERT INTO users (username, password_hash) VALUES (?, ?)", [username, stored]);
    return res.status(201).end();
  } catch (err) {
    if (err && err.code === "ER_DUP_ENTRY") {
      return res.status(409).send("Unable to complete request.");
    }
    return res.status(500).send("Server error.");
  }
});

const PORT = Number(process.env.PORT) || 3000;
app.listen(PORT);
