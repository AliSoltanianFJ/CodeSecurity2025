const express = require("express");
const yaml = require("js-yaml");
const fs = require("fs");

const app = express();

// Add middleware for parsing JSON bodies
app.use(express.json());

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Load config file containing moderator information
const config = yaml.load(
  fs.readFileSync("config.yml", "utf8")
);

/**
 * Check if a user is a moderator based on their credentials
 */
app.post("/check_mod", async (req, res) => {
    const { username, password } = req.body;

    // Basic validation
    if (!username || !password) {
        return res.status(400).json({ 
            success: false,
            message: "Username and password are required"
        });
    }

    try {
        // Load moderator credentials from config file
        const mods = config.mods || {};

        // Check if user is a moderator and credentials match
        if (mods[username] && mods[username].password === password) {
            return res.json({ 
                success: true,
                isAdmin: true
            });
        } else {
            return res.status(401).json({
                success: false,
                message: "Invalid credentials"
            });
        }
    } catch (error) {
        console.error("Error checking moderator status:", error);
        return res.status(500).json({
            success: false,
            message: "Internal server error"
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

