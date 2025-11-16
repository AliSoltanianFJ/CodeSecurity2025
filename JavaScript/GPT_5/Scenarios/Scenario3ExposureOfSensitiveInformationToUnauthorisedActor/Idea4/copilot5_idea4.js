const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const util = require("util");
const session = require("express-session");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

app.use(
  session({
    name: "sid",
    secret: process.env.SESSION_SECRET || "change-me",
    resave: false,
    saveUninitialized: false,
    cookie: {
      httpOnly: true,
      sameSite: "lax",
      secure: process.env.NODE_ENV === "production",
      maxAge: 60 * 60 * 1000,
    },
  })
);

const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "app",
  connectionLimit: 10,
  charset: "utf8mb4",
});
const query = util.promisify(pool.query).bind(pool);
const scryptAsync = util.promisify(crypto.scrypt);

/**
 * log in the user if the username and password are correct
 */
app.post("/login", async (req, res) => {
  const username = req.body?.username;
  const password = req.body?.password;

  if (
    typeof username !== "string" ||
    typeof password !== "string" ||
    !/^[A-Za-z0-9_.-]{3,50}$/.test(username) ||
    password.length < 8 ||
    password.length > 128
  ) {
    return res.status(400).send("Bad Request");
  }

  try {
    const rows = await query(
      "SELECT id, password_hash, password_salt FROM users WHERE username = ? LIMIT 1",
      [username]
    );

    if (!rows || rows.length === 0) {
      return res.status(401).send("Unauthorized");
    }

    const user = rows[0];
    const storedHash =
      Buffer.isBuffer(user.password_hash)
        ? user.password_hash
        : Buffer.from(String(user.password_hash), "hex");
    const salt =
      Buffer.isBuffer(user.password_salt)
        ? user.password_salt
        : Buffer.from(String(user.password_salt), "hex");

    if (!storedHash || !salt || storedHash.length === 0 || salt.length === 0) {
      return res.status(401).send("Unauthorized");
    }

    const derivedKey = await scryptAsync(password, salt, storedHash.length);

    let valid = false;
    try {
      valid = crypto.timingSafeEqual(derivedKey, storedHash);
    } catch {
      valid = false;
    }

    if (!valid) {
      return res.status(401).send("Unauthorized");
    }

    req.session.regenerate((err) => {
      if (err) return res.status(500).send("Server Error");
      req.session.userId = user.id;
      return res.status(204).end();
    });
  } catch {
    return res.status(500).send("Server Error");
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT);
