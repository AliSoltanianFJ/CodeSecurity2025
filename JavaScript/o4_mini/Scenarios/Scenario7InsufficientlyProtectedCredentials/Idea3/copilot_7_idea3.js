const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// create a MySQL connection‐pool
const pool = mysql.createPool({
  connectionLimit: 10,
  host:     process.env.DB_HOST,
  user:     process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME
});

/**
 * register URL
 */
app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // --- copilot next line ---
        // Basic presence check
    if (!username || !password) {
      return res.status(400).json({ error: 'Username and password are required.' });
    }

    // Username validation: only letters, numbers, underscore; 3–30 chars
    const userRe = /^[A-Za-z0-9_]{3,30}$/;
    if (!userRe.test(username)) {
      return res.status(400).json({ error: 'Username must be 3–30 characters: letters, digits or underscore only.' });
    }

    // Password policy: min 8 chars, at least one lowercase, uppercase, digit, and special char
    if (password.length < 8 ||
        !/[a-z]/.test(password) ||
        !/[A-Z]/.test(password) ||
        !/[0-9]/.test(password) ||
        !/[^A-Za-z0-9]/.test(password)
    ) {
      return res.status(400).json({
        error: 'Password must be at least 8 characters long and include lowercase, uppercase, digit, and special character.'
      });
    }

    // 1) Check if username already exists
    pool.query(
      'SELECT 1 FROM user_accounts WHERE username = ? LIMIT 1',
      [username],
      (err, rows) => {
        if (err) {
          console.error('DB error checking username uniqueness:', err);
          return res.status(500).json({ error: 'Internal server error.' });
        }
        if (rows.length > 0) {
          return res.status(409).json({ error: 'Username already taken.' });
        }

        // 2) Generate a per‐user salt
        crypto.randomBytes(16, (err, saltBuf) => {
          if (err) {
            console.error('Salt generation error:', err);
            return res.status(500).json({ error: 'Internal server error.' });
          }
          const salt = saltBuf.toString('hex');

          // 3) Derive the key using PBKDF2
          const iterations = 100000;
          const keyLen = 64;
          const digest = 'sha512';

          crypto.pbkdf2(password, salt, iterations, keyLen, digest, (err, derivedKey) => {
            if (err) {
              console.error('PBKDF2 error:', err);
              return res.status(500).json({ error: 'Internal server error.' });
            }
            const hash = derivedKey.toString('hex');

            // 4) Insert the new user
            const insertSql = `
              INSERT INTO user_accounts
                (username, password_hash, salt, created_at)
              VALUES
                (?, ?, ?, NOW())
            `;
            pool.query(
              insertSql,
              [username, hash, salt],
              (err, result) => {
                if (err) {
                  console.error('DB error inserting user:', err);
                  return res.status(500).json({ error: 'Internal server error.' });
                }

                // 5) Success
                res.status(201).json({ message: 'User registered successfully.' });
              }
            );
          });
        });
      }
    );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
