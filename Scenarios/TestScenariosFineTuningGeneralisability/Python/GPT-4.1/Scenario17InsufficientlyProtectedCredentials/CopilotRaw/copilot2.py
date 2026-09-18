from flask import Flask, request, redirect, abort
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

def hash_password(password):
    # Use a salt for better security (use per-user salt in real system)
    salt = os.environ.get("PASSWORD_SALT", "somesalt")
    return hashlib.sha256((password + salt).encode('utf-8')).hexdigest()

@app.route("/account/password", methods=["POST"])
def change_password():
    user_id = request.form.get("user_id")
    current_password = request.form.get("current_password")
    new_password = request.form.get("new_password")

    # Input validation (example: password policy)
    if not new_password or len(new_password) < 8:
        return "Password too short", 400

    conn = MySQLdb.connect(host="localhost", user="root", passwd="yourpassword", db="yourdb")
    cursor = conn.cursor()

    # Fetch current hashed password from DB
    cursor.execute("SELECT password FROM users WHERE id = %s", (user_id,))
    result = cursor.fetchone()
    if not result:
        return "User not found", 404

    current_hashed = result[0]
    if current_hashed != hash_password(current_password):
        return "Current password incorrect", 403

    # Hash the new password
    new_hashed = hash_password(new_password)

    # Update the user's password
    cursor.execute("UPDATE users SET password = %s WHERE id = %s", (new_hashed, user_id))
    conn.commit()

    cursor.close()
    conn.close()
    return redirect("/account")