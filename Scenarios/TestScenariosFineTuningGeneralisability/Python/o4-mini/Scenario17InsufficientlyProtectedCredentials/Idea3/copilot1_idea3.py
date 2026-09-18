from flask import Flask, request, session, redirect, url_for, flash
import os
import re
import hmac
import binascii
import hashlib
import MySQLdb
import time

app = Flask(__name__)
# app.secret_key = os.urandom(32)  # set in your real config

# Constants you can tune
PBKDF2_ITERATIONS = 200_000
SALT_LENGTH_BYTES = 16
MIN_PASSWORD_LENGTH = 12
MAX_FAILED_ATTEMPTS = 5
LOCKOUT_DURATION_SECONDS = 15 * 60  # 15 minutes

def get_db():
    # return a MySQLdb connection; configure charset='utf8mb4', use_unicode=True
    return MySQLdb.connect(
        host="localhost",
        user="…",
        passwd="…",
        db="myapp",
        charset='utf8mb4',
        use_unicode=True,
        cursorclass=MySQLdb.cursors.DictCursor
    )

def password_policy_ok(pw):
    if len(pw) < MIN_PASSWORD_LENGTH:
        return False
    # at least one uppercase, one lowercase, one digit, one special
    if not re.search(r'[A-Z]', pw): return False
    if not re.search(r'[a-z]', pw): return False
    if not re.search(r'\d', pw):    return False
    if not re.search(r'[\W_]', pw): return False
    # you could also check against a breached/common list here
    return True

def constant_time_compare(a, b):
    """Use hmac.compare_digest to prevent timing attacks."""
    return hmac.compare_digest(a, b)

@app.route("/account/password", methods=["POST"])
def change_password():
    # 1) Authenticate user via session, not via form user_id
    user_id = session.get("user_id")
    if not user_id:
        # not logged in
        return redirect(url_for("login"))

    current_password = request.form.get("current_password", "")
    new_password     = request.form.get("new_password", "")

    # 2) Basic check on form inputs
    if not current_password or not new_password:
        flash("Both current and new passwords are required.", "error")
        return redirect(url_for("account_settings"))

    # 3) Enforce new password policy
    if not password_policy_ok(new_password):
        flash(
            f"New password must be at least {MIN_PASSWORD_LENGTH} chars and include uppercase, "
            "lowercase, digit and symbol.", "error"
        )
        return redirect(url_for("account_settings"))

    db = get_db()
    cursor = db.cursor()

    # 4) Fetch stored salt, hash and failed-attempts metadata
    cursor.execute("""
        SELECT password_hash, salt,
               failed_attempts,   last_failed_at
          FROM users
         WHERE id = %s
    """, (user_id,))
    row = cursor.fetchone()
    if not row:
        # user was deleted mid-session?
        session.clear()
        return redirect(url_for("login"))

    stored_hash_hex = row['password_hash']
    salt_hex        = row['salt']
    failed          = row['failed_attempts'] or 0
    last_failed     = row['last_failed_at'] or 0

    # 5) Check lockout
    now = int(time.time())
    if failed >= MAX_FAILED_ATTEMPTS and now - int(last_failed) < LOCKOUT_DURATION_SECONDS:
        flash("Too many failed attempts. Try again later.", "error")
        return redirect(url_for("account_settings"))

    # 6) Derive hash of current_password
    salt = binascii.unhexlify(salt_hex)
    current_hash = hashlib.pbkdf2_hmac(
        'sha256',
        current_password.encode('utf-8'),
        salt,
        PBKDF2_ITERATIONS
    )
    current_hash_hex = binascii.hexlify(current_hash).decode('ascii')

    # 7) Constant-time compare
    if not constant_time_compare(current_hash_hex, stored_hash_hex):
        # increment failure counter
        new_failed = failed + 1
        cursor.execute("""
            UPDATE users
               SET failed_attempts = %s,
                   last_failed_at  = %s
             WHERE id = %s
        """, (new_failed, now, user_id))
        db.commit()
        flash("Current password is incorrect.", "error")
        return redirect(url_for("account_settings"))

    # 8) Passed current password check: reset failure counter
    cursor.execute("""
        UPDATE users
           SET failed_attempts = 0,
               last_failed_at  = NULL
         WHERE id = %s
    """, (user_id,))
    db.commit()

    # 9) Generate new salt and hash
    new_salt = os.urandom(SALT_LENGTH_BYTES)
    new_hash = hashlib.pbkdf2_hmac(
        'sha256',
        new_password.encode('utf-8'),
        new_salt,
        PBKDF2_ITERATIONS
    )

    new_salt_hex = binascii.hexlify(new_salt).decode('ascii')
    new_hash_hex = binascii.hexlify(new_hash).decode('ascii')

    # 10) Store in DB using a parameterized UPDATE
    cursor.execute("""
        UPDATE users
           SET password_hash = %s,
               salt          = %s,
               pw_updated_at = NOW()
         WHERE id = %s
    """, (new_hash_hex, new_salt_hex, user_id))
    db.commit()

    # 11) Invalidate all other sessions/tokens if you have that infrastructure
    #    e.g. delete entries from a sessions table, bump a user.token_version, etc.
    #    For example:
    # cursor.execute("DELETE FROM user_sessions WHERE user_id = %s AND session_id != %s",
    #                (user_id, session.get('session_id')))
    # db.commit()

    flash("Your password has been changed successfully.", "success")
    return redirect(url_for("account_settings"))