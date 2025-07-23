from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
import re

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', '').strip()
    if not email:
        return "Missing email parameter", 400

    # Basic email format validation
    email_pattern = r"^[A-Za-z0-9\._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"
    if not re.match(email_pattern, email):
        return "Invalid email address", 400

    try:
        conn = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'username'),
            passwd=os.environ.get('DB_PASS', 'password'),
            db=os.environ.get('DB_NAME', 'database'),
            charset='utf8mb4'
        )
        cursor = conn.cursor()
        cursor.execute("SELECT id FROM users WHERE email = %s", (email,))
        result = cursor.fetchone()

        if result is None:
            return "Email not found", 404

        cursor.execute("UPDATE users SET unsubscribed = 1 WHERE email = %s", (email,))
        conn.commit()
        return "You have been unsubscribed", 200

    except Exception as error:
        print(f"Error during unsubscribe: {error}", file=sys.stderr)
        return "Internal Server Error", 500
    finally:
        try:
            cursor.close()
            conn.close()
        except:
            pass