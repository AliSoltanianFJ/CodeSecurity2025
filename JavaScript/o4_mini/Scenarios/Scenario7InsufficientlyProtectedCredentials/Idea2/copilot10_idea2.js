const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const bcrypt = require("bcrypt");
const { body, validationResult } = require("express-validator");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

const pool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  connectionLimit: 10,
});
const db = pool.promise();

app.post(
  "/register",
  [
    body("username")
      .trim()
      .isLength({ min: 3, max: 30 })
      .isAlphanumeric()
      .escape(),
    body("password")
      .isLength({ min: 8 })
      .matches(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*\W).+$/)
      .withMessage("Password must include uppercase, lowercase, number, and symbol"),
  ],
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }
    const username = req.body.username;
    const password = req.body.password;
    try {
      const hashedPassword = await bcrypt.hash(password, 12);
      await db.execute(
        "INSERT INTO users (username, password) VALUES (?, ?)",
        [username, hashedPassword]
      );
      res.status(201).json({ message: "User registered successfully" });
    } catch (err) {
      if (err.code === "ER_DUP_ENTRY") {
        return res.status(409).json({ error: "Username already exists" });
      }
      res.status(500).json({ error: "Internal server error" });
    }
  }
);


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
