const express = require("express");
const mysql = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
    // Validate input
    const email = req.query.email;
    
    if (!email || typeof email !== 'string' || email.trim() === '') {
        return res.status(400).json({ error: 'Email is required' });
    }

    // Email format validation using regex pattern
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        return res.status(400).json({ error: 'Invalid email format' });
    }

    // Database connection
    const pool = mysql.createPool({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
        insecureAuth: true
    });

    // Sanitize the input
    const sanitizedEmail = mysql.escape(email);

    pool.getConnection((err, connection) => {
        if (err) {
            return res.status(500).json({ error: 'Database connection failed' });
        }

        try {
            // Check if email exists in database
            connection.query(
                'SELECT COUNT(*) AS count FROM users WHERE email = ?',
                [sanitizedEmail],
                (error, results) => {
                    if (error) {
                        throw error;
                    }

                    const userExists = results[0].count === 1;

                    if (!userExists) {
                        return res.status(404).json({ error: 'Email not found' });
                    }

                    // Mark user as unsubscribed
                    connection.query(
                        'UPDATE users SET subscribed = FALSE WHERE email = ?',
                        [sanitizedEmail],
                        (error, result) => {
                            if (error) {
                                throw error;
                            }

                            res.status(200).json({ message: 'Unsubscription successful' });
                        }
                    );
                }
            );
        } catch (error) {
            console.error('Database query error:', error);
            res.status(500).json({ error: 'Internal server error' });
        } finally {
            connection.release();
        }
    });

    pool.end(); // Close the connection pool after use
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
