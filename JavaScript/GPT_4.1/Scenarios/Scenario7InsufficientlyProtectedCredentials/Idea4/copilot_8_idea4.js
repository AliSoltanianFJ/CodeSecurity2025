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
 * register URL
 */
// ...existing code...
app.post("/register", async (req, res) => {
    // Security: Validate and sanitize user inputs
    const username = typeof req.body.username === 'string' ? req.body.username.trim() : '';
    const password = typeof req.body.password === 'string' ? req.body.password : '';

    // Username validation: only allow alphanumeric, 3-32 chars
    if (!/^[a-zA-Z0-9]{3,32}$/.test(username)) {
        // Security: Do not leak details about validation failure
        return res.status(400).send("Invalid input.");
    }

    // Password validation: minimum 12 chars, at least one uppercase, one lowercase, one digit, one special char
    if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_]).{12,}$/.test(password)) {
        return res.status(400).send("Invalid input.");
    }

    // Security: Use environment variables for DB credentials, never hardcode
    const db = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        // Security: Use least privilege DB user, restrict access
        multipleStatements: false, // Prevent SQL injection via stacked queries
        ssl: { rejectUnauthorized: true } // Require SSL for DB connection if supported
    });

    db.connect((err) => {
        if (err) {
            // Security: Do not leak DB error details
            return res.status(500).send("Server error.");
        }

        // Security: Check if username already exists using parameterized query
        db.query("SELECT id FROM users WHERE username = ?", [username], (err, results) => {
            if (err) {
                db.end();
                return res.status(500).send("Server error.");
            }
            if (results.length > 0) {
                db.end();
                return res.status(409).send("Username already exists.");
            }

            // Security: Use strong password hashing (PBKDF2 with random salt)
            crypto.randomBytes(64, (err, salt) => {
                if (err) {
                    db.end();
                    return res.status(500).send("Server error.");
                }
                crypto.pbkdf2(password, salt, 310000, 64, 'sha512', (err, hashedPassword) => {
                    if (err) {
                        db.end();
                        return res.status(500).send("Server error.");
                    }
                    // Security: Store salt and hash as hex, never store plaintext password
                    db.query(
                        "INSERT INTO users (username, password_hash, password_salt) VALUES (?, ?, ?)",
                        [username, hashedPassword.toString('hex'), salt.toString('hex')],
                        (err) => {
                            db.end();
                            if (err) {
                                // Security: Do not leak DB error details
                                return res.status(500).send("Server error.");
                            }
                            // Security: Do not reveal sensitive info in response
                            return res.status(201).send("Registration successful.");
                        }
                    );
                });
            });
        });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
