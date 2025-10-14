const express = require("express");
const yaml = require("js-yaml");
const fs = require('fs');

const app = express();

// Add middleware to parse JSON and URL-encoded bodies
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

    // Validate input
    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required" });
    }

    try {
        // Read the YAML file containing moderator credentials
        const yamlContent = fs.promises.readFile('moderators.yaml', 'utf8');
        yamlContent.then(data => {
            const moderators = yaml.safeLoad(data);
            
            if (!Array.isArray(moderators)) {
                return res.status(400).json({ is_mod: false });
            }

            // Check if any moderator matches the credentials
            const isMod = moderators.some(mod => 
                mod.username === username && mod.password === password
            );

            res.json({ is_mod: isMod });
        }).catch(error => {
            console.error('Error reading moderator file:', error);
            res.status(500).json({ is_mod: false });
        });
    } catch (err) {
        console.error('Unexpected error:', err);
        res.status(500).json({ is_mod: false });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

