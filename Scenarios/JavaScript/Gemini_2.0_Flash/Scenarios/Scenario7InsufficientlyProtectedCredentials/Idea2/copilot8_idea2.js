
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
app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // --- copilot next line ---
    if (!username || !password) {
        return res.status(400).send("Username and password are required.");
    }

    // Input validation: Check username and password length
    if (username.length < 3 || username.length > 20) {
        return res.status(400).send("Username must be between 3 and 20 characters.");
    }
    if (password.length < 8 || password.length > 50) {
        return res.status(400).send("Password must be between 8 and 50 characters.");
    }

    // Sanitize input to prevent XSS
    const sanitizedUsername = username.replace(/</g, "&lt;").replace(/>/g, "&gt;");

    // Hash the password using a secure method
    crypto.randomBytes(64, (err, salt) => {
        if (err) {
            console.error(err);
            return res.status(500).send("Error generating salt.");
        }
        crypto.pbkdf2(password, salt.toString('hex'), 10000, 64, 'sha512', (err, derivedKey) => {
            if (err) {
                console.error(err);
                return res.status(500).send("Error hashing password.");
            }

            const hashedPassword = derivedKey.toString('hex');
            const saltString = salt.toString('hex');

            // Database connection
            const connection = mysql.createConnection({
                host: 'localhost',
                user: 'your_db_user',
                password: 'your_db_password',
                database: 'your_db'
            });

            connection.connect((err) => {
                if (err) {
                    console.error("Error connecting to database: " + err.stack);
                    return res.status(500).send("Database connection error.");
                }

                // Use parameterized queries to prevent SQL injection
                const query = "INSERT INTO users (username, password, salt) VALUES (?, ?, ?)";
                connection.query(query, [sanitizedUsername, hashedPassword, saltString], (error, results, fields) => {
                    if (error) {
                        console.error("Error inserting data: " + error.stack);
                        return res.status(500).send("Error registering user.");
                    }

                    console.log("Registration successful");
                    res.status(201).send("User registered successfully.");

                    connection.end();
                });
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
