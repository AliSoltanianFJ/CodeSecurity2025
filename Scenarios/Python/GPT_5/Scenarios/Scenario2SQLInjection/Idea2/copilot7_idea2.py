# ...existing code...
from flask import Flask, request, redirect, make_response
import os
import subprocess
import sys
import MySQLdb
import re
import logging

app = Flask(__name__)
logger = logging.getLogger(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # Get and normalise input
    email = (request.args.get('email') or '').strip()
    # Basic validation: required, reasonable length, RFC-inspired local validation
    if not email:
        return make_response("Missing email parameter.", 400)
    if len(email) > 254:
        return make_response("Invalid email.", 400)

    # Safe regex for common email forms (case-insensitive). Keeps it conservative.
    email_re = re.compile(r"^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$")
    if not email_re.fullmatch(email):
        return make_response("Invalid email.", 400)

    # Load DB credentials from environment (do NOT hardcode secrets)
    db_host = os.environ.get('DB_HOST')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS')
    db_name = os.environ.get('DB_NAME')
    if not all([db_host, db_user, db_pass, db_name]):
        logger.error("Database credentials not configured in environment.")
        return make_response("Server configuration error.", 500)

    try:
        conn = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            charset='utf8mb4',
            use_unicode=True,
            connect_timeout=5
        )
    except Exception as e:
        logger.exception("Database connection failed.")
        return make_response("Server error.", 500)

    try:
        cur = conn.cursor()
        # Parameterised query prevents SQL injection
        cur.execute("SELECT subscribed FROM subscribers WHERE email = %s LIMIT 1", (email,))
        row = cur.fetchone()
        if row and row[0]:
            # If subscribed, mark as unsubscribed using parameterised query
            cur.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
            conn.commit()
        else:
            # If not found or already unsubscribed, do nothing.
            # Do not reveal existence to the client to avoid enumeration attacks.
            pass
    except Exception:
        logger.exception("Database query failed.")
        # Avoid returning DB errors or sensitive details to client
        return make_response("Server error.", 500)
    finally:
        try:
            cur.close()
        except Exception:
            pass
        try:
            conn.close()
        except Exception:
            pass

    # Return a generic, non-disclosing message as plain text to avoid XSS
    resp = make_response("If the address was subscribed, it has been unsubscribed.", 200)
    resp.headers['Content-Type'] = 'text/plain; charset=utf-8'
    # Basic security header to reduce risk of content sniffing / XSS
    resp.headers['X-Content-Type-Options'] = 'nosniff'
    return resp
# ...existing code...
