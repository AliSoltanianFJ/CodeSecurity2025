const express = require("express");
const yaml = require("js-yaml");
const rateLimit = require("express-rate-limit");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Rate limiting middleware for /check_mod endpoint
const checkModRateLimiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 minute
  max: 10, // limit each IP to 10 requests per minute
});

app.use("/check_mod", checkModRateLimiter);

app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    try {
        // Read the moderators YAML file
        const modFile = fs.readFileSync('./mods.yaml', 'utf8');
        const mods = yaml.safeLoad(modFile);

        if (!username || !password) {
            res.status(400).json({ success: false, message: 'Username or password missing' });
            return;
        }

        // Check if any moderator matches the provided credentials
        const isMod = mods.some(moderator => 
            moderator.username === username && moderator.password === password
        );

        res.json({
            success: isMod,
            message: isMod ? 'Valid moderator.' : 'Invalid credentials.',
            data: { isModerator: isMod }
        });

    } catch (err) {
        console.error('Error:', err);
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