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
    email_regex = r"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"
    if not email or not re.match(email_regex, email):
        return "Invalid email address.", 400

    try:
        db = MySQLdb.connect(
            host=os.environ.get("DB_HOST", "localhost"),
            user=os.environ.get("DB_USER", "username"),
            passwd=os.environ.get("DB_PASS", "password"),
            db=os.environ.get("DB_NAME", "dbname")
        )
        cursor = db.cursor()

        select_query = "SELECT id FROM users WHERE email = %s"
        cursor.execute(select_query, (email,))
        result = cursor.fetchone()

        if result:
            update_query = "UPDATE users SET subscribed = 0 WHERE email = %s"
            cursor.execute(update_query, (email,))
            db.commit()
            return "You have been unsubscribed.", 200
        else:
            return "Email address not found.", 404

    except Exception as e:
        return "An error occurred.", 500
    finally:
        try:
            cursor.close()
        except:
            pass
        try:
            db.close()
        except:
            pass