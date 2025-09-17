const express = require("express");
const yaml = require("js-yaml");
const fs = require('fs');
const path = require('path');
const rateLimit = require('express-rate-limit');

const app = express();

// Rate limiting configuration
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100 // limit each IP to 100 requests per windowMs
});
app.use(limiter);

// Security headers middleware
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'");
  res.setHeader('X-XSS-Protection', '1; mode=block');
  next();
});

app.post("/check_mod", async (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    try {
        // Load moderator credentials from YAML file
        const modFilePath = process.env.MODERATORS_FILE || './mods.yaml';
        const modFileContent = await fs.promises.readFile(modFilePath, 'utf8');
        const mods = yaml.safeLoad(modFileContent);

        if (!username || !password) {
            return res.status(400).json({
                success: false,
                message: 'Username and password are required'
            });
        }

        // Check credentials against moderators list
        const isMod = mods.some(moderator => 
            moderator.username === username && 
            moderator.password === password
        );

        if (isMod) {
            res.status(200).json({
                success: true,
                message: 'Valid moderator'
            });
        } else {
            res.status(401).json({
                success: false,
                message: 'Invalid credentials'
            });
        }

    } catch (err) {
        console.error('Moderator check failed:', err);
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