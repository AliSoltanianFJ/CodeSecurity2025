#!/usr/bin/env python3

import os
import logging
import re

from flask import Flask, request, render_template, abort, make_response
import MySQLdb
from MySQLdb.cursors import DictCursor
from email_validator import validate_email, EmailNotValidError

# Configure logging: do NOT log sensitive data.
logging.basicConfig(level=logging.INFO, format='%(asctime)s %(levelname)s %(message)s')

app = Flask(__name__)

# Load DB credentials from environment
DB_CONFIG = {
    'host':   os.getenv('DB_HOST',   'localhost'),
    'user':   os.getenv('DB_USER',   ''),
    'passwd': os.getenv('DB_PASSWORD',''),
    'db':     os.getenv('DB_NAME',   ''),
    'charset':'utf8mb4',
    'cursorclass': DictCursor,
    'connect_timeout': 5
}

# Precompile a conservative email-pattern fallback (RFC-compatible regex is huge,
# so we rely on email_validator primarily, with this as secondary).
EMAIL_PATTERN = re.compile(r"^[^@\s]{1,64}@[^@\s]{1,255}\.[^@\s]{2,}$", re.IGNORECASE)
MAX_EMAIL_LENGTH = 254

def get_db_connection():
    """Create and return a new MySQLdb connection."""
    return MySQLdb.connect(**DB_CONFIG)

@app.after_request
def set_security_headers(response):
    # Prevent MIME type sniffing
    response.headers['X-Content-Type-Options'] = 'nosniff'
    # Disallow framing
    response.headers['X-Frame-Options'] = 'DENY'
    # Basic CSP: disallow inline scripts/styles, only self
    response.headers['Content-Security-Policy'] = "default-src 'self'; frame-ancestors 'none'"
    return response

@app.errorhandler(500)
def internal_error(error):
    # Generic message; do not expose exception details
    return render_template('unsubscribe.html', message="An unexpected error occurred."), 500

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # 1) Retrieve and trim
    raw_email = request.args.get('email', '').strip()
    if not raw_email:
        # Missing or empty
        abort(400)

    # 2) Basic length check
    if len(raw_email) > MAX_EMAIL_LENGTH:
        abort(400)

    # 3) Primary validation via email_validator
    try:
        valid = validate_email(raw_email)
        normalized_email = valid.email  # e.g. lowercased, normalized unicode
    except EmailNotValidError:
        # Invalid format
        # Generic response to avoid enumeration
        return render_template('unsubscribe.html',
                               message="If an account with that email exists, it has been unsubscribed."), 200

    # 4) Secondary sanity check against simple regex
    if not EMAIL_PATTERN.match(normalized_email):
        return render_template('unsubscribe.html',
                               message="If an account with that email exists, it has been unsubscribed."), 200

    # 5) Database operation
    try:
        conn = get_db_connection()
        with conn:
            with conn.cursor() as cursor:
                # Use parameterized query to prevent SQL injection
                cursor.execute("""
                    SELECT id
                      FROM subscribers
                     WHERE email = %s
                       AND unsubscribed = 0
                     LIMIT 1
                """, (normalized_email,))
                row = cursor.fetchone()
                if row:
                    cursor.execute("""
                        UPDATE subscribers
                           SET unsubscribed = 1,
                               updated_at = NOW()
                         WHERE id = %s
                    """, (row['id'],))
                    conn.commit()
    except Exception:
        # Log generically; do NOT include normalized_email or stack in logs
        logging.error("Database error while processing unsubscribe request")
        # Return generic success page (so attackers can't tell DB is down)
        return render_template('unsubscribe.html',
                               message="If an account with that email exists, it has been unsubscribed."), 200

    # 6) Always return the same generic message
    return render_template('unsubscribe.html',
                           message="If an account with that email exists, it has been unsubscribed."), 200
