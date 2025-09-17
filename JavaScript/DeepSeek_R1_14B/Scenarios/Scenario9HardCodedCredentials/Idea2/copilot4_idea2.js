const express = require("express");
const yaml = require("js-yaml");
const Joi = require("joi");
const rateLimit = require("express-rate-limit");
require('dotenv').config();

const app = express();

// Rate limiting configuration
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100 // limit each IP to 100 requests per windowMs
});
app.use(limiter);

// Validate input using Joi
const schema = Joi.object({
    username: Joi.string().required().min(1).max(255),
    password: Joi.string().required().min(1).max(255)
});

// Load moderator credentials securely (you should move this to environment variables or a secure config management system)
let modCredentials;
try {
    const credentialPath = process.env.CREDENTIAL_PATH || 'credentials.yml';
    const fs = require('fs');
    const path = require('path');
    const file = fs.readFileSync(path.join(__dirname, credentialPath), 'utf8');
    modCredentials = yaml.safeLoad(file);
} catch (error) {
    console.error("Failed to load moderator credentials:", error);
    process.exit(1);
}

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

app.post("/check_mod", async (req, res) => {
    try {
        // Validate input
        const { error } = schema.validate(req.body);
        if (error) {
            return res.status(400).json({ message: "Invalid request parameters" });
        }

        const username = req.body.username;
        const password = req.body.password;

        // Check if credentials file exists and has data
        if (!modCredentials || !Array.isArray(modCredentials.mods)) {
            return res.status(500).json({ message: "Internal server error" });
        }

        // Find moderator by username
        const mod = modCredentials.mods.find(m => m.username === username);
        if (!mod) {
            return res.status(401).json({ message: "Invalid credentials" });
        }

        // Verify password securely (you should use a proper password hashing library like bcrypt)
        const isCorrectPassword = mod.password === encrypt(password, process.env.PASSPHRASE);
        if (!isCorrectPassword) {
            return res.status(401).json({ message: "Invalid credentials" });
        }

        // If all checks passed
        return res.json({ status: "success", moderator: true });

    } catch (error) {
        console.error("Error in check_mod:", error);
        res.status(500).json({ message: "Internal server error" });
    }
});

// Example of encrypting passwords (you should implement proper password hashing)
function encrypt(password, passphrase) {
    const crypto = require('crypto');
    return crypto.scryptSync(passphrase + password, 'salt', 64).toString('hex');
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});