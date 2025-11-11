# ...existing code...
from flask import Flask, request, redirect, Response
import os
import subprocess
import sys
import MySQLdb
import re
import hmac
import hashlib
import logging

app = Flask(__name__)

# configure minimal logging (don't log secrets)
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# secret key used to sign unsubscribe links. Must be set in environment.
_SECRET_KEY = os.environ.get("UNSUBSCRIBE_SECRET")
if not _SECRET_KEY:
    logger.warning("UNSUBSCRIBE_SECRET not set; unsubscribe links will be rejected in production.")

# simple email regex (reasonable safeguard; strict validation can be done using email-validator package)
_EMAIL_RE = re.compile(r"^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$")

def _valid_email(email: str) -> bool:
    if not email or len(email) > 254:
        return False
    return bool(_EMAIL_RE.fullmatch(email))

def _valid_token(email: str, token: str) -> bool:
    if not _SECRET_KEY or not token:
        return False
    mac = hmac.new(_SECRET_KEY.encode('utf-8'), email.encode('utf-8'), hashlib.sha256).hexdigest()
    return hmac.compare_digest(mac, token)

def _get_db_connection():
    # Read credentials from environment variables — do not hardcode secrets
    db_host = os.environ.get("DB_HOST", "localhost")
    db_user = os.environ.get("DB_USER")
    db_pass = os.environ.get("DB_PASS")
    db_name = os.environ.get("DB_NAME")
    if not (db_user and db_pass and db_name):
        raise RuntimeError("Database credentials not configured")
    # use utf8mb4 and explicit charset to avoid encoding issues
    return MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8mb4', use_unicode=True)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # Expect both email and token in query string. Token prevents CSRF and abuse.
    email = request.args.get('email', '', type=str).strip()
    token = request.args.get('token', '', type=str).strip()

    # Validate inputs
    if not _valid_email(email) or not _valid_token(email, token):
        # Do not reveal whether email exists or which part failed.
        return Response("If the address was subscribed, it has been unsubscribed.", status=200, mimetype='text/plain')

    conn = None
    try:
        conn = _get_db_connection()
        cursor = conn.cursor()
        # Use parameterized queries to prevent SQL injection
        cursor.execute("SELECT id FROM subscribers WHERE email = %s LIMIT 1", (email,))
        row = cursor.fetchone()
        if row:
            # Perform the unsubscribe action using a parameterized update
            cursor.execute("UPDATE subscribers SET unsubscribed = 1 WHERE email = %s", (email,))
            conn.commit()
        # Always return the same generic response to avoid user enumeration
        return Response("If the address was subscribed, it has been unsubscribed.", status=200, mimetype='text/plain')
    except Exception as exc:
        # Log minimal info for debugging — do not include sensitive data
        logger.exception("Error processing unsubscribe request")
        # Return generic response
        return Response("If the address was subscribed, it has been unsubscribed.", status=200, mimetype='text/plain')
    finally:
        try:
            if conn:
                conn.close()
        except Exception:
            pass
# ...existing code...
