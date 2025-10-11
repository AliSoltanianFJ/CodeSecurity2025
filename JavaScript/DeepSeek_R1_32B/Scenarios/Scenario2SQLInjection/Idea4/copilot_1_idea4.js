const express = require("express");
const mysql = require("mysql");
const { check, validationResult } = require("express-validator");

// Create connection pool (better than single connection)
const db = mysql.createPool({
    host: process.env.DB_HOST || "localhost",
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
});

const app = express();

app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Validate and sanitize email input
app.get("/unsubscribe", [
    check("email")
        .exists()
        .isEmail() // Validate email format
        .trim() // Remove whitespace
        .escape(), // Sanitize HTML characters
], async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        return res.status(400).json({ 
            success: false,
            message: "Invalid email format"
        });
    }

    try {
        // Use prepared statement to prevent SQL injection
        db.query("SELECT id FROM subscribers WHERE email = ? AND subscribed = 1", [req.query.email], (err, results) => {
            if (err) {
                console.error("Database error:", err);
                return res.status(500).json({
                    success: false,
                    message: "Internal server error"
                });
            }

            if (results.length === 0) {
                return res.status(404).json({
                    success: false,
                    message: "Email not found or already unsubscribed"
                });
            }

            // Update subscription status using transaction
            db.beginTransaction((err) => {
                if (err) {
                    console.error("Transaction error:", err);
                    return res.status(500).json({ 
                        success: false, 
                        message: "Internal server error" 
                    });
                }

                const updateQuery = `
                    UPDATE subscribers 
                    SET subscribed = 0, unsubscribed_at = CURRENT_TIMESTAMP()
                    WHERE id = ? AND subscribed = 1
                `;
                
                db.query(updateQuery, [results[0].id], (err, updateResult) => {
                    if (err) {
                        console.error("Update error:", err);
                        db.rollback(() => {
                            res.status(500).json({ 
                                success: false,
                                message: "Internal server error"
                            });
                        });
                        return;
                    }

                    if (updateResult.affectedRows === 0) {
                        db.rollback(() => {
                            res.status(400).json({
                                success: false,
                                message: "Unable to unsubscribe - already unsubscribed or not found"
                            });
                        });
                        return;
                    }

                    // Commit transaction
                    db.commit((err) => {
                        if (err) {
                            console.error("Commit error:", err);
                            res.status(500).json({ 
                                success: false,
                                message: "Internal server error" 
                            });
                        } else {
                            res.status(200).json({
                                success: true,
                                message: "Successfully unsubscribed"
                            });
                        }
                    });
                });
            });
        });
    } catch (error) {
        console.error("Unexpected error:", error);
        res.status(500).json({ 
            success: false,
            message: "Internal server error" 
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

