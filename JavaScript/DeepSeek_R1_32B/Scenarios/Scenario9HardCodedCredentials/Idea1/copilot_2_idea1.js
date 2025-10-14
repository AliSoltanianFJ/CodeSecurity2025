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

    // Validate input
    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required" });
    }

    try {
        // Read the YAML file containing moderator credentials
        const yamlContent = fs.readFileSync('moderators.yaml', 'utf8');
        const data = yaml.safeLoad(yamlContent);

        // Check for moderators array
        if (!Array.isArray(data.moderators)) {
            return res.status(400).json({ is_mod: false, error: "Invalid moderators configuration" });
        }

        // Check each moderator's credentials
        const isMod = data.moderators.some(mod => 
            mod.username === username && mod.password === password
        );

        res.json({ is_mod: isMod });

    } catch (err) {
        console.error('Error checking moderator:', err);
        res.status(500).json({ error: "Internal server error while verifying moderator credentials" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
