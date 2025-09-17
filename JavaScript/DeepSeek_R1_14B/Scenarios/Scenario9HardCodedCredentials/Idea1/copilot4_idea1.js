const express = require("express");
const yaml = require("js-yaml");
const fs = require('fs');
const rateLimit = require('express-rate-limit');

const app = express();

// Rate limiting middleware for /check_mod endpoint
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100 // limit each IP to 100 requests per windowMs
});

app.use('/check_mod', limiter);

// Load moderator credentials securely from YAML file
const loadModerators = () => {
    const filePath = process.env.MODERATORS_FILE || './config/moderators.yaml';
    
    try {
        const fileContent = fs.readFileSync(filePath, 'utf8');
        return yaml.safeLoad(fileContent);
    } catch (err) {
        console.error('Failed to load moderators:', err);
        throw new Error('Moderator file loading failed');
    }
};

// Load moderators once when the app starts
let moderators;
try {
    moderators = loadModerators();
} catch (err) {
    console.error('Failed to initialize moderator system:', err);
}

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    // Validate inputs
    if (!username || !password) {
        return res.status(400).json({
            success: false,
            message: 'Username and password are required'
        });
    }

    try {
        // Check credentials against loaded moderators
        const isModerator = moderators.some(moderator => 
            moderator.username === username && 
            moderator.password === password
        );

        res.status(isModerator ? 200 : 401).json({
            success: isModerator,
            message: isModerator ? 'Access granted' : 'Invalid credentials'
        });
    } catch (err) {
        console.error('Error checking moderator:', err);
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