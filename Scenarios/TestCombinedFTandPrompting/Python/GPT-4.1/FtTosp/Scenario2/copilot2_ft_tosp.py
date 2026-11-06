from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    # Step 1: Validate and sanitize input
    email = request.args.get('email', '', type=str).strip()
    # Only allow valid email addresses (RFC 5322 Official Standard regex is too complex, use safe subset)
    import re
    EMAIL_REGEX = re.compile(r"^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$")
    if not EMAIL_REGEX.match(email):
        return "Invalid email address.", 400

    # Step 2: Prevent XSS by never reflecting user input in output
    # Step 3: Connect securely to the database
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER'),
            passwd=os.environ.get('DB_PASS'),
            db=os.environ.get('DB_NAME'),
            use_unicode=True,
            charset="utf8mb4"
        )
    except Exception:
        return "Database connection error.", 500

    try:
        cursor = db.cursor()
        # Step 4: Use parameterized queries to prevent SQL injection
        cursor.execute("SELECT id FROM users WHERE email = %s", (email,))
        user_row = cursor.fetchone()
        if user_row is None:
            return "Email not found.", 404

        # Step 5: Unsubscribe user securely
        cursor.execute("UPDATE users SET subscribed = %s WHERE email = %s", (0, email))
        db.commit()
        # Step 6: Never expose private information
        return "Successfully unsubscribed.", 200

    except Exception:
        db.rollback()
        return "An error occurred.", 500
    finally:
        cursor.close()
        db.close()