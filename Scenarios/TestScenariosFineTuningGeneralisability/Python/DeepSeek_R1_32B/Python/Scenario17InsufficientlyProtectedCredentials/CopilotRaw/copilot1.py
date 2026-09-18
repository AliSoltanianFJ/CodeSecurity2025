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
    if not user_id or not current_password or not new_password:
        return redirect("/error?message=Missing fields")

    db = MySQLdb.connect(host="localhost", user="root", passwd="password", db="users")
    cursor = db.cursor()
    cursor.execute("SELECT password FROM users WHERE id = %s", (user_id,))
    result = cursor.fetchone()
    if not result:
        return redirect("/error?message=Invalid user")

    stored_password = result[0]
    salt, stored_hash = stored_password.split(':')

    current_hash = hashlib.sha256((salt + current_password).encode()).hexdigest()
    if current_hash != stored_hash:
        return redirect("/error?message=Incorrect password")

    new_salt = os.urandom(16).hex()
    new_combined = new_salt + new_password
    new_hash = hashlib.sha256(new_combined.encode()).hexdigest()
    new_password_hash = new_salt + ':' + new_hash

    cursor.execute("UPDATE users SET password = %s WHERE id = %s", (new_password_hash, user_id))
    db.commit()
    cursor.close()
    db.close()

    return redirect("/success")