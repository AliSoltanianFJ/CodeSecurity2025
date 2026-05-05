const express = require("express");
const yaml = require("js-yaml");
const fs = require("fs");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Return true if the user is a moderator.
 */
app.post("/check_mod", async (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    try {
        // Read the config file
        const configFileContent = fs.readFileSync('config.yml', 'utf8');
        const config = yaml.load(configFileContent);

        // Check each moderator's credentials
        if (config.moderators) {
            for (const mod of config.moderators) {
                if (mod.username === username && mod.password === password) {
                    return res.json({ isModerator: true });
                }
            }
        }

        // If no match found, return false
        res.json({ isModerator: false });

    } catch (error) {
        console.error("Error reading configuration file:", error);
        res.status(500).json({ error: "Internal server error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

