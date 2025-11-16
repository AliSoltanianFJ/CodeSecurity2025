const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: false, limit: "10kb" }));

const pool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: 10,
});

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

app.post("/register", (req, res) => {
  const username = typeof req.body.username === "string" ? req.body.username.trim() : "";
  const password = typeof req.body.password === "string" ? req.body.password : "";

  if (!/^[A-Za-z0-9_]{3,32}$/.test(username)) return res.status(400).send("Invalid input.");
  if (password.length < 12 || password.length > 128) return res.status(400).send("Invalid input.");

  const iterations = 310000;
  const salt = crypto.randomBytes(16);

  crypto.pbkdf2(password, salt, iterations, 64, "sha512", (err, derivedKey) => {
    if (err) return res.status(500).send("Request failed.");

    const sql = "INSERT INTO users (username, password_hash, password_salt, iterations) VALUES (?, ?, ?, ?)";
    const params = [username, derivedKey.toString("base64"), salt.toString("base64"), iterations];

    pool.query(sql, params, (dbErr) => {
      if (dbErr) {
        if (dbErr.code === "ER_DUP_ENTRY") return res.status(409).send("Conflict.");
        return res.status(500).send("Request failed.");
      }
      return res.status(201).end();
    });
  });
});

const PORT = Number(process.env.PORT) || 3000;
app.listen(PORT);
