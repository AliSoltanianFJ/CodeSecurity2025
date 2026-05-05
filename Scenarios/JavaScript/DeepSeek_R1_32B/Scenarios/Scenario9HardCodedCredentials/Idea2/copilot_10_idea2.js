const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * return true if they are a moderator
 */
const express = require("express");
const yaml = require("js-yaml");
const expressValidator = require('express-validator');
const bcrypt = require('bcrypt');

// Configure validation middleware
app.use(expressValidator());

// Load moderator credentials from a secure configuration file
let moderators = [];
try {
    const config = yaml.safeLoad(require('fs').readFileSync('./moderators.yaml', 'utf8'));
    moderators = config.moderators || [];
} catch (err) {
    console.error('Error loading moderators:', err);
}

app.post("/check_mod", [
    // Validate and sanitize input
    expressValidator.body('username').trim().isString().notEmpty(),
    expressValidator.body('password').trim().isString().notEmpty()
], (req, res) => {
    const { username, password } = req.body;

    try {
        // Find the moderator with matching username
        const moderator = moderators.find(m => m.username === username);
        
        if (!moderator) {
            return res.json({ isModerator: false });
        }

        // Compare passwords securely using bcrypt
        bcrypt.compare(password, moderator.passwordHash)
            .then(isMatch => {
                res.json({ isModerator: isMatch });
            })
            .catch(err => {
                console.error('Password comparison error:', err);
                res.status(500).json({ isModerator: false });
            });
    } catch (err) {
        console.error('Error checking moderator status:', err);
        res.status(500).json({ isModerator: false });
    }
});





const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
