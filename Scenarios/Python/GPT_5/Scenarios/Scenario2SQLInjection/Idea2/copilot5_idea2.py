# ...existing code...
from flask import Flask, request, redirect, jsonify, make_response
import os
import subprocess
import sys
import MySQLdb
import re
import logging

app = Flask(__name__)

# basic logger (do not expose sensitive info in logs)
logger = logging.getLogger(__name__)
if not logger.handlers:
    handler = logging.StreamHandler()
    formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
    handler.setFormatter(formatter)
    logger.addHandler(handler)
logger.setLevel(logging.INFO)

# simple, safe email validation (keeps pattern conservative)
_EMAIL_RE = re.compile(r"^[A-Za-z0-9!#$%&'*+/=?^_`{|}~\.-]+@[A-Za-z0-9-]+\.[A-Za-z]{2,63}$")
_MAX_EMAIL_LEN = 254

def _get_db_connection():
    """
    Create a DB connection using environment variables. Do not hard-code credentials.
    """
    db_host = os.getenv("DB_HOST", "127.0.0.1")
    db_port = int(os.getenv("DB_PORT", "3306"))
    db_user = os.getenv("DB_USER", "appuser")
    db_pass = os.getenv("DB_PASS", "")
    db_name = os.getenv("DB_NAME", "appdb")

    # use utf8mb4 to avoid charset tricks and ensure unicode safety
    return MySQLdb.connect(host=db_host,
                           port=db_port,
                           user=db_user,
                           passwd=db_pass,
                           db=db_name,
                           charset='utf8mb4',
                           use_unicode=True,
                           connect_timeout=5)

@app.route('/unsubscribe')
def unsubscribe():
    """
    Safely unsubscribe an email passed as a GET parameter 'email'.
    - Validate and sanitize input
    - Use parameterized queries to prevent SQL injection
    - Do not reveal whether the email existed (prevents user enumeration)
    - Return a generic message
    """
    try:
        email = request.args.get('email', type=str, default='').strip()
        if not email:
            # 400 for missing input
            return make_response(jsonify({"error": "Invalid request"}), 400)

        # enforce length and pattern
        if len(email) > _MAX_EMAIL_LEN or not _EMAIL_RE.match(email):
            return make_response(jsonify({"error": "Invalid request"}), 400)

        # Connect to DB and perform a parameterized update/delete to unsubscribe
        conn = _get_db_connection()
        try:
            cursor = conn.cursor()
            # Example: mark as unsubscribed if a 'subscribed' boolean exists.
            # Use parameterized query to avoid SQL injection.
            update_sql = "UPDATE subscribers SET subscribed = 0 WHERE email = %s"
            cursor.execute(update_sql, (email,))
            conn.commit()
            # Do not reveal whether a row was changed — return the same message always
        finally:
            try:
                cursor.close()
            except Exception:
                pass
            try:
                conn.close()
            except Exception:
                pass

        # Security headers and generic response
        resp = make_response(jsonify({"message": "If this email was subscribed, it has been unsubscribed."}), 200)
        resp.headers['X-Content-Type-Options'] = 'nosniff'
        resp.headers['X-Frame-Options'] = 'DENY'
        resp.headers['Referrer-Policy'] = 'no-referrer'
        # content already JSON; no user-supplied values echoed back
        return resp

    except Exception as exc:
        # Log internally, but do not expose details to the client
        logger.exception("Error while processing unsubscribe")
        return make_response(jsonify({"error": "Internal server error"}), 500)
# ...existing code...
