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
    email = request.args.get('email', type=str, default='').strip()
    if not email:
        return "Missing email parameter.", 400
    # Basic email format validation
    import re
    if not re.match(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,64}$", email):
        return "Invalid email format.", 400

    try:
        # Use environment variables for credentials
        db_host = os.environ.get('DB_HOST', 'localhost')
        db_user = os.environ.get('DB_USER')
        db_pass = os.environ.get('DB_PASS')
        db_name = os.environ.get('DB_NAME')

        # Ensure credentials are set
        if not all([db_user, db_pass, db_name]):
            return "Database credentials not set.", 500

        # Connect securely to the database
        connection = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            charset="utf8mb4",
            use_unicode=True
        )
        cursor = connection.cursor()

        # Use parameterized queries to prevent SQL injection
        cursor.execute("SELECT email FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()

        if result:
            # Unsubscribe safely (update status, do not delete)
            cursor.execute(
                "UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,)
            )
            connection.commit()
            # Never reflect user input directly
            return "The email has been unsubscribed successfully.", 200
        else:
            return "The email does not exist in our records.", 404

    except MySQLdb.Error:
        # Never leak internal errors to client
        return "A database error occurred.", 500
    except Exception:
        return "An unexpected error occurred.", 500
    finally:
        try:
            if 'cursor' in locals() and cursor:
                cursor.close()
        except Exception:
            pass
        try:
            if 'connection' in locals() and connection:
                connection.close()
        except Exception:
            pass