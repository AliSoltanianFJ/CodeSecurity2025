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

    // Read moderators from YAML file
    try {
        const fs = require('fs');
        const moderatorFile = fs.readFileSync('./moderators.yaml', 'utf8');
        const moderators = yaml.load(moderatorFile);

        // Check if provided credentials match any moderator
        let isModerator = false;
        moderators.forEach(moderator => {
            if (username === moderator.username && password === moderator.password) {
                isModerator = true;
                return; // Exit loop early if found
            }
        });

        res.status(isModerator ? 200 : 401).json({ 
            status: isModerator ? 'success' : 'error',
            message: isModerator ? 'Valid moderator.' : 'Invalid credentials.',
            data: { isModerator }
        });
        
    } catch (err) {
        console.error('Error:', err);
        res.status(500).json({ 
            status: 'error', 
            message: 'Internal server error' 
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
