const express = require("express");
const yaml = require("js-yaml");
const fs = require("fs");
const { escape } = require('escape-html');

const app = express();

// Middleware to parse JSON bodies
app.use(express.json());

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

function sanitizeInput(input) {
    return escape(String(input));
}

app.post("/check_mod", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Validate input
    if (!username || !password) {
        return res.status(400).json({ 
            status: 'error', 
            message: 'Username and password are required.' 
        });
    }

    if (typeof username !== 'string' || typeof password !== 'string') {
        return res.status(400).json({
            status: 'error',
            message: 'Username and password must be strings.'
        });
    }

    // Prevent potential buffer overflow by limiting input length
    const MAX_LENGTH = 50;
    if (username.length > MAX_LENGTH || password.length > MAX_LENGTH) {
        return res.status(400).json({
            status: 'error',
            message: 'Input exceeds maximum allowed length.'
        });
    }

    // Sanitize inputs to prevent XSS and injection attacks
    const safeUsername = sanitizeInput(username);
    const safePassword = sanitizeInput(password);

    try {
        // Read configuration file containing moderator credentials
        const configContent = fs.readFileSync('./config.yml', 'utf8');
        const config = yaml.safeLoad(configContent);
        const mods = config.mods || [];  // Default to empty array if no mods defined

        // Check if the user is a moderator by comparing sanitized inputs
        const isMod = mods.some(mod => 
            mod.username === safeUsername && mod.password === safePassword
        );

        res.json({
            status: 'success',
            isMod: isMod
        });
    } catch (error) {
        console.error('Error reading or parsing config file:', error);
        res.status(500).json({
            status: 'error',
            message: 'Internal server error occurred while checking moderator status.'
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

