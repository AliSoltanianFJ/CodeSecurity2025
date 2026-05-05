const express = require("express");
const mysql = require("mysql");
const { check } = require("express-validator"); // For input validation
const rateLimit = require("express-rate-limit"); // To prevent abuse

const app = express();

// Rate limiting to prevent abuse (max 10 requests per minute)
const limiter = rateLimit({
    windowMs: 60 * 1000,
    max: 10
});
app.use(limiter);

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('Content-Security-Policy', "default-src 'self'");
    res.setHeader('X-XSS-Protection', '1; mode=block');
    next();
});

/**
 * Check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", [
    check("email")
        .notEmpty().withMessage("Email is required")
        .isEmail().withMessage("Invalid email format")
], async (req, res) => {
    try {
        const { email } = req.query;
        
        // Validate input
        const errors = await check(req).catch(() => true);
        if (errors) {
            return res.status(400).json({ error: "Invalid email" });
        }

        // Sanitize email - remove any potential malicious characters
        const sanitizedEmail = sanitizeInput(email);

        // Create a new promise wrapper for mysql connection
        const checkSubscription = () => {
            return new Promise((resolve, reject) => {
                const query = "SELECT id FROM subscribers WHERE email = ?";
                db.query(query, [sanitizedEmail], (err, results) => {
                    if (err) reject(err);
                    else resolve(results);
                });
            });
        };

        // Check if email exists in the database
        const subscriptionCheck = await checkSubscription();
        
        if (subscriptionCheck.length === 0) {
            return res.status(404).json({ message: "Email not found" });
        }

        // If email exists, unsubscribe by deleting it from the database
        const unsubscribeQuery = "DELETE FROM subscribers WHERE email = ?";
        await db.query(unsubscribeQuery, [sanitizedEmail]);

        res.json({ 
            success: true,
            message: "Successfully unsubscribed"
        });

    } catch (error) {
        console.error("Error during unsubscribe:", error);
        res.status(500).json({
            error: "An error occurred while processing your request",
            details: process.env.NODE_ENV === 'development' ? error.message : undefined
        });
    }
});

// Sanitize input function
function sanitizeInput(input) {
    // Strip any potential malicious characters
    const allowedChars = /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*$/;
    if (!allowedChars.test(input)) {
        throw new Error("Invalid email format");
    }
    return input;
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});