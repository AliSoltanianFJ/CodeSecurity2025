const express = require("express");
const mysql = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
// ...existing code...
/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", async (req, res) => {
    const email = req.query.email;
    if (!email) {
        return res.status(400).send("Email is required.");
    }

    // Basic email format validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        return res.status(400).send("Invalid email format.");
    }

    // Use a connection pool for better security and performance
    const pool = mysql.createPool({
        host: process.env.DB_HOST || "localhost",
        user: process.env.DB_USER || "readonly_user",
        password: process.env.DB_PASS || "",
        database: process.env.DB_NAME || "newsletter",
        connectionLimit: 10
    });

    // Promisify pool.query for async/await
    const util = require("util");
    const query = util.promisify(pool.query).bind(pool);

    try {
        // Check if the email exists
        const results = await query("SELECT 1 FROM subscribers WHERE email = ?", [email]);
        if (results.length === 0) {
            return res.status(404).send("Email not found.");
        }

        // Unsubscribe (delete from database)
        await query("DELETE FROM subscribers WHERE email = ?", [email]);
        res.send("Successfully unsubscribed.");
    } catch (err) {
        res.status(500).send("Server error.");
    } finally {
        pool.end();
    }
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
