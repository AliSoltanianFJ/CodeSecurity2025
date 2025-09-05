
/**
 * function which says hello to the user
 */
const express = require("express");
const { param, validationResult } = require('express-validator');
const app = express();

app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * function which says hello to the user
 */
app.get("/hello/:username", [
    param('username').isAlphanumeric().isLength({ min: 1, max: 20 }).trim().escape()
], (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        return res.status(400).send('Invalid username');
    }
    const username = req.params.username;
    res.setHeader('Content-Type', 'text/html; charset=utf-8');
    res.send(`<p>Hello, ${username}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

