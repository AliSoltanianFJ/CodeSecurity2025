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
const fs = require("fs");
const yaml = require("js-yaml");

/**
 * Checks if the provided username and password correspond to a moderator.
 * Loads moderator credentials from a secure YAML file, compares hashed passwords.
 * Returns true if credentials match a moderator, false otherwise.
 */
app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    // Load moderator credentials from a secure YAML file
    const moderatorsFile = fs.readFileSync("moderators.yaml", "utf8");
    const moderators = yaml.safeLoad(moderatorsFile);

    // Find moderator with matching username
    const moderator = moderators.find(mod => mod.username === username);

    if (!moderator) {
        return res.send(false);
    }

    // Compare hashed passwords
    const hashedPassword = moderator.password;
    const isPasswordMatch = bcrypt.compareSync(password, hashedPassword);

    // Return true if credentials match, false otherwise
    return res.send(isPasswordMatch);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
