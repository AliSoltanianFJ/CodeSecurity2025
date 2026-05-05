const express = require('express');
const mysql = require('mysql');

// Create connection pool
const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    port: parseInt(process.env.DB_PORT) || 3306,
    // Connection pool settings
    connectionLimit: 10, // Adjust based on your needs
});

const app = express();

app.get('/unsubscribe', async (req, res) => {
    const email = req.query.email;

    if (!email) {
        return res.status(400).json({ 
            success: false,
            message: 'Email parameter is required'
        });
    }

    try {
        // Get connection from pool
        let connection;
        
        connection = await new Promise((resolve, reject) => {
            pool.getConnection((err, conn) => {
                if (err) reject(err);
                else resolve(conn);
            });
        });

        try {
            // Check if email exists in the database
            const [checkResults] = await new Promise((resolve, reject) => {
                connection.query(
                    'SELECT id FROM subscribers WHERE email = ?',
                    [email],
                    (error, results) => {
                        if (error) reject(error);
                        else resolve(results);
                    }
                );
            });

            if (!checkResults || checkResults.length === 0) {
                return res.status(404).json({
                    success: false,
                    message: 'Email not found in subscription list'
                });
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

            res.status(200).json({
                success: true,
                message: 'Successfully unsubscribed',
                affectedRows: deleteResult.affectedRows
            });
        } catch (error) {
            console.error('Subscription Error:', error);
            res.status(500).json({
                success: false,
                message: 'An error occurred during subscription check'
            });
        } finally {
            // Release connection back to pool
            connection.release();
        }
    } catch (error) {
        console.error('Connection Error:', error);
        res.status(500).json({
            success: false,
            message: 'Internal server error. Please try again later.'
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

