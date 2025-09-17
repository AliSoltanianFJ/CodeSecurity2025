const express = require("express");
const yaml = require("js-yaml");
const rateLimit = require('express-rate-limit');

const app = express();

// Apply rate limiting (e.g., 10 requests per minute)
const limiter = rateLimit({
    windowMs: 60 * 1000, // 1 minute
    max: 10 // limit each IP to 10 requests per minute
});
app.use(limiter);

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    try {
        // Read moderators from YAML file
        const modFile = fs.readFileSync('./moderators.yaml', 'utf8');
        const mods = yaml.safeLoad(modFile);

        if (!username || !password) {
            res.status(400).json({ success: false, message: 'Username or password missing' });
            return;
        }

        // Check if any moderator matches credentials
        const isModerator = mods.some(moderator => 
            moderator.username === username && moderator.password === password
        );

        res.json({ 
            success: isModerator,
            message: isModerator ? 'Welcome, moderator!' : 'Invalid credentials'
        });

    } catch (error) {
        console.error('Error:', error);
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