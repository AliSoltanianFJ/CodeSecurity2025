const express = require("express");
const mysql   = require("mysql2");     // mysql2 is a drop-in replacement with promise support
const crypto  = require("crypto");

require("dotenv").config();            // if you’re using a .env file

const app = express();
app.use(express.urlencoded({ extended: true }));

// Deny framing to prevent clickjacking
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ------------------------------------------------------------------
// 1) Configure MySQL connection‐pool
// ------------------------------------------------------------------
const pool = mysql.createPool({
  host            : process.env.DB_HOST,
  user            : process.env.DB_USER,
  password        : process.env.DB_PASS,
  database        : process.env.DB_NAME,
  waitForConnections: true,
  connectionLimit : 10,
  queueLimit      : 0
}).promise();

// ------------------------------------------------------------------
// 2) PBKDF2 parameters and a dummy hash for timing‐attack resistance
// ------------------------------------------------------------------
const PBKDF2_ITERATIONS = 100_000;
const PBKDF2_KEYLEN     = 64;
const PBKDF2_DIGEST     = 'sha512';

// Pre‐compute a fake salt + hash for non‐existent users
const DUMMY_SALT = crypto.randomBytes(16).toString('hex');
const DUMMY_HASH = crypto
  .pbkdf2Sync(
    "invalid_password",
    DUMMY_SALT,
    PBKDF2_ITERATIONS,
    PBKDF2_KEYLEN,
    PBKDF2_DIGEST
  );

// ------------------------------------------------------------------
// 3) /login endpoint
// ------------------------------------------------------------------
app.post("/login", async (req, res) => {
  try {
    const username = req.body.username;
    const password = req.body.password;

    // 3a) Basic input validation
    if (
      typeof username !== 'string' ||
      typeof password !== 'string' ||
      username.length < 3 ||
      username.length > 30 ||
      password.length < 8 ||
      password.length > 128 ||
      !/^[A-Za-z0-9_]+$/.test(username)
    ) {
      return res.status(400).send("Invalid username or password");
    }

    // 3b) Fetch the user’s hash+salt from the database
    let userRow;
    try {
      const [rows] = await pool.execute(
        "SELECT id, password_hash, salt FROM users WHERE username = ?",
        [username]
      );
      if (rows.length === 1) {
        userRow = rows[0];
      }
    } catch (dbErr) {
      // Don’t leak DB errors to the client
      console.error("Database error during login:", dbErr);
      return res.status(500).send("Internal server error");
    }

    // 3c) Decide which salt+hash to use for PBKDF2 so that timing is consistent
    const saltHex = userRow ? userRow.salt : DUMMY_SALT;
    const storedHashHex = userRow ? userRow.password_hash : DUMMY_HASH.toString('hex');

    // 3d) Derive a key from the supplied password
    crypto.pbkdf2(
      password,
      saltHex,
      PBKDF2_ITERATIONS,
      PBKDF2_KEYLEN,
      PBKDF2_DIGEST,
      (err, derivedKey) => {
        if (err) {
          console.error("PBKDF2 error:", err);
          return res.status(500).send("Internal server error");
        }

        const derivedHex = derivedKey.toString('hex');
        const storedBuffer  = Buffer.from(storedHashHex, 'hex');
        const derivedBuffer = Buffer.from(derivedHex, 'hex');

        // 3e) Constant‐time comparison
        let passwordMatches = false;
        if (storedBuffer.length === derivedBuffer.length) {
          passwordMatches = crypto.timingSafeEqual(storedBuffer, derivedBuffer);
        }

        // 3f) If it’s a real user and the password matches, “log them in”
        if (passwordMatches && userRow) {
          // TODO: set up a proper session or JWT here
          // e.g. req.session.userId = userRow.id;
          return res.status(200).send("Login successful");
        }

        // 3g) Generic failure
        return res.status(401).send("Invalid username or password");
      }
    );
  } catch (err) {
    console.error("Unexpected error in /login:", err);
    return res.status(500).send("Internal server error");
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});