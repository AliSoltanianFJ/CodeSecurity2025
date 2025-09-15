const express = require("express");
const mysql = require("mysql");

// Create a database connection pool
const pool = mysql.createPool({
    host: 'localhost',
    user: 'your_user',
    password: 'your_password',
    database: 'your_database',
    connectionLimit: 10,
});

const app = express();

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    // Add more security headers if needed
    res.setHeader('Content-Security-Policy', "default-src 'self'");
    next();
});

// Validate email format (basic validation)
const validateEmail = (email) => {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
};

app.get("/unsubscribe", async (req, res) => {
    // Get the email from query parameters
    const { email } = req.query;

    if (!email) {
        return res.status(400).json({ status: 'error', message: 'Email is required' });
    }

    // Validate email format to prevent injection attacks
    if (!validateEmail(email)) {
        return res.status(400).json({ status: 'error', message: 'Invalid email format' });
    }

    try {
        const connection = await pool.getConnection();
        
        // Prepare SQL statement with proper sanitation
        const [rows] = await connection.query(
            'SELECT id FROM users WHERE email = ? LIMIT 1',
            [email]
        );

        if (rows.length === 0) {
            return res.status(404).json({ status: 'error', message: 'Email not found' });
        }

        // Update unsubscribe status
        await connection.query(
            'UPDATE users SET unsubscribe_status = \'Y\', unscribe_date = CURRENT_TIMESTAMP WHERE email = ?',
            [email]
        );

        await connection.release();

        res.json({
            status: 'success',
            message: 'Unsubscription request processed'
        });

    } catch (error) {
        console.error('Error:', error);
        res.status(500).json({ 
            status: 'error', 
            message: 'An error occurred while processing your request'
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});