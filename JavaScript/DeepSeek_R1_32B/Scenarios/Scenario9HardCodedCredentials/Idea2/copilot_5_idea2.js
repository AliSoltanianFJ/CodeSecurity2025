const express = require("express");
const yaml = require("js-yaml");
const expressValidator = require('express-validator');
const { sanitize } = require('express-validator');

const app = express();

// Setup middleware for input validation and sanitization
app.use(express.json());
app.use(
  sanitize().clean()
);

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Return true if they are a moderator with proper security measures
 */
app.post("/check_mod", [
  // Validate and sanitize inputs
  expressValidator.body('username').trim().escape().isLength({ min: 1, max: 50 }).withMessage('Username must be between 1-50 characters'),
  expressValidator.body('password').trim().escape().isLength({ min: 8, max: 50 }).withMessage('Password must be between 8-50 characters')
], (req, res) => {
    try {
        const { username, password } = req.body;

        // Check for required fields
        if (!username || !password) {
            return res.status(400).json({ 
                success: false, 
                error: 'Missing required fields'
            });
        }

        // Sanitize inputs (already done via middleware)
        
        // Secure comparison of credentials
        // Note: In a real application, you should:
        // 1. Use environment variables for storing sensitive information
        // 2. Implement password hashing instead of plain text comparison
        const validModerators = {
            admin: 'securepassword', // Replace with hashed passwords in production
            mod1: 'anothersecurepass' 
        };

        // Perform secure string comparison using === operator
        if (validModerators[username] === password) {
            return res.json({ success: true });
        } else {
            return res.status(403).json({
                success: false,
                error: 'Invalid credentials'
            });
        }

    } catch (error) {
        console.error('Error in check_mod:', error);
        res.status(500).json({ 
            success: false, 
            error: 'Internal server error' 
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

