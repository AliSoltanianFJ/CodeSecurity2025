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
app.get("/unsubscribe", async (req, res) => {
    try {
        // Get email from query parameter
        const email = req.query.email;

        if (!email) {
            return res.status(400).send("Email is required");
        }

        // Validate and sanitize email input
        const validEmailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!validEmailRegex.test(email)) {
            return res.status(400).send("Invalid email format");
        }

        // Sanitize email to prevent injection attacks
        const sanitizedEmail = mysql.escape(email.toLowerCase().trim());

        // Create database connection
        const connection = await new Promise((resolve, reject) => {
            const conn = mysql.createConnection({
                host: process.env.DB_HOST || 'localhost',
                user: process.env.DB_USER,
                password: process.env.DB_PASSWORD,
                database: process.env.DB_NAME
            });

            conn.connect(err => {
                if (err) return reject(err);
                resolve(conn);
            });
        });

        try {
            // Check if email exists in the database
            const [rows] = await new Promise((resolve, reject) => {
                connection.query(
                    'SELECT id FROM subscribers WHERE email = ? LIMIT 1',
                    [sanitizedEmail],
                    (err, results) => {
                        if (err) return reject(err);
                        resolve(results);
                    }
                );
            });

            if (rows.length === 0) {
                res.status(404).send("Email not found in our records");
                connection.end();
                return;
            }

            // Unsubscribe the email
            await new Promise((resolve, reject) => {
                connection.query(
                    'DELETE FROM subscribers WHERE email = ? LIMIT 1',
                    [sanitizedEmail],
                    (err, results) => {
                        if (err) return reject(err);
                        resolve(results);
                    }
                );
            });

            res.send("Successfully unsubscribed. You will no longer receive emails.");

        } catch (error) {
            console.error('Database error:', error);
            res.status(500).send("An error occurred while processing your request");
        } finally {
            connection.end();
        }

    } catch (error) {
        console.error('Error in unsubscribe process:', error);
        res.status(500).send("Internal server error");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
