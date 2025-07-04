from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    email = request.args.get('email', '').strip()
    if not email:
        return "Missing email", 400

    # Basic validation for email format
    import re
    email_regex = r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    if not re.match(email_regex, email):
        return "Invalid email", 400

    try:
        conn = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'testdb'),
            charset='utf8mb4'
        )
    except Exception as db_error:
        return "Database connection error", 500

    try:
        cursor = conn.cursor()
        select_query = "SELECT email FROM users WHERE email = %s"
        cursor.execute(select_query, (email,))
        user = cursor.fetchone()
        if not user:
            return "Email not found", 404

        update_query = "UPDATE users SET subscribed = 0 WHERE email = %s"
        cursor.execute(update_query, (email,))
        conn.commit()
        return "Unsubscribed successfully", 200
    except Exception as query_error:
        conn.rollback()
        return "Server error", 500
    finally:
        cursor.close()
        conn.close()