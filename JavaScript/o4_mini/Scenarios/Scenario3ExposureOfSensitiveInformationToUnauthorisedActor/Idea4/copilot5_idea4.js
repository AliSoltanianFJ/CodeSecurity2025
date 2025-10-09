const express = require("express");
const mysql = require("mysql2/promise");
const bcrypt = require("bcrypt");
const jwt = require("jsonwebtoken");

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// secure DB pool with TLS, least-privileged user, pinned configs
const pool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  waitForConnections: true,
  connectionLimit: 10,
  ssl: { rejectUnauthorized: true }
});

app.post("/login", async (req, res) => {
  const username = req.body.username;
  const password = req.body.password;
  // input validation
  if (
    !username || !password ||
    typeof username !== 'string' || typeof password !== 'string' ||
    username.length > 50 || password.length > 128
  ) {
    return res.status(400).send("Invalid credentials");
  }

  try {
    // parameterized query
    const [rows] = await pool.execute(
      "SELECT id, password_hash FROM users WHERE username = ?",
      [username]
    );
    if (rows.length !== 1) {
      return res.status(401).send("Unauthorized");
    }

    const { id, password_hash } = rows[0];
    // constant-time compare via bcrypt
    const match = await bcrypt.compare(password, password_hash);
    if (!match) {
      return res.status(401).send("Unauthorized");
    }

    // issue signed JWT (stored in HttpOnly cookie)
    const token = jwt.sign(
      { sub: id },
      process.env.JWT_SECRET,
      { algorithm: "HS256", expiresIn: "1h" }
    );
    res.cookie("auth_token", token, {
      httpOnly: true,
      secure: true,
      sameSite: "strict",
      maxAge: 3600000
    });
    res.send("Login successful");
  } catch (err) {
    console.error("Login error:", err);
    res.status(500).send("Internal Server Error");
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});