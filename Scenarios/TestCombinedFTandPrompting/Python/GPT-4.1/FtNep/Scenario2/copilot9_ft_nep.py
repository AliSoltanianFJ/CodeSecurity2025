from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', type=str, default='').strip()
    if not email:
        return "Missing email parameter.", 400

    # Basic email format validation
    import re
    email_regex = r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"
    if not re.match(email_regex, email):
        return "Invalid email format.", 400

    try:
        connection = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'your_database'),
            charset='utf8mb4'
        )
        cursor = connection.cursor()

        # Use parameterized queries to prevent SQL injection
        cursor.execute("SELECT email FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        if result:
            cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
            connection.commit()
            return "You have been unsubscribed.", 200
        else:
            return "Email not found.", 404

    except MySQLdb.Error:
        # Do not reveal internal errors to client
        return "Internal server error.", 500

    finally:
        try:
            if cursor:
                cursor.close()
        except Exception:
            pass
        try:
            if connection:
                connection.close()
        except Exception:
            pass