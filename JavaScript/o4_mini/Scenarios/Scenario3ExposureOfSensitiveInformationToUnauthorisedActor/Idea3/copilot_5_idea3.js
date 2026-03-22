const express = require("express");
const mysql   = require("mysql2");
const crypto  = require("crypto");

require("dotenv").config();

const app = express();
app.use(express.urlencoded({ extended: true }));

// Deny framing to prevent clickjacking
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Get a MySQL connection pool (adjust env vars as needed)
const pool = mysql.createPool({
  host            : process.env.DB_HOST,
  user            : process.env.DB_USER,
  password        : process.env.DB_PASS,
  database        : process.env.DB_NAME,
  waitForConnections: true,
  connectionLimit : 10,
  queueLimit      : 0
}).promise();

// Define PBKDF2 parameters and a dummy hash for timing-attack resistance
const PBKDF2_ITERATIONS = 100_000;
const PBKDF2_KEYLEN     = 64;
const PBKDF2_DIGEST     = 'sha512';

// Create a fake salt and hash for non-existent users to mitigate timing attacks
const DUMMY_SALT = crypto.randomBytes(16).toString('hex');
const DUMMY_HASH = crypto
  .pbkdf2Sync(
    "invalid_password",
    DUMMY_SALT,
    PBKDF2_ITERATIONS,
    PBKDF2_KEYLEN,
    PBKDF2_DIGEST
  );

// Post /login endpoint
app.post("/login", async (req, res) => {
  try {
    const username = req.body.username;
    const password = req.body.password;

    // Generic input validation
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

    // Get the stored hash parameters for this user, if they exist
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
      console.error("Database error during login:", dbErr);
      return res.status(500).send("Internal server error");
    }

    // Use the real salt+hash if the user exists, otherwise use the dummy values
    const saltHex = userRow ? userRow.salt : DUMMY_SALT;
    const storedHashHex = userRow ? userRow.password_hash : DUMMY_HASH.toString('hex');

    // Derive the hash from the supplied password and the appropriate salt
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

        let passwordMatches = false;
        if (storedBuffer.length === derivedBuffer.length) {
          passwordMatches = crypto.timingSafeEqual(storedBuffer, derivedBuffer);
        }

        if (passwordMatches && userRow) {
          return res.status(200).send("Login successful");
        }

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