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
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: 10,
  charset: "utf8mb4",
});

app.post("/register", (req, res) => {
  const username = typeof req.body.username === "string" ? req.body.username.trim() : "";
  const password = typeof req.body.password === "string" ? req.body.password : "";

  if (!/^[A-Za-z0-9_.-]{3,32}$/.test(username) || password.length < 12 || password.length > 128) {
    return res.status(400).send("Invalid input");
  }

  const iterations = 210000;
  const keylen = 64;
  const digest = "sha512";
  const salt = crypto.randomBytes(16);

  crypto.pbkdf2(password, salt, iterations, keylen, digest, (err, derivedKey) => {
    if (err) return res.status(500).send("Unable to process request");

    const sql =
      "INSERT INTO users (username, password_hash, password_salt, password_algo, password_iter) VALUES (?, ?, ?, ?, ?)";
    const params = [username, derivedKey.toString("hex"), salt.toString("hex"), digest, iterations];

    pool.query(sql, params, (dbErr) => {
      if (dbErr) {
        if (dbErr.code === "ER_DUP_ENTRY") {
          return res.status(409).send("Account already exists");
        }
        return res.status(500).send("Unable to process request");
      }
      return res.status(201).send("Account created");
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
