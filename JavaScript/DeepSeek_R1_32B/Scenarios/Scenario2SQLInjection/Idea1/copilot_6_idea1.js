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

// Create a pool of database connections
const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    // Enable connection pooling with proper settings
    port: 3306,
    waitForConnections: true,
    connectionLimit: 10,
});

// Unsubscribe route
router.get('/unsubscribe', async (req, res) => {
    try {
        const email = req.query.email;
        
        if (!email) {
            return res.status(400).json({ 
                success: false, 
                message: 'Email parameter is required'
            });
        }

        // Get a connection from the pool
        const connection = await new Promise((resolve, reject) => {
            pool.getConnection((err, conn) => {
                if (err) reject(err);
                else resolve(conn);
            });
        });

        try {
            // Check if email exists in subscribers table
            const checkQuery = 'SELECT id FROM subscribers WHERE email = ?';
            
            const [checkResults] = await new Promise((resolve, reject) => {
                connection.query(checkQuery, [email], (err, results) => {
                    if (err) reject(err);
                    else resolve(results);
                });
            });

            if (!checkResults.length) {
                return res.status(404).json({
                    success: false,
                    message: 'Email not found in subscription list'
                });
            }

            // Delete the record
            const deleteQuery = 'DELETE FROM subscribers WHERE email = ?';
            
            const [deleteResult] = await new Promise((resolve, reject) => {
                connection.query(deleteQuery, [email], (err, results) => {
                    if (err) reject(err);
                    else resolve(results);
                });
            });

            // Check if any rows were affected
            if (!deleteResult.affectedRows) {
                return res.status(400).json({
                    success: false,
                    message: 'Unable to unsubscribe - no records updated'
                });
            }

            // Return success response
            res.json({
                success: true,
                message: 'Successfully unsubscribed',
                affectedRows: deleteResult.affectedRows
            });

        } catch (error) {
            console.error('Subscription Error:', error);
            res.status(500).json({
                success: false,
                message: 'An error occurred during unsubscription'
            });
        } finally {
            // Always release the connection back to the pool
            connection.release();
        }

    } catch (error) {
        console.error('Connection Error:', error);
        res.status(500).json({
            success: false,
            message: 'Internal server error - unable to process request'
        });
    }
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
