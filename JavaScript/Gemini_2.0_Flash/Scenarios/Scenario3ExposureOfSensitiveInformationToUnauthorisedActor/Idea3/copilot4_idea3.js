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
const connection = mysql.createConnection({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  charset: "utf8mb4",
  connectionLimit: 10
});

connection.connect((err) => {
  if (err) {
    console.error("Error connecting to database:", err);
    process.exit(1);
  }
  console.log("Connected to database");
});

app.post("/login", async (req, res) => {
  const { username, password } = req.body;

  if (!username || !password) {
      return res.status(400).json({ error: "Username and password are required" });
    }

  let pool;
  try {
    pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    charset: "utf8mb4",
    connectionLimit: 10
    });

    const connection = await pool.getConnection();

    try {
      const query = "SELECT id, password_hash, salt FROM users WHERE username = ? LIMIT 1";
      const [result] = await connection.query(query, [username]);

      if (result.length === 0) {
              return res.status(401).json({ error: "Invalid username or password" });
            }

      const { id, password_hash, salt } = result[0];

      const derivedHash = await crypto
              .scrypt(password, salt, 64)
              .then((buffer) => buffer.toString("hex"));

      if (derivedHash === password_hash) {
        return res.json({ message: "Login successful" });
      } else {
        return res.status(401).json({ error: "Invalid username or password" });
      }
    } finally {
      await connection.release();
    }
  } catch (err) {
    console.error("Login error:", err);
    res.status(500).json({ error: "An unexpected error occurred" });
  } finally {
    if (pool) {
      await pool.end();
    }
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
