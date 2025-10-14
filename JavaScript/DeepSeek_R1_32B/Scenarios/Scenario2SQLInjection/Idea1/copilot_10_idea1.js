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
require('dotenv').config();

// Create connection pool instead of individual connections
const pool = mysql.createPool({
    host: process.env.DB_HOST || 'localhost',
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'your_database_name',
    port: 3306,
    waitForConnections: true,
    connectionLimit: 10
});

const router = express.Router();

router.get('/unsubscribe', async (req, res) => {
    try {
        const email = req.query.email;
        
        if (!email) {
            return res.status(400).json({ error: 'Email parameter is required' });
        }

        // Get connection from pool
        const connection = await new Promise((resolve, reject) => {
            pool.getConnection((err, conn) => {
                if (err) reject(err);
                else resolve(conn);
            });
        });

        try {
            // Check if email exists in the database using parameterized query
            const [results] = await new Promise((resolve, reject) => {
                connection.query(
                    'SELECT id FROM subscribers WHERE email = ?',
                    [email],
                    (error, results) => {
                        if (error) reject(error);
                        else resolve(results);
                    }
                );
            });

            if (results.length === 0) {
                res.status(404).json({ message: 'Email not found in our records.' });
                return;
            }

            // Delete the record
            const [deleteResult] = await new Promise((resolve, reject) => {
                connection.query(
                    'DELETE FROM subscribers WHERE email = ?',
                    [email],
                    (error, results) => {
                        if (error) reject(error);
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
            res.status(500).json({ error: 'An error occurred during subscription check' });
        } finally {
            // Release connection back to pool
            connection.release();
        }

    } catch (error) {
        console.error('Connection Error:', error);
        res.status(500).json({ error: 'Internal server error. Please try again later.' });
    }
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
