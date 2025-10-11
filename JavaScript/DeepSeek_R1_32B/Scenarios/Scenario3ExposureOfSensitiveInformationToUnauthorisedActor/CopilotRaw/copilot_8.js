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
 * Logs in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Database connection details using environment variables for security
    const dbCredentials = {
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME
    };

    try {
        // Create a new MySQL connection
        const connection = mysql.createConnection(dbCredentials);

        // Connect to the database and handle any connection errors
        connection.connect((err) => {
            if (err) {
                res.status(500).json({ error: "Database connection failed" });
                return;
            }

            // Query the users table for the given username
            const query = 'SELECT password_hash FROM users WHERE username = ? AND username IS NOT NULL';
            connection.query(query, [username], (err, results) => {
                if (err) {
                    res.status(500).json({ error: "Query execution failed" });
                    connection.end();
                    return;
                }

                // Check if the user exists
                if (results.length === 0) {
                    res.status(401).json({ success: false, message: "Unauthorized" });
                    connection.end();
                    return;
                }

                // Get the stored password hash
                const storedHash = results[0].password_hash;

                // Hash the input password
                const hashedPassword = crypto.createHash('sha256').update(password).digest('hex');

                // Compare the hashes
                if (hashedPassword === storedHash) {
                    res.status(200).json({ success: true, message: "Login successful" });
                } else {
                    res.status(401).json({ success: false, message: "Unauthorized" });
                }

                connection.end();
            });
        });
    } catch (error) {
        res.status(500).json({ error: "An unexpected error occurred during login" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

