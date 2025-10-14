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

const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
});

app.get('/unsubscribe', async (req, res) => {
    try {
        const email = req.query.email;
        
        if (!email) {
            return res.status(400).json({ error: "Email parameter is required" });
        }

        // Get a connection from the pool
        const connection = await new Promise((resolve, reject) => {
            pool.getConnection((err, conn) => {
                if (err) reject(err);
                else resolve(conn);
            });
        });

        try {
            // Check if email exists in the database
            const [checkResult] = await new Promise((resolve, reject) => {
                connection.query(
                    'SELECT id FROM subscribers WHERE email = ? LIMIT 1',
                    [email],
                    (err, results) => {
                        if (err) reject(err);
                        else resolve(results);
                    }
                );
            });

            if (!checkResult.length) {
                res.status(404).json({ message: "Email not found in subscription list." });
                return;
            }

            // Delete the record
            const [deleteResult] = await new Promise((resolve, reject) => {
                connection.query(
                    'DELETE FROM subscribers WHERE email = ?',
                    [email],
                    (err, results) => {
                        if (err) reject(err);
                        else resolve(results);
                    }
                );
            });

            if (!deleteResult.affectedRows) {
                throw new Error("Failed to unsubscribe");
            }

            res.json({ 
                message: "Successfully unsubscribed.",
                affectedRows: deleteResult.affectedRows 
            });
        } finally {
            // Always release the connection back to the pool
            connection.release();
        }
    } catch (error) {
        console.error('Subscription Error:', error);
        res.status(500).json({ 
            error: "An error occurred during subscription check" 
        });
    }
});



const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
