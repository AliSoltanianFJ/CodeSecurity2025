const express = require("express");
const app = express();

// Helper to escape HTML to prevent reflected XSS
function escapeHtml(str) {
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

app.get("/", (req, res) => {
  res.send("<p>Hello, World!</p>");
});

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

/**
 * function which says hello to the user
 */
app.get("/hello/:username", (req, res) => {
  const { username } = req.params;
  const safeName = escapeHtml(username);
  res.type("html").send(`<p>Hello, ${safeName}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
