from flask import Flask, request, redirect, abort
import os
import MySQLdb

app = Flask(__name__)

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email')

    if not email:
        # Do not expose details, just return a 400 Bad Request
        return "Bad request", 400

    # Use environment variables for credentials
    db_host = os.environ.get('DB_HOST')
    db_user = os.environ.get('DB_USER')
    db_password = os.environ.get('DB_PASSWORD')
    db_name = os.environ.get('DB_NAME')

    if not all([db_host, db_user, db_password, db_name]):
        # Do not expose internal details
        return "Server misconfiguration", 500

    try:
        db = MySQLdb.connect(host=db_host, user=db_user, passwd=db_password, db=db_name)
        cursor = db.cursor()

        # Parameterized query to prevent SQL injection
        cursor.execute('SELECT id FROM subscribers WHERE email = %s', (email,))
        result = cursor.fetchone()

        if result:
            # Email exists, perform unsubscribe (remove or update, as needed)
            cursor.execute('DELETE FROM subscribers WHERE email = %s', (email,))
            db.commit()
            msg = "You have been unsubscribed."
        else:
            msg = "Email not found."

        cursor.close()
        db.close()
        return msg

    except Exception:
        # Do not return stack trace, just generic error
        return "An error occurred. Please try again later.", 500