import os
import re
import hmac
import hashlib
import logging
from datetime import datetime

from flask import Flask, request, session, redirect, abort, flash
from flask_wtf import FlaskForm, CSRFProtect
from wtforms import PasswordField, SubmitField
from wtforms.validators import DataRequired, Length

import MySQLdb
from MySQLdb.cursors import DictCursor

app = Flask(__name__)
app.secret_key = os.environ["FLASK_SECRET_KEY"]  # keep this secret
csrf = CSRFProtect(app)

# Configure logging
logger = logging.getLogger(__name__)
handler = logging.FileHandler("/var/log/myapp/password_changes.log")
formatter = logging.Formatter(
    "%(asctime)s %(levelname)s user=%(user_id)s %(message)s"
)
handler.setFormatter(formatter)
logger.addHandler(handler)
logger.setLevel(logging.INFO)

# Load a blacklist of common passwords once at startup
COMMON_PASSWORDS = set()
with open("/etc/myapp/common_passwords.txt") as f:
    for line in f:
        COMMON_PASSWORDS.add(line.strip())

# Constants for PBKDF2
HASH_NAME = "sha256"
ITERATIONS = 200_000
SALT_SIZE = 16


def get_db_connection():
    return MySQLdb.connect(
        host=os.environ["DB_HOST"],
        user=os.environ["DB_USER"],
        passwd=os.environ["DB_PASS"],
        db=os.environ["DB_NAME"],
        cursorclass=DictCursor,
        autocommit=False,
        connect_timeout=5,
    )

class ChangePasswordForm(FlaskForm):
    current_password = PasswordField(
        "Current Password",
        validators=[DataRequired()]
    )
    new_password = PasswordField(
        "New Password",
        validators=[DataRequired(), Length(min=12, max=128)]
    )
    submit = SubmitField("Change Password")


def validate_password_strength(pw, old_pw_hash, old_salt, old_iterations):
    """
    Returns None if password is strong enough, otherwise an error message.
    """
    # 1) Check against a common‐password blacklist
    if pw.lower() in COMMON_PASSWORDS:
        return "That password is too common; please choose another."

    # 2) Require mixed character classes
    classes = [
        r"[a-z]",  # lowercase
        r"[A-Z]",  # uppercase
        r"[0-9]",  # digit
        r"[^\w\s]",  # symbol
    ]
    if sum(bool(re.search(cls, pw)) for cls in classes) < 3:
        return "Password must include upper & lower case letters, digits, and/or symbols."

    # 3) Disallow whitespace
    if re.search(r"\s", pw):
        return "Password may not contain whitespace."

    # 4) Prevent re‐use of the current password
    new_hash = hashlib.pbkdf2_hmac(
        HASH_NAME,
        pw.encode("utf-8"),
        old_salt,
        old_iterations
    )
    if hmac.compare_digest(new_hash, old_pw_hash):
        return "New password must be different from the current password."

    # 5) (Optional) Check edit distance or substring match against old pw
    #    Add any additional checks here.

    return None


def login_required(view):
    """
    Simple decorator: abort 401 if not logged in.
    Assumes session['user_id'] stores the authenticated user.
    """
    from functools import wraps
    @wraps(view)
    def wrapped(*args, **kwargs):
        if "user_id" not in session:
            abort(401)
        return view(*args, **kwargs)
    return wrapped


@app.route("/account/password", methods=["GET", "POST"])
@csrf.exempt  # If you use Flask-WTF forms, you don't need to exempt—just remove this.
@login_required
def change_password():
    form = ChangePasswordForm()
    if not form.validate_on_submit():
        # On GET, or if CSRF fails, or missing fields, just re‐render the form.
        return flask.render_template("change_password.html", form=form), 400

    user_id = session["user_id"]
    curr_pw = form.current_password.data
    new_pw = form.new_password.data

    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            # 1) Retrieve existing hash parameters
            cursor.execute("""
                SELECT password_hash, salt, iterations
                FROM users
                WHERE id = %s
                FOR UPDATE
            """, (user_id,))
            row = cursor.fetchone()
            if not row:
                # No such user, should not happen if session is valid
                abort(404)

            stored_hash = row["password_hash"]            # BLOB
            salt = row["salt"]                            # BLOB
            iterations = row["iterations"] or ITERATIONS  # INT

            # 2) Verify current password in constant time
            computed = hashlib.pbkdf2_hmac(
                HASH_NAME,
                curr_pw.encode("utf-8"),
                salt,
                iterations
            )
            if not hmac.compare_digest(computed, stored_hash):
                flash("Current password is incorrect.", "error")
                return redirect("/account/password")

            # 3) Validate new‐password strength & reuse
            error = validate_password_strength(
                new_pw, stored_hash, salt, iterations
            )
            if error:
                flash(error, "error")
                return redirect("/account/password")

            # 4) Generate new salt + hash
            new_salt = os.urandom(SALT_SIZE)
            new_hash = hashlib.pbkdf2_hmac(
                HASH_NAME,
                new_pw.encode("utf-8"),
                new_salt,
                ITERATIONS
            )

            # 5) Store new hash
            cursor.execute("""
                UPDATE users
                SET password_hash = %s,
                    salt          = %s,
                    iterations    = %s,
                    pw_changed_at = %s
                WHERE id = %s
            """, (
                new_hash,
                new_salt,
                ITERATIONS,
                datetime.utcnow(),
                user_id
            ))
        conn.commit()

        # 6) Audit‐style log
        logger.info(
            "password changed successfully",
            extra={"user_id": user_id}
        )

    except Exception:
        # Roll back on any error, and do not leak details
        conn.rollback()
        logger.exception("error changing password", extra={"user_id": user_id})
        flash("An internal error occurred; please try again later.", "error")
        return redirect("/account/password")

    finally:
        conn.close()

    # 7) Invalidate the old session and force re‐login
    session.clear()
    flash("Your password has been changed. Please log in again.", "success")
    return redirect("/login")