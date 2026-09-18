from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os
import hmac

app = Flask(__name__)

@app.route("/account/password", methods=["POST"])
def change_password():
    # Retrieve form data
    user_id = request.form.get("user_id")
    current_password = request.form.get("current_password")
    new_password = request.form.get("new_password")

    # Validate input length and complexity
    if not user_id or len(user_id) > 100:
        return "Invalid input", 400
    if not current_password or len(current_password) < 8 or len(current_password) > 128:
        return "Current password invalid", 400
    if not new_password or len(new_password) < 8 or len(new_password) > 128:
        return "New password invalid", 400

    # Connect to database securely
    try:
        db = MySQLdb.connect(
            host=os.environ.get("DB_HOST", "localhost"),
            user=os.environ.get("DB_USER", "root"),
            passwd=os.environ.get("DB_PASS", ""),
            db=os.environ.get("DB_NAME", "test")
        )
    except Exception as e:
        return "Database connection failed", 500

    # Check current password using parameterized query
    cursor = db.cursor()
    try:
        cursor.execute("SELECT password_hash, salt FROM users WHERE user_id = %s", (user_id,))
        result = cursor.fetchone()
        if not result:
            return "User not found", 404
        
        stored_hash, salt = result

        # Verify current password
        computed_hash = hashlib.pbkdf2_hmac('sha256', current_password.encode('utf-8'), salt.encode('utf-8'), 100000).hex()
        if not hmac.compare_digest(computed_hash, stored_hash):
            return "Incorrect current password", 400

    except Exception as e:
        db.rollback()
        return "Internal server error", 500
    finally:
        cursor.close()

    # Generate new salt and hash
    try:
        new_salt = os.urandom(16).hex()
        new_hash = hashlib.pbkdf2_hmac('sha256', new_password.encode('utf-8'), new_salt.encode('utf-8'), 100000).hex()

    except Exception as e:
        return "Internal server error", 500

    # Update password in database
    try:
        cursor = db.cursor()
        cursor.execute("UPDATE users SET password_hash = %s, salt = %s WHERE user_id = %s",
                      (new_hash, new_salt, user_id))
        db.commit()

    except Exception as e:
        db.rollback()
        return "Internal server error", 500
    finally:
        cursor.close()
        db.close()

    return "Password updated successfully", 200