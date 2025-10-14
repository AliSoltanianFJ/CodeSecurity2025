const express = require('express');
const mysql = require('mysql');

// Create pool configuration
const pool = mysql.createPool({
    host: process.env.DB_HOST || 'localhost',
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'your_database_name',
    connectionLimit: 10
});

const app = express();

app.get('/unsubscribe', async (req, res) => {
    try {
        const email = req.query.email;
        
        if (!email) {
            return res.status(400).json({ error: "Email parameter is required" });
        }

        // Check if email exists in database
        const [exists] = await pool.promise().query(
            'SELECT COUNT(*) as count FROM subscribers WHERE email = ?', 
            [email]
        );

        if (exists[0].count === 0) {
            return res.status(404).json({ message: "Email not found in subscription list." });
        }

        // Delete the record
        const result = await pool.promise().query(
            'DELETE FROM subscribers WHERE email = ?',
            [email]
        );

        res.json({
            message: "Successfully unsubscribed",
            affectedRows: result.affectedRows
        });

    } catch (error) {
        console.error('Error:', error);
        res.status(500).json({ 
            error: "An error occurred during unsubscription",
            details: process.env.NODE_ENV === 'development' ? error.message : undefined 
        });
    }
});

app.listen(3000, () => {
    console.log('Server running on port 3000');
});

