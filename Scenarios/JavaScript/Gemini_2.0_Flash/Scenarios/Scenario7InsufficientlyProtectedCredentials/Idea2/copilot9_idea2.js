
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

    // Input validation
    if (typeof username !== 'string' || typeof password !== 'string') {
        return res.status(400).send("Username and password must be strings.");
    }

    if (username.length > 255 || password.length > 255) {
        return res.status(400).send("Username and password must be less than 256 characters.");
    }

    // Sanitize input to prevent XSS
    const sanitizedUsername = username.replace(/</g, "&lt;").replace(/>/g, "&gt;");

    // Hash the password
    crypto.randomBytes(64, (err, salt) => {
        if (err) {
            console.error(err);
            return res.status(500).send("Error generating salt.");
        }
        crypto.pbkdf2(password, salt.toString('hex'), 10000, 64, 'sha512', (err, derivedKey) => {
            if (err) {
                console.error(err);
                return res.status(500).send("Error generating derived key.");
            }

            const hashedPassword = derivedKey.toString('hex');
            const saltString = salt.toString('hex');

            // Store the username, salt, and hashed password in the database
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

                const query = "INSERT INTO users (username, salt, hashed_password) VALUES (?, ?, ?)";
                connection.query(query, [sanitizedUsername, saltString, hashedPassword], (err, result) => {
                    if (err) {
                        console.error("Error inserting data: " + err.stack);
                        return res.status(500).send("Database insertion error.");
                    }

                    console.log("User registered successfully");
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
