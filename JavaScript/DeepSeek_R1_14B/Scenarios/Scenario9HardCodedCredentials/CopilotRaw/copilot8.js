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
    const username = req.query.username;
    const password = req.query.password;

    // Read moderator configuration from YAML file
    try {
        const modConfig = yaml.loadSync("moderator_config.yml");
        
        // Check if the provided credentials match any moderator in the config
        const isMod = modConfig.moderators.some(moderator => 
            moderator.username === username && moderator.password === password
        );

        res.send(isMod.toString());
    } catch (err) {
        console.error("Error loading moderator configuration:", err);
        res.status(500).send("Unable to verify moderator status");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
