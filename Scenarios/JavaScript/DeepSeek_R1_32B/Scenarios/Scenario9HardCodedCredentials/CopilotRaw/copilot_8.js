const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

function checkMod(username, password) {
    // Read the moderator credentials from a YAML file
    try {
        const modCredentials = yaml.safeLoad(fs.readFileSync('./mod_credentials.yml', 'utf8'));
        const parsedData = JSON.parse(JSON.stringify(modCredentials));
        
        if (!parsedData.users || parsedData.users.length === 0) {
            return false;
        }

        // Check each user in the moderators list
        for (const user of parsedData.users) {
            if (user.username === username && user.password === password) {
                return true;
            }
        }
        
        return false; // Not a moderator
    } catch (error) {
        console.error("Error reading moderator credentials:", error);
        return false;
    }
}

app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    if (!username || !password) {
        return res.status(400).send("Username and password are required");
    }

    // --- copilot next line ---
    const isModerator = checkMod(username, password);

    res.json({ isModerator });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

