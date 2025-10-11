const express = require("express");
const mysql = require("mysql");
const crypto = require('crypto');
const Joi = require('@hapi/joi');

const app = express();

// Validate environment variables
const requiredEnvVars = ['DB_HOST', 'DB_USER', 'DB_PASSWORD', 'DB_NAME'];
requiredEnvVars.forEach(varName => {
    if (!process.env[varName]) {
        throw new Error(`Environment variable ${varName} is required`);
    }
});

const schema = Joi.object({
    email: Joi.string().email({ minDomainSegments: 2 }).required()
});

// Create database connection pool
const dbPool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    // Secure MySQL configuration
    insecureAuth: false,
    multipleStatements: false,
});

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('Content-Security-Policy', "default-src 'self'");
    next();
});

app.get("/unsubscribe", async (req, res) => {
    try {
        // Validate input
        const { error } = schema.validate(req.query);
        if (error) {
            return res.status(400).json({ 
                success: false,
                message: 'Invalid email format'
            });
        }

        const email = req.query.email;
        
        // Check for SQL injection using parameterized query
        dbPool.getConnection(async (err, connection) => {
            if (err) {
                console.error('Database connection error:', err);
                return res.status(500).json({ 
                    success: false,
                    message: 'Internal server error'
                });
            }

            try {
                // Check if email exists in the database
                const checkQuery = `SELECT id FROM subscribers WHERE email = ?`;
                
                connection.query(checkQuery, [email], async (err, results) => {
                    if (err) {
                        console.error('Database query error:', err);
                        return res.status(500).json({ 
                            success: false,
                            message: 'Internal server error'
                        });
                    }

                    if (results.length === 0) {
                        return res.status(404).json({ 
                            success: false,
                            message: 'Email not found in subscription list'
                        });
                    }

                    // If email exists, unsubscribe
                    const deleteQuery = `DELETE FROM subscribers WHERE email = ?`;
                    connection.query(deleteQuery, [email], (err) => {
                        if (err) {
                            console.error('Unsubscribe error:', err);
                            return res.status(500).json({ 
                                success: false,
                                message: 'Internal server error'
                            });
                        }

                        res.json({
                            success: true,
                            message: 'Successfully unsubscribed'
                        });
                    });
                });
            } catch (error) {
                console.error('Error in unsubscribe process:', error);
                res.status(500).json({ 
                    success: false,
                    message: 'Internal server error'
                });
            } finally {
                connection.release();
            }
        });
    } catch (error) {
        console.error('Unexpected error:', error);
        res.status(500).json({ 
            success: false,
            message: 'Internal server error'
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

