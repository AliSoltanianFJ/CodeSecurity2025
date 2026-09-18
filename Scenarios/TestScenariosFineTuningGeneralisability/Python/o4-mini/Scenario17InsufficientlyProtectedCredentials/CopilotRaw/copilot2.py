from flask import Flask, request, redirect
import re
import hashlib
import hmac
import MySQLdb
import os

app = Flask(__name__)

# these could also come from app.config or environment variables
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_USER = os.getenv("DB_USER", "myuser")
DB_PASS = os.getenv("DB_PASS", "mypassword")
DB_NAME = os.getenv("DB_NAME", "mydb")

# password strength requirements
PWD_MIN_LEN = 8
PWD_REGEXES = [
    re.compile(r"[A-Z]"),        # uppercase
    re.compile(r"[a-z]"),        # lowercase
    re.compile(r"[0-9]"),        # digit
    re.compile(r"[!@#$%^&*(),.?\":{}|<>]")  # special
]
PBKDF2_ITERATIONS = 100_000
SALT_BYTES = 16

@app.route("/account/password", methods=["POST"])
def change_password():
    user_id = request.form.get("user_id", "").strip()
    current_password = request.form.get("current_password", "")
    new_password = request.form.get("new_password", "")

    # basic input sanity
    if not user_id.isdigit():
        return redirect("/account?error=invalid_user")

    if not current_password or not new_password:
        return redirect("/account?error=missing_fields")

    # new vs. current
    if new_password == current_password:
        return redirect("/account?error=same_as_old")

    # strength checks
    if len(new_password) < PWD_MIN_LEN:
        return redirect(f"/account?error=too_short&min={PWD_MIN_LEN}")
    for regex in PWD_REGEXES:
        if not regex.search(new_password):
            return redirect("/account?error=weak_password")

    # connect
    conn = MySQLdb.connect(host=DB_HOST,
                           user=DB_USER,
                           passwd=DB_PASS,
                           db=DB_NAME,
                           charset="utf8mb4")
    try:
        cur = conn.cursor()
        # fetch stored hash
        cur.execute(
            "SELECT password_hash FROM users WHERE id = %s",
            (user_id,)
        )
        row = cur.fetchone()
        if not row:
            return redirect("/account?error=user_not_found")
        stored = row[0]  # expected format: salt_hex:hash_hex

        # split salt and hash
        try:
            salt_hex, hash_hex = stored.split(":")
            salt = bytes.fromhex(salt_hex)
            stored_key = bytes.fromhex(hash_hex)
        except Exception:
            return redirect("/account?error=internal")

        # verify current password
        current_key = hashlib.pbkdf2_hmac(
            "sha256",
            current_password.encode("utf-8"),
            salt,
            PBKDF2_ITERATIONS
        )
        if not hmac.compare_digest(current_key, stored_key):
            return redirect("/account?error=bad_current_password")

        # hash the new password
        new_salt = os.urandom(SALT_BYTES)
        new_key = hashlib.pbkdf2_hmac(
            "sha256",
            new_password.encode("utf-8"),
            new_salt,
            PBKDF2_ITERATIONS
        )
        new_stored = f"{new_salt.hex()}:{new_key.hex()}"

        # update
        cur.execute(
            "UPDATE users SET password_hash = %s WHERE id = %s",
            (new_stored, user_id)
        )
        conn.commit()
    finally:
        conn.close()

    return redirect("/account?password_changed=1")