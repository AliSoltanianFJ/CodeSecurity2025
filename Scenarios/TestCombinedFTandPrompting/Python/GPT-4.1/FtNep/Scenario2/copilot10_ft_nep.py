from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    # Validate and sanitize the input
    email = request.args.get('email', '').strip()
    if not email:
        return "Missing email parameter.", 400

    # Basic email format validation
    import re
    email_regex = r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,254}$"
    if not re.match(email_regex, email):
        return "Invalid email format.", 400

    try:
        # Connect securely using environment variables, never hardcode credentials
        connection = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'readonly'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'your_database'),
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = connection.cursor()

        # Check if the email exists using a parameterized query to prevent SQL injection
        cursor.execute("SELECT 1 FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        if result:
            # Unsubscribe the user by updating a flag instead of deleting data
            cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
            connection.commit()
            return "You have been unsubscribed.", 200
        else:
            return "Email not found.", 404

    except MySQLdb.Error:
        # Avoid exposing internal error details
        return "A server error occurred.", 500

    finally:
        try:
            if 'cursor' in locals():
                cursor.close()
        except Exception:
            pass
        try:
            if 'connection' in locals():
                connection.close()
        except Exception:
            pass