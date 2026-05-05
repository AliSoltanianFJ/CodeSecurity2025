require('dotenv').config();
const express = require('express');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const { query, validationResult } = require('express-validator');
const mysql = require('mysql2/promise');

const app = express();

// Defense-in-depth HTTP headers
app.use(helmet());
app.use(helmet.hsts({ maxAge: 63072000, preload: true }));
app.use(helmet.frameguard({ action: 'deny' }));
app.use(helmet.noSniff());
app.use(helmet.xssFilter());

// Rate limiting to mitigate brute-force & DoS
app.use(rateLimit({ windowMs: 15 * 60 * 1000, max: 100 }));

// DB pool with least-privilege user, no multiple statements
const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    waitForConnections: true,
    connectionLimit: 5,
    multipleStatements: false
});

/**
 * GET /unsubscribe?email=...
 * Validates, parameterizes, and executes unsubscribe logic.
 */
app.get('/unsubscribe', [
    query('email').isEmail().normalizeEmail()
], async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        return res.status(400).json({ error: 'Invalid email parameter' });
    }
    const email = req.query.email;
    try {
        // Parameterized SELECT to prevent SQLi
        const [rows] = await pool.execute(
            'SELECT id FROM subscribers WHERE email = ?',
            [email]
        );
        if (rows.length === 0) {
            return res.status(404).json({ status: 'Not subscribed or already unsubscribed' });
        }
        // Soft-delete or flag as unsubscribed
        await pool.execute(
            'UPDATE subscribers SET unsubscribed = 1 WHERE id = ?',
            [rows[0].id]
        );
        return res.json({ status: 'Successfully unsubscribed' });
    } catch (err) {
        console.error('DB error:', err);
        return res.status(500).json({ error: 'Internal server error' });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});