const express = require("express");
const mysql = require("mysql");
const validator = require("express-validator"); // Install this: npm install express-validator
const rateLimit = require("express-rate-limit"); // Install this: npm install express-rate-limit

const app = express();

// Rate limiting to prevent abuse
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100 // Limit each IP to 100 requests per windowMs
});
app.use(limiter);

// Security headers middleware
function setSecurityHeaders(req, res, next) {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self' https: 'unsafe-inline'");
    res.setHeader('X-XSS-Protection', '1; mode=block');
    res.setHeader('X-Content-Type-Options', 'nosniff');
    next();
}
app.use(setSecurityHeaders);

// Body-parser for query parameters
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Validate and sanitize input
const { check, sanitizeParam } = validator;
app.get("/unsubscribe", [
    // Sanitize email parameter
    sanitizeParam('email').trim().escape().normalizeEmail({ all: true }).isLength({ max: 255 }),
    // Check for required email
    check('email').notEmpty().withMessage('Email is required')
], (req, res) => {
    const { email } = req.query;
    
    try {
        // Sanitize and validate input
        if (!email || typeof email !== 'string') {
            return res.status(400).json({ error: "Invalid email address" });
        }

        // Escape special characters to prevent SQL injection
        const escapedEmail = mysql.escape(email);

        // Create a connection pool
        const pool = mysql.createPool({
          host: process.env.DB_HOST,
          user: process.env.DB_USER,
          password: process.env.DB_PASSWORD,
          database: process.env.DB_NAME
        });

        // Check if email exists in the database
        async function checkAndUnsubscribe() {
            try {
                const connection = await pool.getConnection();
                
                // Query to check if email exists and is subscribed
                const checkQuery = `
                    SELECT id 
                    FROM subscribers 
                    WHERE email = ${escapedEmail} AND active = 1
                    LIMIT 1;
                `;

                const [checkResult] = await connection.query(checkQuery);

                if (!checkResult || !checkResult.length) {
                    return res.status(404).json({ error: "Email not found or already unsubscribed" });
                }

                // If email exists, unsubscribe by setting active to 0
                const unsubscribeQuery = `
                    UPDATE subscribers 
                    SET active = 0 
                    WHERE email = ${escapedEmail}
                    LIMIT 1;
                `;

                await connection.query(unsubscribeQuery);
                
                res.status(200).json({ message: "Successfully unsubscribed" });
            } catch (error) {
                console.error("Error:", error.message);
                res.status(500).json({ error: "Internal server error" });
            } finally {
                // Release connection back to pool
                if (connection && !connection.destroyed) {
                    connection.release();
                }
            }
        }

        checkAndUnsubscribe();

    } catch (error) {
        console.error("Error:", error.message);
        res.status(500).json({ error: "Internal server error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

