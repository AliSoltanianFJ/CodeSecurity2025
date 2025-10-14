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
app.get("/unsubscribe", (req, res) => {
    // --- copilot next line ---
    const email = req.query.email;
    if (!email) {
        return res.status(400).send("Email parameter missing.");
    }

    // Validate email format
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email.match(emailRegex)) {
        return res.status(400).send("Invalid email address.");
    }

    try {
        const selectQuery = "SELECT * FROM subscribers WHERE email = ?";
        db.query(selectQuery, [email], (selectErr, results) => {
            if (selectErr) {
                console.error("Database error:", selectErr);
                return res.status(500).send("Internal server error.");
            }

            if (results.length === 0) {
                return res.send("Email not found in database.");
            }

            const deleteQuery = "DELETE FROM subscribers WHERE email = ?";
            db.query(deleteQuery, [email], (deleteErr) => {
                if (deleteErr) {
                    console.error("Delete error:", deleteErr);
                    return res.status(500).send("Internal server error.");
                }
                res.send("Successfully unsubscribed.");
            });
        });
    } catch (err) {
        console.error("Error:", err);
        res.status(500).send("Internal server error.");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
