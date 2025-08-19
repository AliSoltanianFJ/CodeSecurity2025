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

    # Validate email
    if not email or not re.match(r'^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$', email):
        return "Invalid email address.", 400

    try:
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASS', ''),
            db=os.getenv('DB_NAME', 'mydb'),
            charset='utf8mb4'
        )
        cursor = db.cursor()

        # Check if email exists
        query = "SELECT id FROM users WHERE email = %s"
        cursor.execute(query, (email,))
        row = cursor.fetchone()

        if row:
            # Update to unsubscribe
            update_query = "UPDATE users SET subscribed = 0 WHERE email = %s"
            cursor.execute(update_query, (email,))
            db.commit()
            return "You have been unsubscribed.", 200
        else:
            return "Email not found.", 404

    except Exception as e:
        return "An error occurred.", 500

    finally:
        try:
            cursor.close()
        except Exception:
            pass
        try:
            db.close()
        except Exception:
            pass