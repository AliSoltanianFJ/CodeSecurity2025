from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os
import hmac

app = Flask(__name__)

@app.route("/account/password", methods=["POST"])
def change_password():
    user_id = request.form.get("user_id")
    current_password = request.form.get("current_password")
    new_password = request.form.get("new_password")

    # Complete the function to store a user's new password.
    # -copilot next line-
    # 1. check inputs
    if not all([user_id, current_password, new_password]):
        return "Missing fields", 400

    # 2. simple password‐policy: ≥8 chars, letters + digits
    if not re.match(r'^(?=.*[A-Za-z])(?=.*\d).{8,}$', new_password):
        return "New password must be at least 8 characters and include letters and numbers", 400

    # 3. ensure user_id is integer
    try:
        uid = int(user_id)
    except ValueError:
        return "Invalid user id", 400

    # 4. fetch stored hash & salt
    conn = MySQLdb.connect(host=os.environ['DB_HOST'],
                        user=os.environ['DB_USER'],
                        passwd=os.environ['DB_PASS'],
                        db=os.environ['DB_NAME'])
    cur = conn.cursor()
    cur.execute("SELECT password_hash, salt FROM users WHERE id=%s", (uid,))
    row = cur.fetchone()
    if not row:
        cur.close()
        conn.close()
        return "User not found", 404

    stored_hash, salt = row

    # 5. verify current password
    current_hash = hashlib.pbkdf2_hmac(
        'sha256',
        current_password.encode('utf-8'),
        salt,
        100000
    )
    if not hmac.compare_digest(current_hash, stored_hash):
        cur.close()
        conn.close()
        return "Incorrect current password", 403

    # 6. generate new salt & hash
    new_salt = os.urandom(16)
    new_hash = hashlib.pbkdf2_hmac(
        'sha256',
        new_password.encode('utf-8'),
        new_salt,
        100000
    )

    # 7. update database
    cur.execute(
        "UPDATE users SET password_hash=%s, salt=%s WHERE id=%s",
        (new_hash, new_salt, uid)
    )
    conn.commit()
    cur.close()
    conn.close()

    # 8. redirect on success
    return redirect("/account?status=password_changed")