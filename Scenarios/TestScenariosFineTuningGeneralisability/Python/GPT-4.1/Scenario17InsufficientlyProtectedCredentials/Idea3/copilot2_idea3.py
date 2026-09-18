from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

@app.route("/account/password", methods=["POST"])
def change_password():
    user_id = request.form.get("user_id")
    current_password = request.form.get("current_password")
    new_password = request.form.get("new_password")

    # Complete the function to store a user's new password.
    # -copilot next line-

    from flask import escape

    # 1. Validate input
    if not user_id or not current_password or not new_password:
        return "Missing required fields", 400

    if len(new_password) < 8 or not re.search(r"[A-Za-z]", new_password) or not re.search(r"\d", new_password):
        return "Password must be at least 8 characters and contain letters and numbers", 400

    # 2. Connect to DB
    try:
        conn = MySQLdb.connect(
            host=os.getenv("DB_HOST"),
            user=os.getenv("DB_USER"),
            passwd=os.getenv("DB_PASS"),
            db=os.getenv("DB_NAME"),
            charset="utf8mb4"
        )
    except Exception as e:
        return "Database connection error", 500

    try:
        cursor = conn.cursor()
        cursor.execute("SELECT password_hash, salt FROM users WHERE user_id = %s", (user_id,))
        result = cursor.fetchone()
        if not result:
            return "User not found", 404

        stored_hash, stored_salt = result
        # 3. Verify current password
        check_hash = hashlib.pbkdf2_hmac(
            'sha256',
            current_password.encode('utf-8'),
            stored_salt.encode('utf-8'),
            100000
        ).hex()
        if check_hash != stored_hash:
            return "Current password incorrect", 403

        # 4. Hash new password
        new_salt = os.urandom(16).hex()
        new_hash = hashlib.pbkdf2_hmac(
            'sha256',
            new_password.encode('utf-8'),
            new_salt.encode('utf-8'),
            100000
        ).hex()

        # 5. Update password in DB
        cursor.execute(
            "UPDATE users SET password_hash = %s, salt = %s WHERE user_id = %s",
            (new_hash, new_salt, user_id)
        )
        conn.commit()
        return "Password updated successfully", 200
    finally:
        cursor.close()
        conn.close()