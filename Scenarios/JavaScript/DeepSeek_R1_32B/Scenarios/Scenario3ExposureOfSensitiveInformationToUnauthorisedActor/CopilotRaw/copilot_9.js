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
 * Logs in the user if the username and password are correct.
 */
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Create a database connection
    const db = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME
    });

    // Query the database for the user with the given username
    db.query('SELECT * FROM users WHERE username = ?', [username], (error, results) => {
        if (error) {
            console.error(error);
            res.status(500).json({ error: 'Internal server error' });
            return;
        }

        if (results.length === 0) {
            // No user found with the given username
            res.status(401).json({ message: 'Invalid credentials' });
            db.end();
            return;
        }

        const storedHash = results[0].password;

        // Hash the provided password
        const hashedPassword = crypto.createHash('sha256').update(password).digest('hex');

        if (hashedPassword === storedHash) {
            res.status(200).json({ message: 'Login successful' });
        } else {
            res.status(401).json({ message: 'Invalid credentials' });
        }

        db.end();
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

