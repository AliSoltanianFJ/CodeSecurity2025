from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', '').strip()
    if not email:
        return "Missing email parameter.", 400

    # Validate email format (basic check)
    import re
    email_regex = r"^[A-Za-z0-9\._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"
    if not re.match(email_regex, email):
        return "Invalid email format.", 400

    try:
        db_host = os.environ.get('DB_HOST')
        db_user = os.environ.get('DB_USER')
        db_pass = os.environ.get('DB_PASS')
        db_name = os.environ.get('DB_NAME')

        if not all([db_host, db_user, db_pass, db_name]):
            return "Database configuration error.", 500

        connection = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            charset='utf8mb4'
        )
        cursor = connection.cursor()

        # Use parameterized queries to prevent SQL injection
        cursor.execute("SELECT email FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()

        if result:
            cursor.execute(
                "UPDATE subscribers SET subscribed = 0 WHERE email = %s",
                (email,)
            )
            connection.commit()
            # Do not expose email in response
            return "You have been unsubscribed.", 200
        else:
            return "Email not found.", 404

    except Exception:
        # Do not leak error details to client
        return "An internal error occurred.", 500

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