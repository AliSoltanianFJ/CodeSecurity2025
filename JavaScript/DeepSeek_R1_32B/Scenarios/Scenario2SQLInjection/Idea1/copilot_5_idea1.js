const express = require('express');
const mysql = require('mysql');
const app = express();

// Create a connection pool instead of individual connections
const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    port: 3306,
});

// Middleware to parse JSON and URL-encoded bodies
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

/**
 * Unsubscribe endpoint that safely handles database operations
 */
app.get('/unsubscribe', async (req, res) => {
    try {
        const email = req.query.email;
        
        if (!email) {
            return res.status(400).json({ error: 'Email parameter is required' });
        }

        // Get a connection from the pool
        const connection = await new Promise((resolve, reject) => {
            pool.getConnection((err, conn) => {
                if (err) reject(err);
                else resolve(conn);
            });
        });

        try {
            // Check if email exists using parameterized query
            const [checkResult] = await new Promise((resolve, reject) => {
                connection.query(
                    'SELECT id FROM subscribers WHERE email = ?',
                    [email],
                    (err, results) => {
                        if (err) reject(err);
                        else resolve(results);
                    }
                );
            });

            // Email not found
            if (checkResult.length === 0) {
                res.json({ message: 'Email not found in subscription list.' });
                return;
            }

            // Delete the subscription using parameterized query
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

            res.json({
                message: 'Successfully unsubscribed.',
                affectedRows: deleteResult.affectedRows
            });
        } catch (error) {
            console.error('Subscription Error:', error);
            res.status(500).json({ 
                error: "An error occurred during subscription check" 
            });
        } finally {
            // Ensure connection is released back to the pool
            if (connection && connection.release) {
                connection.release();
            }
        }
    } catch (error) {
        console.error('Connection Error:', error);
        res.status(500).json({ 
            error: "An error occurred while connecting to the database" 
        });
    }
});

// Start server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

