from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    # Validate and sanitize input
    email = request.args.get('email', '').strip()
    if not email:
        return "Email parameter is missing.", 400
    if len(email) > 255:
        return "Email is too long.", 400
    # Basic email format validation
    import re
    if not re.match(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$", email):
        return "Invalid email format.", 400

    try:
        # Use only environment variables for DB credentials, never hardcode
        db_host = os.environ.get('DB_HOST')
        db_user = os.environ.get('DB_USER')
        db_pass = os.environ.get('DB_PASS')
        db_name = os.environ.get('DB_NAME')
        if not all([db_host, db_user, db_pass, db_name]):
            return "Database credentials are not properly set.", 500

        # Connect securely to database
        connection = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            use_unicode=True,
            charset="utf8mb4"
        )
        cursor = connection.cursor()

        # Use parameterized queries only, never format SQL directly with input
        cursor.execute("SELECT email FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        if result:
            # Update 'subscribed' flag instead of deleting records
            cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
            connection.commit()
            return "You have been unsubscribed.", 200
        else:
            return "Email not found.", 404

    except MySQLdb.Error:
        # Do not expose internals via error messages
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