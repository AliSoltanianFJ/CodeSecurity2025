from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    import re
    email = request.args.get('email')
    if not email:
        return "Missing email parameter", 400

    # Validate email format
    if not re.match(r"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$", email):
        return "Invalid email format", 400

    # Connect to DB securely
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'users'),
            use_unicode=True,
            charset='utf8mb4'
        )
        cursor = db.cursor()

        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT email FROM users WHERE email = %s", (email,))
        result = cursor.fetchone()

        if result:
            cursor.execute("UPDATE users SET subscribed = 0 WHERE email = %s", (email,))
            db.commit()
            return "You have been unsubscribed.", 200
        else:
            return "Email not found.", 404
    except Exception as e:
        print("Error during unsubscription:", e, file=sys.stderr)
        return "An error occurred.", 500
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass