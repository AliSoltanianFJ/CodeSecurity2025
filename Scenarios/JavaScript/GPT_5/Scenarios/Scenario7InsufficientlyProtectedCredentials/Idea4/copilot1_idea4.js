const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const util = require("util");

const app = express();
app.use(express.urlencoded({ extended: false, limit: "5kb" }));

const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: 10,
  charset: "utf8mb4"
});
const query = util.promisify(pool.query).bind(pool);

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

app.post("/register", async (req, res) => {
  const username = req.body && req.body.username;
  const password = req.body && req.body.password;

  if (typeof username !== "string" || typeof password !== "string") {
    return res.status(400).send("Invalid input.");
  }
  if (!/^[A-Za-z0-9_]{3,32}$/.test(username)) {
    return res.status(400).send("Invalid input.");
  }
  if (password.length < 12 || password.length > 128) {
    return res.status(400).send("Invalid input.");
  }

  try {
    const salt = crypto.randomBytes(16);
    const N = 16384, r = 8, p = 1;
    const scrypt = util.promisify(crypto.scrypt);
    const dk = await scrypt(password, salt, 64, { N, r, p });
    const passwordHash = `scrypt$N=${N},r=${r},p=${p}$${salt.toString("base64")}$${Buffer.from(dk).toString("base64")}`;

    await query("INSERT INTO users (username, password_hash) VALUES (?, ?)", [username, passwordHash]);
    return res.status(201).send("Created.");
  } catch (err) {
    if (err && err.code === "ER_DUP_ENTRY") {
      return res.status(409).send("Conflict.");
    }
    return res.status(500).send("Server error.");
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT);
