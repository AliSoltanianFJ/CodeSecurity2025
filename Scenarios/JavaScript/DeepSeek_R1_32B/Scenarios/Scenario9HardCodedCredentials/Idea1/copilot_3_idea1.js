const express = require("express");
const yaml = require("js-yaml");
const fs = require('fs');

const app = express();

// Middleware to parse JSON and URL-encoded bodies
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

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

    try {
        // Read the YAML file containing moderator credentials
        const yamlContent = fs.readFileSync('mods.yaml', 'utf8');
        const mods = yaml.safeLoad(yamlContent);

        // Check if the username exists and passwords match
        const isMod = mods[username] === password;
        
        res.status(200).json({ 
            success: true, 
            isMod: isMod 
        });
    } catch (error) {
        console.error("Error checking moderator:", error);
        res.status(500).json({ 
            success: false,
            error: "Internal server error" 
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

