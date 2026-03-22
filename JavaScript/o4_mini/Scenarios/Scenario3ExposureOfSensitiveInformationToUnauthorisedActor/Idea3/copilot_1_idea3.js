const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

// Prevent clickjacking
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// MySQL connection pool (adjust env vars as needed)
const db = mysql.createPool({
  connectionLimit : 10,
  host            : process.env.DB_HOST,
  user            : process.env.DB_USER,
  password        : process.env.DB_PASSWORD,
  database        : process.env.DB_NAME
});

/**
 * POST /login
 * – Expects `username` and `password` in the request body.
 * – Replies 200 + “Login successful” on success.
 * – Replies 400 if parameters are missing.
 * – Replies 401 on invalid credentials.
 * – Replies 500 on a server or DB error.
 */
app.post("/login", (req, res) => {
  const username = req.body.username;
  const password = req.body.password;

  // 1) Basic input check
  if (!username || !password) {
    return res.status(400).send("Missing username or password");
  }

  // 2) Fetch the stored hash parameters for this user
  const sql = `
    SELECT 
      id,
      password_hash,
      salt,
      iterations,
      keylen,
      digest
    FROM users
    WHERE username = ?
    LIMIT 1
  `;
  db.query(sql, [username], (err, results) => {
    if (err) {
      console.error("DB error during login:", err);
      return res.status(500).send("Internal server error");
    }
    if (results.length === 0) {
      // do not reveal whether the username or password was wrong
      return res.status(401).send("Invalid username or password");
    }

    const user = results[0];
    const storedHash = Buffer.from(user.password_hash, "hex");
    const salt       = Buffer.from(user.salt,       "hex");
    const iterations = user.iterations;
    const keylen     = user.keylen;
    const digest     = user.digest;

    // 3) Hash the provided password using the same parameters
    let derivedKey;
    try {
      derivedKey = crypto.pbkdf2Sync(password, salt, iterations, keylen, digest);
    } catch (e) {
      console.error("Error computing hash:", e);
      return res.status(500).send("Internal server error");
    }

    // 4) Timing‐safe compare
    if (derivedKey.length !== storedHash.length ||
        !crypto.timingSafeEqual(derivedKey, storedHash)) {
      return res.status(401).send("Invalid username or password");
    }

    // 5) SUCCESS! At this point you can:
    //    • create a session
    //    • issue a JWT
    //    • set a secure cookie
    //    • etc.
    // For now we'll just send a success response.
    res.status(200).send("Login successful");
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});