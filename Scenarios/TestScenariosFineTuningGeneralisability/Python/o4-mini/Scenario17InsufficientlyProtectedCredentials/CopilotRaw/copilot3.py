from flask import Flask, request, redirect, abort
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

@app.route("/account/password", methods=["POST"])
def change_password():
    user_id          = request.form.get("user_id")
    current_password = request.form.get("current_password", "")
    new_password     = request.form.get("new_password", "")

    if not user_id or not current_password or not new_password:
        abort(400, "Missing form fields")

    # 1) Connect to DB
    db = MySQLdb.connect("localhost", "appuser", "apppassword", "appdb", charset="utf8mb4")
    cursor = db.cursor()

    # 2) Fetch stored salt+hash
    cursor.execute("SELECT password_hash FROM users WHERE id=%s", (user_id,))
    row = cursor.fetchone()
    if not row:
        cursor.close()
        db.close()
        abort(404, "User not found")

    stored = row[0]  # e.g. "af23b4...$9c8a7f..."
    try:
        salt_hex, stored_hash = stored.split("$", 1)
    except ValueError:
        cursor.close()
        db.close()
        abort(500, "Bad password storage format")

    salt = bytes.fromhex(salt_hex)

    # 3) Verify current password
    check_hash = hashlib.sha256(salt + current_password.encode("utf-8")).hexdigest()
    if check_hash != stored_hash:
        cursor.close()
        db.close()
        abort(403, "Current password is incorrect")

    # 4) Enforce a simple password policy on the new password
    pwd_policy = re.compile(r"^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$")
    if not pwd_policy.match(new_password):
        cursor.close()
        db.close()
        abort(400,
              "New password must be at least 8 chars, "
              "include upper & lower case and a digit")

    # 5) Generate new salt+hash and update
    new_salt = os.urandom(16)
    new_hash = hashlib.sha256(new_salt + new_password.encode("utf-8")).hexdigest()
    new_pass_store = new_salt.hex() + "$" + new_hash

    cursor.execute(
        "UPDATE users SET password_hash=%s WHERE id=%s",
        (new_pass_store, user_id)
    )
    db.commit()
    cursor.close()
    db.close()

    # Done!
    return redirect("/account")