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

    # Validate input
    if not user_id or not current_password or not new_password:
        return "Missing fields", 400

    # Sanitize input (basic)
    user_id = re.sub(r'\D', '', user_id)  # keep only digits

    # Validate new password strength
    if len(new_password) < 12 or not re.search(r'[A-Z]', new_password) or not re.search(r'[a-z]', new_password) or not re.search(r'\d', new_password) or not re.search(r'\W', new_password):
        return "Password not strong enough", 400

    # Connect to DB securely
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST'),
            user=os.environ.get('DB_USER'),
            passwd=os.environ.get('DB_PASS'),
            db=os.environ.get('DB_NAME')
        )
    except Exception as e:
        return "DB connection error", 500

    cursor = db.cursor()

    # Fetch and verify current password hash
    try:
        cursor.execute("SELECT password_hash, password_salt FROM users WHERE id = %s", (user_id,))
        row = cursor.fetchone()
        if not row:
            return "User not found", 404

        stored_hash, stored_salt = row
        calc_hash = hashlib.pbkdf2_hmac(
            'sha256',
            current_password.encode('utf-8'),
            stored_salt.encode('utf-8'),
            100_000
        ).hex()
        if calc_hash != stored_hash:
            return "Current password incorrect", 403
    except Exception as e:
        return "Error verifying password", 500

    # Generate new salt and hash new password
    new_salt = os.urandom(16).hex()
    new_hash = hashlib.pbkdf2_hmac(
        'sha256',
        new_password.encode('utf-8'),
        new_salt.encode('utf-8'),
        100_000
    ).hex()

    # Update password in DB using parameterized query
    try:
        cursor.execute(
            "UPDATE users SET password_hash = %s, password_salt = %s WHERE id = %s",
            (new_hash, new_salt, user_id)
        )
        db.commit()
    except Exception as e:
        db.rollback()
        return "Password update failed", 500
    finally:
        cursor.close()
        db.close()

    return redirect("/account/profile")