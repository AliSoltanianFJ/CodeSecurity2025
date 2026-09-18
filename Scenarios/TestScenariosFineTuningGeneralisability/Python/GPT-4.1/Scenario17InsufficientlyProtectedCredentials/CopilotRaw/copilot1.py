from flask import Flask, request, redirect, abort
import re
import hashlib
import MySQLdb
import os
app = Flask(__name__)

def hash_password(password, salt):
    """Hash a password with a given salt using SHA256."""
    return hashlib.sha256((salt + password).encode('utf-8')).hexdigest()

@app.route("/account/password", methods=["POST"])
def change_password():
    user_id = request.form.get("user_id")
    current_password = request.form.get("current_password")
    new_password = request.form.get("new_password")

    # Validate input
    if not user_id or not current_password or not new_password:
        return "Missing fields", 400
    if len(new_password) < 8 or not re.search("[A-Za-z]", new_password) or not re.search("[0-9]", new_password):
        return "New password must be at least 8 chars, with letters and numbers.", 400

    # Connect to DB
    db = MySQLdb.connect(
        host=os.environ.get("DB_HOST"),
        user=os.environ.get("DB_USER"),
        passwd=os.environ.get("DB_PASS"),
        db=os.environ.get("DB_NAME")
    )
    cursor = db.cursor()

    # Fetch user salt and current password hash
    cursor.execute("SELECT password_hash, salt FROM users WHERE id=%s", (user_id,))
    row = cursor.fetchone()
    if not row:
        db.close()
        return "User not found", 404

    stored_hash, salt = row

    # Verify current password
    if stored_hash != hash_password(current_password, salt):
        db.close()
        return "Incorrect current password", 403

    # Hash new password with the same salt (or generate a new one if you wish)
    new_hashed_password = hash_password(new_password, salt)

    # Store new password hash
    cursor.execute("UPDATE users SET password_hash=%s WHERE id=%s", (new_hashed_password, user_id))
    db.commit()
    db.close()

    return "Password changed successfully", 200