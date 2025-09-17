const express = require("express");
const yaml = require("js-yaml");
const { rateLimit } = require('express-rate-limit');
const bcrypt = require('bcrypt');

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Apply rate limiting to the check_mod endpoint
const checkModRateLimiter = rateLimit({
    windowMs: 1 * 60 * 1000, // 1 minute
    max: 5 // limit each IP to 5 requests per minute
});

app.use('/check_mod', checkModRateLimiter);

app.post("/check_mod", async (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    try {
        if (!username || !password) {
            return res.status(400).json({ 
                success: false,
                message: 'Username or password missing' 
            });
        }

        // Read moderator data from YAML file specified in environment variable
        const modFile = process.env.MODERATORS_FILE || './mods.yaml';
        const modStream = fs.promises.readFile(modFile, 'utf8');
        
        const modsContent = await modStream;
        const mods = yaml.safeLoad(modsContent);

        if (!Array.isArray(mods) || !mods.length) {
            return res.status(500).json({ 
                success: false,
                message: 'No moderators found' 
            });
        }

        // Find moderator by username
        const moderator = mods.find(m => m.username === username);
        
        if (!moderator) {
            return res.status(403).json({ 
                success: false,
                message: 'Invalid username' 
            });
        }

        // Compare passwords securely using bcrypt
        const passwordMatch = await bcrypt.compare(password, moderator.password);

        if (passwordMatch) {
            res.status(200).json({ 
                success: true,
                moderator: true 
            });
        } else {
            res.status(403).json({ 
                success: false,
                moderator: false 
            });
        }

    } catch (err) {
        console.error('Error:', err);
        res.status(500).json({ 
            status: 'error', 
            message: 'Internal server error' 
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});