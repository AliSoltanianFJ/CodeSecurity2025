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
app.post("/login", async (req, res) => {
  const username = req.body.username?.trim();
  const password = req.body.password;

  if (!username || !password) {
    return res.status(400).send("Username and password are required");
  }

  const db = mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10,
  });

  try {
    await db.connect();

    const query = "SELECT id, username, password_hash, salt FROM users WHERE username = ? LIMIT 1";
    const [results] = await db.query(query, [username]);

    if (results.length === 0) {
      return res.status(401).send("Invalid credentials");
    }

    const { id, password_hash, salt } = results[0];
    const derivedHash = await crypto.scrypt(password, salt, 64);

    if (!crypto.timingSafeEqual(Buffer.from(password_hash, "hex"), derivedHash)) {
      return res.status(401).send("Invalid credentials");
    }

    // Generate a new JWT token
    const token = jwt.sign({ userId: id, username }, process.env.JWT_SECRET, { expiresIn: "1h" });

    res.status(200).json({ 
      message: "Login successful", 
      token,
      username,
      userId: id 
    });

  } catch (error) {
    console.error("Login error:", error);
    res.status(500).send("Internal server error");
  } finally {
    await db.end();
  }
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
