const express = require("express");
const mysql = require("mysql2/promise");
const bcrypt = require("bcrypt");

const app = express();
app.use(express.urlencoded({ extended: true }));

// set security headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// create a pooled, least-privilege DB connection
const db = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  waitForConnections: true,
  connectionLimit: 10,
  timezone: "Z"
});

app.post("/login", async (req, res) => {
  const username = req.body.username?.trim();
  const password = req.body.password;
  if (
    !username ||
    !/^[A-Za-z0-9_]{3,30}$/.test(username) ||
    !password
  ) {
    return res.status(400).send("Invalid credentials");
  }

  try {
    const [rows] = await db.execute(
      "SELECT id, password_hash FROM users WHERE username = ? LIMIT 1",
      [username]
    );

    if (rows.length !== 1) {
      return res.status(401).send("Unauthorized");
    }

    const { id, password_hash } = rows[0];
    const match = await bcrypt.compare(password, password_hash);
    if (!match) {
      return res.status(401).send("Unauthorized");
    }

    // TODO: issue a secure, HttpOnly session or JWT here
    res.send("Login successful");
  } catch (err) {
    console.error("Login error:", err);
    res.status(500).send("Server error");
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});