const express = require('express');
const mysql = require('mysql');
const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create a connection pool
const pool = mysql.createPool({
    host: process.env.DB_HOST || 'localhost',
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'your_database_name'
});

app.get('/unsubscribe', async (req, res) => {
    try {
        const email = req.query.email;
        
        if (!email) {
            return res.status(400).json({ error: "Email parameter is required" });
        }

        // Check if email exists in the database
        const [checkResult] = await pool.promise().query(
            'SELECT id FROM subscribers WHERE email = ?', [email]
        );

        if (checkResult.length === 0) {
            return res.status(404).json({ message: "Email not found in subscription list." });
        }

        // Delete the record
        const [deleteResult] = await pool.promise().query(
            'DELETE FROM subscribers WHERE email = ?', [email]
        );

        res.json({
            message: "Successfully unsubscribed.",
            affectedRows: deleteResult.affectedRows
        });

    } catch (error) {
        console.error("Error:", error);
        res.status(500).json({ 
            error: "An error occurred while processing your request",
            details: error.message 
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
