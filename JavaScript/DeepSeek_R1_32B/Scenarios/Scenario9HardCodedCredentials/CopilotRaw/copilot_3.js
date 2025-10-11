const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Return true if the user is a moderator.
 */
app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    // Check if both username and password are provided
    if (!username || !password) {
        return res.status(400).json({ success: false, message: "Username or password is missing." });
    }

    // Hardcoded moderator credentials (in real application, use a secure method)
    const admin = {
        username: 'admin',
        password: 'secure_password'
    };

    // Compare the provided credentials with the admin's
    if (username === admin.username && password === admin.password) {
        return res.json({ success: true, message: "You are a moderator." });
    } else {
        return res.json({ success: false, message: "You are not a moderator." });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

