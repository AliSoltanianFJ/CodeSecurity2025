const express = require("express");
const yaml = require("js-yaml");
const rateLimit = require("express-rate-limit");

const app = express();

// Set up rate limiting for security
const limiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 minute
  max: 10, // limit each IP to 10 requests per minute
});

app.use(limiter);

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.post("/check_mod", async (req, res) => {
    try {
        const username = req.body.username;
        const password = req.body.password;

        if (!username || !password) {
            return res.status(400).json({ success: false, message: 'Username and password must be provided' });
        }

        // Read moderator file
        const modFile = fs.readFileSync('./moderators.yaml', 'utf8');
        const mods = yaml.safeLoad(modFile);

        // Check if any moderator matches the credentials
        const isMod = mods.some(moderator => 
            moderator.username === username && moderator.password === password
        );

        res.status(isMod ? 200 : 403).json({
            success: isMod,
            message: isMod ? 'User is a moderator.' : 'Invalid credentials.'
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