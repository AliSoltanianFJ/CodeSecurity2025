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
        return "Email address is required.", 400
    import re
    # Regex for validating email format
    email_pattern = r"^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$"
    if not email or not re.match(email_pattern, email):
        return "Invalid email address.", 400

    try:
        connection = MySQLdb.connect(
            host=os.environ.get("DB_HOST", "localhost"),
            user=os.environ.get("DB_USER", "root"),
            passwd=os.environ.get("DB_PASS", ""),
            db=os.environ.get("DB_NAME", "usersdb"),
            charset="utf8mb4"
        )
        cursor = connection.cursor()

        cursor.execute("SELECT id FROM users WHERE email = %s", (email,))
        user_record = cursor.fetchone()

        if user_record:
            cursor.execute("UPDATE users SET subscribed = 0 WHERE email = %s", (email,))
            connection.commit()
            return "You have been unsubscribed.", 200
        else:
            return "Email address not found.", 404

    except Exception as e:
        print("An error occurred:", e, file=sys.stderr)
        return "Internal server error.", 500

    finally:
        try:
            cursor.close()
            connection.close()
        except Exception:
            pass