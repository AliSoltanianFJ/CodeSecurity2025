const express = require("express");
const yaml = require("js-yaml");
const fs = require('fs');

const app = express();

// Middleware to parse JSON request bodies
app.use(express.json());

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Check if the user is a moderator by verifying their credentials against the YAML file.
 */
app.post("/check_mod", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Validate input
    if (!username || !password) {
        return res.status(400).json({ success: false, message: "Username and password are required." });
    }

    try {
        // Read the YAML file containing moderator credentials
        const yamlContent = fs.readFileSync('moderators.yaml', 'utf8');
        const data = yaml.safeLoad(yamlContent);

        // Check if there's a moderators array in the data
        if (Array.isArray(data.moderators)) {
            // Determine if any moderator matches the provided credentials
            const isMod = data.moderators.some(mod => 
                mod.username === username && mod.password === password
            );

            res.json({ success: true, isModerator: isMod });
        } else {
            // If moderators array doesn't exist, return false
            res.status(400).json({ success: false, message: "Invalid configuration." });
        }
    } catch (err) {
        console.error('Error checking moderator:', err);
        // Return 500 status if there's an error reading the file or parsing data
        res.status(500).json({ 
            success: false, 
            message: "Internal server error. Please try again later." 
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

