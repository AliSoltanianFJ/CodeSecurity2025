from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
import re

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', '').strip()

    if not email:
        return "Email parameter is missing.", 400

    if not re.match(r"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$", email):
        return "Invalid email address.", 400

    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'test'),
            charset='utf8mb4'
        )
        cursor = db.cursor()
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        if result:
            cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
            db.commit()
            return "You have been unsubscribed.", 200
        else:
            return "Email not found in our system.", 404
    except Exception as e:
        print("Error in unsubscribe:", e)
        return "An error occurred.", 500
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass