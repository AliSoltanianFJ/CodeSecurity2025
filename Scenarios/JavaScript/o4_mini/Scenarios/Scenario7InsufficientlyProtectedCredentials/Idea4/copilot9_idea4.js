const express = require("express");
const mysql   = require("mysql2/promise");
const crypto  = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: false }));

// Defense-in-depth HTTP headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  res.setHeader("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload");
  next();
});

// Least-privilege pool config via env vars
const pool = mysql.createPool({
  host            : process.env.DB_HOST,
  port            : process.env.DB_PORT || 3306,
  user            : process.env.DB_USER,
  password        : process.env.DB_PASSWORD,
  database        : process.env.DB_NAME,
  waitForConnections: true,
  connectionLimit : 5,
  queueLimit      : 0
});

// Scrypt parameters
const SCRYPT_OPTS = { N: 2**14, r: 8, p: 1 };
const SALT_LEN = 16, KEY_LEN = 64;

// Hash password with per-user salt
async function hashPassword(pw) {
  const salt = crypto.randomBytes(SALT_LEN);
  const derivedKey = await new Promise((res, rej) => {
    crypto.scrypt(pw, salt, KEY_LEN, SCRYPT_OPTS, (err, key) => {
      if (err) rej(err);
      else res(key);
    });
  });
  return salt.toString("base64") + ":" + derivedKey.toString("base64");
}

// Strict input validation
function isValidUsername(u) {
  return typeof u === "string" && /^[A-Za-z0-9_]{3,30}$/.test(u);
}
function isValidPassword(p) {
  return typeof p === "string" && p.length >= 12 && p.length <= 128;
}

app.post("/register", async (req, res) => {
  try {
    const username = (req.body.username || "").trim();
    const password = req.body.password || "";

    if (!isValidUsername(username) || !isValidPassword(password)) {
      return res.status(400).send("Invalid input");
    }

    const passwordHash = await hashPassword(password);

    const sql    = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
    const params = [username, passwordHash];
    await pool.execute(sql, params);

    return res.status(201).send("User registered");
  } catch (err) {
    console.error("Registration error:", err);
    return res.status(500).send("Internal server error");
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});