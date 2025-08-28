const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
/**
 * return true if they are a moderator
 */
app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    // Example: Check moderator credentials from a YAML file (secure alternative)
    // Assume moderators.yaml contains:
    // moderators:
    //   - username: admin
    //     password: $2b$10$hashedpasswordhere
    //   - username: mod
    //     password: $2b$10$anotherhashedpassword

    const fs = require("fs");
    const bcrypt = require("bcrypt");

    try {
        const fileContents = fs.readFileSync("moderators.yaml", "utf8");
        const data = yaml.load(fileContents);

        const moderator = data.moderators.find(
            (mod) => mod.username === username
        );

        if (moderator && bcrypt.compareSync(password, moderator.password)) {
            return res.json({ isModerator: true });
        }
    } catch (err) {
        // Log error or handle as needed
    }

    res.json({ isModerator: false });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
