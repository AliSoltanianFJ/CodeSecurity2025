const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
app.post("/login", (req, res) => {
    // Input validation and sanitization
    const username = typeof req.body.username === 'string' ? req.body.username.trim() : '';
    const password = typeof req.body.password === 'string' ? req.body.password : '';

    // Basic input length checks to prevent buffer overflow and DoS
    if (
        !username ||
        !password ||
        username.length > 64 ||
        password.length > 128 ||
        !/^[a-zA-Z0-9_.-]+$/.test(username)
    ) {
        return res.status(400).send("Invalid input.");
    }

    // Use environment variables for DB credentials
    const db = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        // Prevent insecure deserialization by disabling multiple statements
        multipleStatements: false
    });

    db.connect((err) => {
        if (err) {
            // Do not leak internal errors
            return res.status(500).send("Server error.");
        }

        // Use parameterized queries to prevent SQL injection
        db.query(
            "SELECT id, username, password_hash, salt FROM users WHERE username = ? LIMIT 1",
            [username],
            (err, results) => {
                if (err) {
                    db.end();
                    return res.status(500).send("Server error.");
                }
                if (results.length === 0) {
                    db.end();
                    // Generic message to prevent user enumeration
                    return res.status(401).send("Invalid username or password.");
                }

                const user = results[0];

                // Use a secure hash (e.g., pbkdf2) for password verification
                crypto.pbkdf2(
                    password,
                    user.salt,
                    100000,
                    64,
                    'sha512',
                    (err, derivedKey) => {
                        db.end();
                        if (err) {
                            return res.status(500).send("Server error.");
                        }
                        if (
                            crypto.timingSafeEqual(
                                Buffer.from(user.password_hash, 'hex'),
                                derivedKey
                            )
                        ) {
                            // Do not expose sensitive info in the response
                            // Set a secure session or token here (not implemented for brevity)
                            return res.status(200).send("Login successful.");
                        } else {
                            return res.status(401).send("Invalid username or password.");
                        }
                    }
                );
            }
        );
    });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
