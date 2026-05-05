import re
import os
import time
import hmac
import hashlib
import binascii
from datetime import datetime, timedelta

from flask import Flask, request, abort, render_template, current_app
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address
import MySQLdb
import MySQLdb.cursors

# ----------------------------------------------------------------------------
#  Application setup
# ----------------------------------------------------------------------------
app = Flask(__name__)
app.config.from_pyfile("config.py")
limiter = Limiter(app, key_func=get_remote_address)

# Email validation regex (simple but effective)
EMAIL_REGEX = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")

# ----------------------------------------------------------------------------
#  Database helper
# ----------------------------------------------------------------------------
def get_db():
    return MySQLdb.connect(
        host   = app.config["DB_HOST"],
        user   = app.config["DB_USER"],
        passwd = app.config["DB_PASSWORD"],
        db     = app.config["DB_NAME"],
        charset = "utf8mb4",
        cursorclass = MySQLdb.cursors.DictCursor
    )

# ----------------------------------------------------------------------------
#  Token generation & validation
# ----------------------------------------------------------------------------
def make_token(email):
    # HMAC-sign the email + timestamp so we can detect tampering
    ts = int(time.time())
    msg = f"{email}|{ts}".encode("utf-8")
    sig = hmac.new(app.config["SECRET_KEY"].encode("utf-8"),
                   msg,
                   digestmod=hashlib.sha256).digest()
    token = binascii.hexlify(msg + b"|" + sig).decode("ascii")
    return token

def parse_and_validate_token(token):
    try:
        raw = binascii.unhexlify(token.encode("ascii"))
        parts = raw.split(b"|")
        if len(parts) != 3:
            return None
        email = parts[0].decode("utf-8")
        ts    = int(parts[1].decode("utf-8"))
        sig   = parts[2]
    except (binascii.Error, ValueError, UnicodeDecodeError):
        return None

    # Check expiration
    if datetime.utcnow() > datetime.utcfromtimestamp(ts) + timedelta(hours=app.config["TOKEN_EXPIRY_HOURS"]):
        return None

    # Recompute HMAC
    msg = b"|".join(parts[0:2])
    expected_sig = hmac.new(app.config["SECRET_KEY"].encode("utf-8"),
                            msg,
                            digestmod=hashlib.sha256).digest()
    if not hmac.compare_digest(sig, expected_sig):
        return None

    # Final email check
    email = email.strip().lower()
    if not EMAIL_REGEX.fullmatch(email):
        return None

    return email

# ----------------------------------------------------------------------------
#  Step 1: Send the user their one-time unsubscribe link by email
# ----------------------------------------------------------------------------
# This is *not* the route the user clicks—this is the backend step when you
# receive a request from your app asking “please email an unsubscribe link
# to bob@example.com.” You must require authentication (omitted here).
@app.route("/_internal/send_unsub_link", methods=["POST"])
def send_unsub_link():
    email = request.form.get("email", "").strip().lower()
    if not EMAIL_REGEX.fullmatch(email):
        abort(400, "Invalid email")
    token = make_token(email)
    link  = f"{request.url_root.rstrip('/')}/unsubscribe?token={token}"
    # send_email(to=email, subject="Unsubscribe", body=f"Click here to unsubscribe:\n{link}")
    current_app.logger.info("Unsubscribe link for %s: %s", email, link)
    return ("OK", 200)

# ----------------------------------------------------------------------------
#  Step 2: The user clicks the link in their inbox
# ----------------------------------------------------------------------------
@app.route("/unsubscribe", methods=["GET"])
@limiter.limit(app.config["RATELIMIT_DEFAULT"])
def unsubscribe():
    token = request.args.get("token", "").strip()
    if not token:
        abort(400, "Missing token")

    email = parse_and_validate_token(token)
    if not email:
        current_app.logger.warning("Invalid or expired token attempt: %s", token)
        abort(404)   # Don’t reveal that the token was bad vs. expired

    try:
        db = get_db()
        with db.cursor() as cur:
            # Parameterized query avoids SQL injection
            cur.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
            db.commit()
    except Exception as e:
        current_app.logger.error("DB error unsubscribing %s: %s", email, e)
        abort(500, "Server error")

    # Render a local template—no user-supplied redirect URLs!
    return render_template("unsubscribe_success.html", email=email)

# ----------------------------------------------------------------------------
#  Error handlers (don’t leak stack traces)
# ----------------------------------------------------------------------------
@app.errorhandler(400)
@app.errorhandler(403)
@app.errorhandler(404)
@app.errorhandler(429)
@app.errorhandler(500)
def handle_errors(err):
    # You can customize per-status templates if you like
    return render_template(f"errors/{err.code}.html", message=err.description), err.code

# ----------------------------------------------------------------------------
#  Run
# ----------------------------------------------------------------------------
if __name__ == "__main__":
    # In production, sit behind gunicorn/uwsgi, don’t use debug=True
    app.run(host="0.0.0.0", port=5000, debug=False)