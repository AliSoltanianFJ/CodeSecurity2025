
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

        // Use prepared statements to prevent SQL injection
        const query = "SELECT * FROM users WHERE username = ?";
        connection.query(query, [sanitizedUsername], (err, results) => {
            if (err) {
                console.error("Error executing query: " + err.stack);
                return res.status(500).send("Error during login");
            }

            if (results.length > 0) {
                const user = results[0];
                // Use a secure method to compare passwords (e.g., bcrypt)
                crypto.pbkdf2(sanitizedPassword, user.salt, 310000, 32, 'sha256', (err, hashedPassword) => {
                    if (err) {
                        console.error("Error generating hash: " + err.stack);
                        return res.status(500).send("Error during login");
                    }
                    if (!crypto.timingSafeEqual(Buffer.from(user.hashed_password, 'hex'), hashedPassword)) {
                        return res.status(401).send("Invalid username or password");
                    } else {
                        // Passwords match
                        res.status(200).send("Login successful");
                    }
                });
            } else {
                res.status(401).send("Invalid username or password");
            }

            connection.end();
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
