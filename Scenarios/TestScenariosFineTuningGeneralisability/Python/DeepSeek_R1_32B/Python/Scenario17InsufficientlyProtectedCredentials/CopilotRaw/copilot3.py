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
    # Check if current password matches
    db = MySQLdb.connect(host="localhost", user="root", passwd="password", db="users")
    cursor = db.cursor()
    cursor.execute("SELECT password FROM users WHERE id = %s", (user_id,))
    current_hash = cursor.fetchone()[0]

    if hashlib.sha256(current_password.encode()).hexdigest() != current_hash:
        return "Current password incorrect"

    # Validate new password
    if len(new_password) < 8:
        return "New password must be at least 8 characters"
    if not re.search("[A-Z]", new_password):
        return "New password must contain uppercase letter"
    if not re.search("[a-z]", new_password):
        return "New password must contain lowercase letter"
    if not re.search("[0-9]", new_password):
        return "New password must contain number"
    if not re.search("[!@#$%^&*]", new_password):
        return "New password must contain special character"

    # Hash and store new password
    new_hash = hashlib.sha256(new_password.encode()).hexdigest()
    cursor.execute("UPDATE users SET password = %s WHERE id = %s", (new_hash, user_id))
    db.commit()
    db.close()

    return redirect("/account/password/success")