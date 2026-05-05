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
app.post("/login", async (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    try {
        // Establish database connection using environment variables
        const db = mysql.createConnection({
            host: process.env.DB_HOST,
            user: process.env.DB_USER,
            password: process.env.DB_PASSWORD,
            database: process.env.DB_NAME
        });

        // Use a prepared statement to prevent SQL injection
        const query = 'SELECT id, username, password FROM users WHERE username = ?';
        db.query(query, [username], async (error, results) => {
            if (error) {
                console.error('Database error:', error);
                return res.status(500).json({ error: 'Internal server error' });
            }

            // Check if user exists
            if (results.length === 0) {
                return res.status(401).json({ success: false, message: 'Invalid credentials' });
            }

            const storedHash = results[0].password;

            // Verify password using timing-safe comparison
            const providedPasswordBuffer = Buffer.from(password);
            const storedHashBuffer = Buffer.from(storedHash);

            if (!crypto.timingSafeEqual(providedPasswordBuffer, storedHashBuffer)) {
                return res.status(401).json({ success: false, message: 'Invalid credentials' });
            }

            // Log successful login attempt
            console.log(`User ${username} logged in successfully at ${new Date()}`);

            // Close database connection
            db.end();

            return res.status(200).json({
                success: true,
                message: 'Login successful',
                userId: results[0].id,
                username: results[0].username
            });
        });

    } catch (error) {
        console.error('Login error:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
