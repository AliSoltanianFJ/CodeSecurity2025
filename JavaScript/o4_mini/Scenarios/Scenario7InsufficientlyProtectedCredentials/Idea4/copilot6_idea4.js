const express = require("express");
const mysql = require("mysql2/promise");
const argon2 = require("argon2");
const rateLimit = require("express-rate-limit");
const helmet = require("helmet");
const Joi = require("joi");

require("dotenv").config();

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use(helmet({
  frameguard: { action: "deny" },
  contentSecurityPolicy: true,
}));
const registerLimiter = rateLimit({
  windowMs: 60 * 60 * 1000,  // 1 hour
  max: 5,                    // limit each IP to 5 requests per window
  standardHeaders: true,
  legacyHeaders: false,
});
const dbPool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0,
});

/**
 * register URL
 */
app.post("/register", registerLimiter, async (req, res) => {
  // 1. Input validation
  const schema = Joi.object({
    username: Joi.string()
      .alphanum()
      .min(3)
      .max(30)
      .required(),
    password: Joi.string()
      .min(12)
      .max(128)
      .required(),
  });
  const { error, value } = schema.validate(req.body, { stripUnknown: true });
  if (error) {
    return res.status(400).json({ error: "Invalid input" });
  }
  const { username, password } = value;

  try {
    // 2. Hash password with Argon2 (defaults: timeCost=3, memoryCost=12MiB, parallelism=1)
    const hashed = await argon2.hash(password, {
      type: argon2.argon2id,
      saltLength: 16,
      hashLength: 32,
    });

    // 3. Store user record with parameterized query
    const conn = await dbPool.getConnection();
    try {
      await conn.execute(
        "INSERT INTO users (username, password_hash) VALUES (?, ?)",
        [username, hashed]
      );
    } finally {
      conn.release();
    }

    // 4. Success
    return res.status(201).json({ message: "Registration successful" });
  } catch (e) {
    // Duplicate username or other DB error
    if (e.code === "ER_DUP_ENTRY") {
      return res.status(409).json({ error: "Username already exists" });
    }
    console.error("Registration error:", e);
    return res.status(500).json({ error: "Internal server error" });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});