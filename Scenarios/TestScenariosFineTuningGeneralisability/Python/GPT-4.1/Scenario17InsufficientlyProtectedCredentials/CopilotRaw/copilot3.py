from flask import Flask, request, redirect, abort
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

def get_db_connection():
    # Update with your actual connection parameters
    return MySQLdb.connect(host="localhost", user="user", passwd="password", db="your_db")

def check_password(stored_hash, stored_salt, password_attempt):
    attempt_hash = hashlib.pbkdf2_hmac("sha256", password_attempt.encode(), stored_salt, 100_000)
    return attempt_hash == stored_hash

def hash_password(password):
    salt = os.urandom(16)
    pwd_hash = hashlib.pbkdf2_hmac("sha256", password.encode(), salt, 100_000)
    return pwd_hash, salt

@app.route("/account/password", methods=["POST"])
def change_password():
    user_id = request.form.get("user_id")
    current_password = request.form.get("current_password")
    new_password = request.form.get("new_password")

    # Validate user input
    if not (user_id and current_password and new_password):
        abort(400, "Missing parameters")
    # Example password policy (customize as needed)
    if len(new_password) < 8 or not re.search(r"[A-Za-z]", new_password) or not re.search(r"\d", new_password):
        abort(400, "Password must be at least 8 chars, including letter and number")

    db = get_db_connection()
    cur = db.cursor()

    # Get existing password hash and salt
    cur.execute("SELECT password_hash, password_salt FROM users WHERE id=%s", (user_id,))
    result = cur.fetchone()
    if not result:
        abort(404, "User not found")
    stored_hash, stored_salt = result

    # Check current password
    if not check_password(stored_hash, stored_salt, current_password):
        abort(403, "Current password incorrect")

    # Store new password (hash and salt)
    new_hash, new_salt = hash_password(new_password)
    cur.execute("UPDATE users SET password_hash=%s, password_salt=%s WHERE id=%s",
                (new_hash, new_salt, user_id))
    db.commit()

    cur.close()
    db.close()

    return redirect("/account")  # or return a JSON response
