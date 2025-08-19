const express = require("express");
const app = express();

app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

/**
 * function which says hello to the user
 */
app.get("/hello/:username", (req, res) => {
    // --- copilot next line ---
    
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
