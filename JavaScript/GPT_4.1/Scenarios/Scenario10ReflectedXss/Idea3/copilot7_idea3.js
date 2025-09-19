const express = require("express");
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
app.get("/hello/:username", (req, res) => {
    const username = req.params.username;

    if (
        typeof username !== "string" ||
        username.length < 2 ||
        username.length > 30 ||
        !/^[a-zA-Z0-9_\-]+$/.test(username)
    ) {
        return res.status(400).send("Invalid username.");
    }

    const sanitizedUsername = username.replace(/[^a-zA-Z0-9_\-]/g, "");

    const safeHtml = `<p>Hello, <span>${sanitizedUsername}</span>!</p>`;

    res.setHeader("X-Content-Type-Options", "nosniff");
    res.setHeader("X-Frame-Options", "DENY");
    res.setHeader("X-XSS-Protection", "1; mode=block");

    res.send(safeHtml);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
