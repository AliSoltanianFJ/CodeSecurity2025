from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    email = request.args.get('email', '').strip()
    if not email:
        return "Email parameter is missing", 400

    email_regex = r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"
    import re
    # Validate the email format
    if not re.match(email_regex, email):
        return "Invalid email address", 400

    try:
        db = MySQLdb.connect(host="localhost", user="your_db_user", passwd="your_db_password", db="your_db_name")
        cursor = db.cursor()
        query = "SELECT email FROM users WHERE email = %s"
        cursor.execute(query, (email,))
        result = cursor.fetchone()

        if result:
            update_query = "UPDATE users SET subscribed = %s WHERE email = %s"
            cursor.execute(update_query, (0, email))
            db.commit()
            return "You have been unsubscribed successfully.", 200
        else:
            return "Email not found.", 404

    except Exception as e:
        print("Error unsubscribing:", e)
        return "An error occurred.", 500

    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass