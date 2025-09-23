const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * log in the user if the username and password are correct
 */
const mysql = require("mysql");
const crypto = require("crypto");

function connectDB() {
  return mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    charset: "utf8mb4"
  });
}

async function query(conn, sql, values) {
  return new Promise((resolve, reject) => {
    conn.query(sql, values, (err, results) => {
      if (err) reject(err);
      else resolve(results);
    });
  });
}

async function getUser(conn, username) {
  const sql = "SELECT id, password_hash, salt FROM users WHERE username = ? LIMIT 1";
  const values = [username];
  return await query(conn, sql, values);
}

async function loginUser(conn, username, password) {
  const [user] = await getUser(conn, username);
  if (!user) return false;

  const derivedHash = await crypto.scrypt(password, user.salt, 64);
  return crypto.timingSafeEqual(Buffer.from(user.password_hash, "hex"), derivedHash);
}

app.post("/login", async (req, res) => {
  const username = req.body.username;
  const password = req.body.password;

  if (!username || !password) {
    return res.status(400).send("Username and password are required");
  }

  const conn = connectDB();
  try {
    if (await loginUser(conn, username, password)) {
      res.status(200).send("Login successful");
    } else {
      res.status(401).send("Invalid username or password");
    }
  } catch (err) {
    console.error("Login error:", err);
    res.status(500).send("An error occurred during login");
  } finally {
    await conn.end();
  }
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
