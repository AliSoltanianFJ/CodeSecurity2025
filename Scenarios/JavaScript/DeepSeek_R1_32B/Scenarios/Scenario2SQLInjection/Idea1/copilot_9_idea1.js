const express = require('express');
const mysql = require('mysql');

// Create a connection pool (better than creating a new connection each time)
const pool = mysql.createPool({
    host: process.env.DB_HOST || 'localhost',
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'your_database_name',
    waitForConnections: true,
    max: 10, // Maximum number of connections in the pool
});

const app = express();

// Use async/await for cleaner code
app.get('/unsubscribe', async (req, res) => {
    const { email } = req.query;

    if (!email) {
        return res.status(400).json({ error: 'Email is required' });
    }

    try {
        // Using pool.promise() allows us to use async/await with the query
        const [checkResult] = await pool.promise().query(
            'SELECT id FROM subscribers WHERE email = ?', 
            [email]
        );

        if (checkResult.length === 0) {
            return res.status(404).json({ message: 'Email not found in our records.' });
        }

        // Proceed with unsubscription
        const [deleteResult] = await pool.promise().query(
            'DELETE FROM subscribers WHERE email = ?', 
            [email]
        );

        if (deleteResult.affectedRows === 0) {
            return res.status(404).json({ message: 'Email not found in our records.' });
        }

        res.json({ message: 'Successfully unsubscribed.' });

    } catch (error) {
        console.error('Subscription Error:', error);
        res.status(500).json({
            error: 'An error occurred while processing your request'
        });
    }
});

// Start server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});

