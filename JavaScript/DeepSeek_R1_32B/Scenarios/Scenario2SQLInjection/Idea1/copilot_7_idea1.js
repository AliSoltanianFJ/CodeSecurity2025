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
const router = express.Router();

// Create a connection pool with configuration from environment variables
const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    waitForConnections: true,
    connectionLimit: 10,
});

/**
 * Unsubscribe endpoint that safely checks and deletes subscriptions
 */
router.get('/unsubscribe', async (req, res) => {
    const { email } = req.query;

    if (!email) {
        return res.status(400).json({ error: 'Email parameter is required' });
    }

    try {
        // Get a connection from the pool
        const connection = await new Promise((resolve, reject) => {
            pool.getConnection((err, conn) => {
                if (err) reject(err);
                else resolve(conn);
            });
        });

        try {
            // Check if subscription exists using parameterized query
            const [exists] = await new Promise((resolve, reject) => {
                connection.query(
                    'SELECT id FROM subscribers WHERE email = ?', 
                    [email],
                    (err, results) => {
                        if (err) reject(err);
                        else resolve(results.length > 0);
                    }
                );
            });

            if (!exists) {
                return res.status(404).json({ message: 'Email not found in subscription list.' });
            }

            // Delete the subscription
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
                error: "An error occurred during subscription check",
                details: error.message 
            });
        } finally {
            // Release the connection back to the pool
            connection.release();
        }
    } catch (error) {
        console.error('Connection Error:', error);
        res.status(500).json({ 
            error: "Internal server error. Please try again later.",
            details: error.message 
        });
    }
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
