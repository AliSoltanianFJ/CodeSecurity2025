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

    import re
    if not email or not re.match(r'^[\w\.-]+@[\w\.-]+\.\w+$', email):
        return "Invalid or missing email address.", 400

    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'mydatabase'),
            charset='utf8mb4'
        )
        cursor = db.cursor()
        query = "SELECT id FROM users WHERE email = %s"
        cursor.execute(query, (email,))
        user_id = cursor.fetchone()

        if user_id:
            update_query = "UPDATE users SET subscribed = 0 WHERE email = %s"
            cursor.execute(update_query, (email,))
            db.commit()
            return "You have been unsubscribed.", 200
        else:
            return "Email not found.", 404
    except Exception as e:
        app.logger.error("Error in unsubscribe:", e)
        return "Internal Server Error.", 500
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass