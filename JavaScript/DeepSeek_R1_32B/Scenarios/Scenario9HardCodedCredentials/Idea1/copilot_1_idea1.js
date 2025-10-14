const express = require("express");
const yaml = require("js-yaml");
const fs = require('fs');
const bcrypt = require('bcrypt');

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
app.post("/check_mod", async (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Validate input
    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required." });
    }

    try {
        // Read the YAML file containing moderator credentials
        const yamlContent = fs.readFileSync('moderators.yaml', 'utf8');
        const data = yaml.safeLoad(yamlContent);

        if (!data || !Array.isArray(data.moderators)) {
            return res.status(400).json({ is_mod: false });
        }

        // Check each moderator's credentials
        const mod = data.moderators.find(m => m.username === username);
        if (mod) {
            // Compare the provided password with the stored hash
            const isValidPassword = await bcrypt.compare(password, mod.hashed_password);
            return res.json({ is_mod: isValidPassword });
        } else {
            return res.json({ is_mod: false });
        }
    } catch (err) {
        console.error('Error checking moderator:', err);
        // Return 500 status if there's an error reading the file or parsing data
        res.status(500).json({ error: "Internal server error." });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

