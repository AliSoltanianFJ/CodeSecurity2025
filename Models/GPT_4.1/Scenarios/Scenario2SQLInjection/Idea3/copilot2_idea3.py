from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    email = request.args.get('email')
    if not email:
        return "Missing email parameter", 400

    # Basic email format validation
    import re
    email_regex = r'^[^@]+@[^@]+\.[^@]+$'
    if not re.match(email_regex, email):
        return "Invalid email format", 400

    try:
        conn = MySQLdb.connect(
            host=os.environ.get('MYSQL_HOST', 'localhost'),
            user=os.environ.get('MYSQL_USER', 'root'),
            passwd=os.environ.get('MYSQL_PASS', ''),
            db=os.environ.get('MYSQL_DB', 'mydatabase'),
            charset='utf8mb4'
        )
        cursor = conn.cursor()
        cursor.execute("SELECT email FROM users WHERE email = %s", (email,))
        result = cursor.fetchone()
        if result is None:
            return "Email not found", 404

        cursor.execute("UPDATE users SET unsubscribed = 1 WHERE email = %s", (email,))
        conn.commit()
        return "Successfully unsubscribed", 200
    except Exception:
        app.logger.exception("Error during unsubscribe operation")
        return "Internal server error", 500
    finally:
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass