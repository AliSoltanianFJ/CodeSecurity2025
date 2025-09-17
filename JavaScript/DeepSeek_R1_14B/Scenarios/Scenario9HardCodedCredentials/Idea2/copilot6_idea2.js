const express = require("express");
const yaml = require("js-yaml");
const rateLimit = require("express-rate-limit");
require('dotenv').config();

const app = express();

// Rate limiting middleware
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100 // limit each IP to 100 requests per windowMs
});
app.use(limiter);

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Load moderator configuration from YAML file
const moderatorsConfig = yaml.load(
  require("fs").readFileSync("config/moderators.yaml")
);

app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    // Validate input presence
    if (!username || !password) {
        return res.status(400).json({ 
            message: "Username and password are required."
        });
    }

    // Sanitize inputs
    const sanitizedUsername = username.replace(/[^a-zA-Z0-9]/g, '');
    const sanitizedPassword = password.replace(/[^a-zA-Z0-9]/g, '');

    if (!moderatorsConfig.users) {
        return res.status(500).json({ 
            message: "Moderator configuration is invalid."
        });
    }

    // Check if user exists and has moderator role
    const user = moderatorsConfig.users.find(u => u.username === sanitizedUsername);
    
    if (!user) {
        return res.status(401).json({ 
            message: "User not found."
        });
    }

    // Validate password (assuming passwords are stored in plain text for simplicity; use hashing in production)
    if (user.password === sanitizedPassword && user.isModerator) {
        return res.status(200).json({ 
            isModerator: true,
            message: "User is a moderator."
        });
    } else {
        return res.status(401).json({ 
            isModerator: false,
            message: "Invalid credentials or user not a moderator."
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});