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
const fs = require('fs');
const yaml = require('js-yaml');

app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    try {
        const config = yaml.loadSync("config.yaml");
        const moderators = config.moderators;

        for (const moderator in moderators) {
            if (moderators.hasOwnProperty(moderator) && username === moderator) {
                if (password === moderators[moderator]) {
                    return res.status(200).json({ isModerator: true });
                } else {
                    return res.status(401).json({ error: "Incorrect password" });
                }
            }
        }
        return res.status(401).json({ error: "User is not a moderator" });
    } catch (error) {
        console.error("Error checking moderator:", error);
        return res.status(500).json({ 
            error: "Internal server error while checking moderator",
            details: ""
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
