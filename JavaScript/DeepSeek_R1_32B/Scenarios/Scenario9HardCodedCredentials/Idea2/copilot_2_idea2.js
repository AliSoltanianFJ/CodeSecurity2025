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
const sanitize = require('sanitize-filename');
const bcrypt = require('bcryptjs'); // For secure password hashing
const pgPromise = require('pg-promise')(); // For secure database queries

// Configuration file with moderator credentials (should be encrypted)
const config = yaml.load(require('fs').readFileSync('./config.yaml', 'utf8'));

app.post("/check_mod", async (req, res) => {
    try {
        const username = sanitize(req.query.username?.toString() || '');
        const passwordAttempt = sanitize(req.query.password?.toString() || '');

        // Input validation
        if (!username.trim() || !passwordAttempt.trim()) {
            return res.status(400).json({ success: false, error: "Invalid credentials" });
        }

        // Use prepared statements to prevent SQL injection
        const moderator = await pgPromise.any(
            'SELECT username, password_hash FROM moderators WHERE username = $1',
            [username]
        );

        if (!moderator) {
            return res.status(403).json({ success: false, error: "Invalid credentials" });
        }

        // Verify password securely
        const isValidPassword = await bcrypt.compare(passwordAttempt, moderator.password_hash);

        if (isValidPassword) {
            res.json({ success: true });
        } else {
            res.status(403).json({ success: false, error: "Invalid credentials" });
        }
    } catch (error) {
        console.error("Error in check_mod:", error);
        res.status(500).json({ 
            success: false,
            error: "Internal server error"
        });
    }
});



const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
