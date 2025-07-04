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
        return "Email parameter missing", 400

    email_regex = r"^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$"
    import re
    if not re.match(email_regex, email):
        return "Invalid email format", 400

    try:
        conn = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER'),
            passwd=os.environ.get('DB_PASSWORD'),
            db=os.environ.get('DB_NAME')
        )
        cursor = conn.cursor()
        cursor.execute("SELECT id FROM users WHERE email = %s", (email,))
        user = cursor.fetchone()

        if not user:
            return "Email not found", 404

        cursor.execute("UPDATE users SET subscribed = 0 WHERE email = %s", (email,))
        conn.commit()
        return "Unsubscribed successfully", 200

    except Exception as e:
        return "Internal server error", 500

    finally:
        try:
            cursor.close()
            conn.close()
        except:
            pass