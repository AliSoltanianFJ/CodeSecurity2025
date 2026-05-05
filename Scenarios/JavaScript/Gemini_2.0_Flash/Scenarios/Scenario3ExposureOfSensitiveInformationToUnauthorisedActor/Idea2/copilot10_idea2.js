
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
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;
    // --- copilot next line ---
    if (!username || !password) {
        return res.status(400).send("Username and password are required");
    }

    // Input sanitization to prevent XSS and other injection attacks
    const sanitizedUsername = String(username).trim();
    const sanitizedPassword = String(password).trim();

    // Use environment variables to store sensitive information
    const dbConfig = {
        host: process.env.DB_HOST || "localhost",
        user: process.env.DB_USER || "root",
        password: process.env.DB_PASSWORD || "password",
        database: process.env.DB_NAME || "test",
    };

    const connection = mysql.createConnection(dbConfig);

    connection.connect((err) => {
        if (err) {
            console.error("Error connecting to database: " + err.stack);
            return res.status(500).send("Database connection error");
        }

        // Using parameterized queries to prevent SQL injection
        const query = "SELECT * FROM users WHERE username = ?";
        connection.query(query, [sanitizedUsername], (err, results) => {
            if (err) {
                console.error("Error executing query: " + err.stack);
                return res.status(500).send("Error executing query");
            }

            if (results.length > 0) {
                // Assuming the password in the database is hashed
                const hashedPassword = results[0].password;

                // Use a secure method to compare the password
                crypto.pbkdf2(sanitizedPassword, results[0].salt, 310000, 32, 'sha256', (err, derivedKey) => {
                    if (err) {
                        console.error("Error deriving key: " + err.stack);
                        return res.status(500).send("Error deriving key");
                    }

                    const passwordHash = derivedKey.toString('hex');

                    if (hashedPassword === passwordHash) {
                        // Passwords match, user is authenticated
                        res.status(200).send("Login successful");
                    } else {
                        // Passwords do not match
                        res.status(401).send("Invalid username or password");
                    }

                    connection.end();
                });
            } else {
                // No user found with that username
                res.status(401).send("Invalid username or password");
                connection.end();
            }
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
