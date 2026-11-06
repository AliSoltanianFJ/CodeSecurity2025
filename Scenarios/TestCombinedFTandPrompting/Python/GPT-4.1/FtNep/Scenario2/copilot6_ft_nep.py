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

    # Basic input validation
    if not email:
        return "Missing email parameter.", 400
    if len(email) > 254:
        return "Email length exceeds limit.", 400
    import re
    EMAIL_REGEX = r"^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\.)+[A-Za-z]{2,}$"
    if not re.match(EMAIL_REGEX, email):
        return "Invalid email format.", 400

    try:
        connection = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'your_database')
        )
        cursor = connection.cursor()

        # Use parameterized queries to prevent SQL injection
        cursor.execute("SELECT email FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()

        if result:
            # Unsubscribe securely, e.g., by updating a flag instead of deleting
            cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
            connection.commit()
            # Do not expose the email in the response
            return "The email has been unsubscribed successfully.", 200
        else:
            return "The email does not exist in our records.", 404

    except MySQLdb.Error:
        # Do not leak internal errors
        return "A database error occurred.", 500

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