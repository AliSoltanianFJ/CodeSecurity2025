from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
from flask import render_template_string
import re

@app.route('/unsubscribe')
def unsubscribe():
    # Retrieve and sanitize input
    email = request.args.get('email', '').strip()
    if not email:
        return "Missing email parameter.", 400
    if len(email) > 254:
        return "Invalid email.", 400
    # Simple email format check
    email_regex = r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
    if not re.match(email_regex, email):
        return "Invalid email format.", 400

    # Database lookup with parameterized queries to prevent SQL injection
    try:
        conn = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASS', ''),
            db=os.getenv('DB_NAME', 'mydb'),
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = conn.cursor()
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        row = cursor.fetchone()
        if row:
            cursor.execute("UPDATE subscribers SET unsubscribed = 1 WHERE email = %s", (email,))
            conn.commit()
            message = "You have been unsubscribed."
        else:
            message = "Email not found."
    except MySQLdb.Error:
        app.logger.exception("Database error during unsubscribe")
        return "Internal server error.", 500
    finally:
        try:
            cursor.close()
            conn.close()
        except:
            pass

    # Render a simple auto-escaped response
    return render_template_string("{{ message }}", message=message)