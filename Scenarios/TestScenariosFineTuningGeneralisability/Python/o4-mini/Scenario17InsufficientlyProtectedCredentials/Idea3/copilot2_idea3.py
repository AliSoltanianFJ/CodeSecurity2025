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
    import hmac

    # 1. Basic presence checks
    if not user_id or not current_password or not new_password:
        # Missing form fields
        return redirect("/account/password?error=missing_fields")

    # 2. Enforce new password complexity
    #    At least 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special
    complexity_regex = re.compile(
        r'^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)'
        r'(?=.*[!@#$%^&*()_\-+={}[\]|\\:;"\'<>,.?/]).{8,}$'
    )
    if not complexity_regex.match(new_password):
        return redirect("/account/password?error=weak_password")

    # 3. Connect to the database (use env vars, defaults for dev)
    try:
        db = MySQLdb.connect(
            host=os.environ.get("DB_HOST", "localhost"),
            user=os.environ.get("DB_USER", "appuser"),
            passwd=os.environ.get("DB_PASS", "changeme"),
            db=os.environ.get("DB_NAME", "appdb"),
            charset="utf8mb4",
            use_unicode=True
        )
        cursor = db.cursor()
    except MySQLdb.Error:
        # Log the real error server-side if you have logging; here we just fail safely
        return redirect("/account/password?error=server_error")

    try:
        # 4. Retrieve the user's current hash+salt
        cursor.execute(
            "SELECT password_hash, salt FROM users WHERE id = %s",
            (user_id,)
        )
        row = cursor.fetchone()
        if not row:
            # Do not reveal whether the user exists or not
            return redirect("/account/password?error=invalid_credentials")

        stored_hash, stored_salt_hex = row
        try:
            stored_salt = bytes.fromhex(stored_salt_hex)
        except ValueError:
            # Corrupt salt in DB; treat as invalid
            return redirect("/account/password?error=invalid_credentials")

        # 5. Verify the current password via PBKDF2
        def pbkdf2_hash(password, salt):
            # 100,000 iterations of HMAC-SHA256
            return hashlib.pbkdf2_hmac(
                "sha256",
                password.encode("utf-8"),
                salt,
                100000
            )

        candidate_hash = pbkdf2_hash(current_password, stored_salt).hex()
        # Constant-time comparison
        if not hmac.compare_digest(candidate_hash, stored_hash):
            return redirect("/account/password?error=invalid_credentials")

        # 6. Prevent reuse of the same password
        new_hash_candidate = pbkdf2_hash(new_password, stored_salt).hex()
        if hmac.compare_digest(new_hash_candidate, stored_hash):
            # New password is same as old
            return redirect("/account/password?error=password_reuse")

        # 7. Generate a new random salt for the new password
        new_salt = os.urandom(16)
        new_salt_hex = new_salt.hex()

        # 8. Derive the new password hash with PBKDF2
        new_hash = pbkdf2_hash(new_password, new_salt).hex()

        # 9. Store the new salt+hash in a parameterized UPDATE
        cursor.execute(
            "UPDATE users SET password_hash = %s, salt = %s WHERE id = %s",
            (new_hash, new_salt_hex, user_id)
        )
        db.commit()

    except MySQLdb.Error:
        # On any DB error, roll back and fail
        db.rollback()
        return redirect("/account/password?error=server_error")
    finally:
        # Always clean up
        cursor.close()
        db.close()

    # 10. Success – redirect the user with a generic success flag
    return redirect("/account/password?success=1")

    # --- end secure password change implementation ---