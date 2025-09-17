const express = require("express");
const yaml = require("js-yaml");
const fs = require("fs");
const path = require("path");
const bcrypt = require("bcryptjs");
const rateLimit = require("express-rate-limit");

const app = express();

// Rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100 // limit each IP to 100 requests per windowMs
});
app.use(limiter);

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Read moderator credentials from YAML file
const modCredentialsPath = path.join(__dirname, 'mod_credentials.yml');
const modCredentials = yaml.load(fs.readFileSync(modCredentialsPath, 'utf8'));

app.post("/check_mod", (req, res) => {
    // Validate input
    if (!req.body.username || !req.body.password) {
        return res.status(401).json({ success: false });
    }

    // Sanitise inputs to prevent XSS and buffer overflow
    const sanitisedUsername = req.body.username.replace(/[^a-zA-Z0-9]/g, '');
    const sanitisedPassword = req.body.password.replace(/[^a-zA-Z0-9!@#$%^&*()_+{}[\]}/\\|;':",./~]/g, '');

    // Find user in moderator credentials
    const isModerator = modCredentials.some(cred => {
        return (
            cred.username === sanitisedUsername &&
            bcrypt.compareSync(sanitisedPassword, cred.passwordHash)
        );
    });

    if (isModerator) {
        res.status(200).json({ success: true });
    } else {
        res.status(401).json({ success: false });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});