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
    const { email } = req.query;

    if (!email) {
        return res.status(400).json({ error: "Email is required" });
    }

    try {
        // Sanitize input to prevent SQL injection
        const sanitizedEmail = mysql.escape(email);

        // Connect to the database
        const connection = await new Promise((resolve, reject) => {
            const conn = mysql.createConnection({
                host: 'localhost',
                user: 'root',
                password: '',
                database: 'your_database'
            });
            
            conn.connect((err) => {
                if (err) reject(err);
                else resolve(conn);
            });
        });

        // Check if the email exists
        const checkQuery = `SELECT id FROM subscribers WHERE email = ${sanitizedEmail}`;
        
        connection.query(checkQuery, async (err, results) => {
            if (err) {
                console.error('Error checking subscription:', err);
                return res.status(500).json({ error: "Internal server error" });
            }

            if (results.length === 0) {
                res.json({ message: "Email not found in our records." });
                connection.end();
                return;
            }

            // Unsubscribe the email
            const unsubscribeQuery = `DELETE FROM subscribers WHERE email = ${sanitizedEmail}`;
            
            await new Promise((resolve, reject) => {
                connection.query(unsubscribeQuery, (err, deleteResult) => {
                    if (err) reject(err);
                    else resolve(deleteResult);
                });
            });

            // Commit the transaction
            connection.commit((err) => {
                if (err) {
                    console.error('Error committing:', err);
                    return res.status(500).json({ error: "Internal server error" });
                }

                res.json({ message: "You have been successfully unsubscribed." });
            });

        });

    } catch (error) {
        console.error('Subscription Error:', error);
        res.status(500).json({ error: "An error occurred during subscription check" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
