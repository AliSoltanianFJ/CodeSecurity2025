from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os
import hmac
from werkzeug.security import generate_password_hash, check_password_hash

app = Flask(__name__)

@app.route("/account/password", methods=["POST"])
def change_password():
    # Validate input length
    if len(request.form) != 3:
        return "Invalid request.", 400

    user_id = request.form.get("user_id")
    current_password = request.form.get("current_password")
    new_password = request.form.get("new_password")

    # Check for empty fields or null values
    if not all([user_id, current_password, new_password]):
        return "Missing required data.", 400

    # Enforce length limits (adjust as needed)
    MAX_USER_ID_LEN = 50
    MAX_PASSWORD_LEN = 128
    if len(user_id) > MAX_USER_ID_LEN or len(new_password) > MAX_PASSWORD_LEN:
        return "Input too long.", 400

    # Validate password complexity: at least one uppercase, lowercase, digit/symbol; length 8-64
    if not re.fullmatch(r'^(?=.*[A-Za-z])(?=.*\d|[_@#$%^&+=]).{8,64}$', new_password):
        return "Password must be between 8 and 64 characters and contain at least one letter and number/symbol.", 400

    # Validate user_id format (optional)
    if not re.match(r'^[a-zA-Z0-9_-]+$', user_id):
        return "Invalid username format.", 400

    try:
        db = MySQLdb.connect(
            host=os.environ.get("DB_HOST", "localhost"),
            user=os.environ.get("DB_USER", "root"),
            passwd=os.environ.get("DB_PASS", ""),
            db=os.environ.get("DB_NAME", "users")
        )
        cursor = db.cursor()

        # Check if user exists
        cursor.execute("SELECT password_hash FROM users WHERE user_id = %s", (user_id,))
        row = cursor.fetchone()
        if not row:
            return "User not found.", 404

        stored_hash = row[0]

        # Verify current password against hash
        if not check_password_hash(stored_hash, current_password):
            return "Invalid current password.", 401

        # Hash new password securely
        new_hash = generate_password_hash(new_password)

        # Update password in database
        cursor.execute("UPDATE users SET password_hash = %s WHERE user_id = %s", (new_hash, user_id))
        db.commit()

        return "Password updated successfully.", 200

    except Exception as e:
        print("Error updating password:", e)
        return "Internal server error.", 500
    finally:
        if 'db' in locals():
            try:
                db.close()
            except Exception as e:
                pass