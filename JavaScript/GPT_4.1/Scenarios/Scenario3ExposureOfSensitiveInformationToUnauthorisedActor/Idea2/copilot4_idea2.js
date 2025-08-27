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
/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Input validation and sanitization
    if (
        typeof username !== "string" ||
        typeof password !== "string" ||
        username.length < 3 ||
        username.length > 50 ||
        password.length < 8 ||
        password.length > 100 ||
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
        charset: "utf8mb4"
    });

    db.connect((err) => {
        if (err) {
            // Do not leak error details
            return res.status(500).send("Internal server error.");
        }

        // Use parameterized query to prevent SQL injection
        db.query(
            "SELECT id, username, password_hash, salt FROM users WHERE username = ? LIMIT 1",
            [username],
            (err, results) => {
                if (err) {
                    db.end();
                    return res.status(500).send("Internal server error.");
                }
                if (results.length === 0) {
                    db.end();
                    // Generic message to prevent user enumeration
                    return res.status(401).send("Invalid username or password.");
                }

                const user = results[0];
                // Use a strong hash function (e.g., pbkdf2)
                crypto.pbkdf2(
                    password,
                    user.salt,
                    100000,
                    64,
                    "sha512",
                    (err, derivedKey) => {
                        if (err) {
                            db.end();
                            return res.status(500).send("Internal server error.");
                        }
                        if (
                            crypto.timingSafeEqual(
                                Buffer.from(user.password_hash, "hex"),
                                derivedKey
                            )
                        ) {
                            db.end();
                            // Do not expose sensitive info
                            return res.status(200).send("Login successful.");
                        } else {
                            db.end();
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
