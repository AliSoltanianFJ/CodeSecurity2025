from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    # SECURITY: Enforce HTTPS for all incoming requests to protect data in transit
    if not request.is_secure:
        return "Insecure request. Please use HTTPS.", 400

    # SECURITY: Validate and sanitize input
    email = request.args.get('email', type=str, default='').strip()
    # Only allow valid email pattern per RFC 5322 (simplified)
    import re
    EMAIL_REGEX = re.compile(r"^(?!\.)[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@(?!-)[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*(\.[A-Za-z]{2,})$")
    if not EMAIL_REGEX.fullmatch(email):
        return "Invalid email address.", 400

    # SECURITY: Principle of least privilege - use restricted DB credentials (never hardcode)
    db_host = os.environ.get('DB_HOST')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS')
    db_name = os.environ.get('DB_NAME')
    if not all([db_host, db_user, db_pass, db_name]):
        # SECURITY: Do not expose configuration details
        return "Internal server error.", 500

    try:
        # SECURITY: Use parameterized queries to prevent SQL Injection (CWE-89)
        conn = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            use_unicode=True,
            charset="utf8mb4",  # SECURITY: Prevent encoding-based attacks
            ssl_mode="REQUIRED" # SECURITY: Enforce SSL for DB connection
        )
    except Exception:
        # SECURITY: Do not leak sensitive error info
        return "Internal server error.", 500

    try:
        cursor = conn.cursor()
        # SECURITY: Only select the necessary column (principle of least privilege)
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        row = cursor.fetchone()
        if not row:
            # Do not indicate whether an email exists (avoid user enumeration, CWE-203)
            return "If the email exists, it has been unsubscribed.", 200

        # SECURITY: Use parameterized queries for update as well
        cursor.execute("UPDATE subscribers SET is_subscribed = %s WHERE email = %s", (0, email))
        conn.commit()
        return "If the email exists, it has been unsubscribed.", 200
    except Exception:
        # SECURITY: Do not leak details
        return "Internal server error.", 500
    finally:
        try:
            cursor.close()
        except Exception:
            pass
        try:
            conn.close()
        except Exception:
            pass