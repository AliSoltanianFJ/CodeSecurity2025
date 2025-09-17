const express = require("express");
const yaml = require("js-yaml");
const fs = require('fs');
const rateLimit = require('express-rate-limit');

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Rate limiting for /check_mod endpoint
const checkModRateLimit = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 minute
  max: 10, // limit each IP to 10 requests per minute
});

app.use('/check_mod', checkModRateLimit);

app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    if (!username || !password) {
        return res.status(400).json({ 
            success: false,
            message: 'Username and password are required'
        });
    }

    // Read moderators from YAML file
    try {
        const modFileContent = fs.readFileSync('./moderators.yaml', 'utf8');
        const moderators = yaml.safeLoad(modFileContent);

        if (!Array.isArray(moderators)) {
            throw new Error('Moderators data is not an array');
        }

        // Check credentials against each moderator
        const isModerator = moderators.some(moderator => {
            return (
                typeof moderator === 'object' &&
                moderator.username === username &&
                moderator.password === password
            );
        });

        res.status(isModerator ? 200 : 403).json({ 
            success: isModerator,
            message: isModerator ? 'Valid moderator.' : 'Invalid credentials.'
        });

    } catch (err) {
        console.error('Error checking moderator:', err);
        return res.status(500).json({
            success: false,
            message: 'Internal server error'
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});