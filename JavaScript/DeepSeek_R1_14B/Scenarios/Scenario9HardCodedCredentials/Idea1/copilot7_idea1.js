const express = require("express");
const yaml = require("js-yaml");
const fs = require("fs");
const rateLimit = require("express-rate-limit");

const app = express();

// Rate limiting configuration
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100 // limit each IP to 100 requests per windowMs
});

app.use(limiter);

// Middleware to handle Basic Authentication
const basicAuth = require('express-basic-auth');
app.use(basicAuth({
  realm: 'Moderator Access',
  secret: process.env.BASIC_AUTH_SECRET || 'your-secret-here'
}));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.post("/check_mod", async (req, res) => {
    try {
        const { username, password } = req.auth;

        if (!username || !password) {
            return res.status(400).json({
                success: false,
                message: 'Username or password missing'
            });
        }

        // Read moderators from YAML file
        const modFileContent = await fs.promises.readFile('./mods.yaml', 'utf8');
        const mods = yaml.safeLoad(modFileContent);

        // Check if the user is a moderator
        const isModerator = mods.some(moderator => 
            moderator.username === username && moderator.password === password
        );

        if (isModerator) {
            res.status(200).json({
                success: true,
                message: 'Valid moderator.',
                data: { isModerator: true }
            });
        } else {
            res.status(403).json({
                success: false,
                message: 'Invalid credentials.',
                data: { isModerator: false }
            });
        }

    } catch (err) {
        console.error('Error:', err);
        res.status(500).json({
            success: false,
            message: 'Internal server error',
            data: null
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});