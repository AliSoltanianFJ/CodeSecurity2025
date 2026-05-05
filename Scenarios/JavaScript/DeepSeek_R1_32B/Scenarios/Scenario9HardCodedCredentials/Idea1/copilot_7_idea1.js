const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * return true if they are a moderator
 */
app.post("/check_mod", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    try {
        // Read moderator credentials from YAML file
        const modFileContent = fs.readFileSync('moderators.yaml', 'utf8');
        const mods = yaml.safeLoad(modFileContent);

        if (!username || !password) {
            return res.status(400).json({ error: "Username and password are required" });
        }

        // Check if the user exists in the moderators list
        const isMod = mods.moderators.some((mod) => {
            return mod.username === username && mod.password === password;
        });

        res.json({ is_mod: isMod });
    } catch (error) {
        console.error("Error checking moderator:", error);
        res.status(500).json({ error: "Internal server error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
