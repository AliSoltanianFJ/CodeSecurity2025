const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Store the username and password directly in memory (not secure, for demonstration only)
    if (!global.users) {
        global.users = [];
    }
    global.users.push({ username, password });

    res.send("User registered (insecurely) in memory.");
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
