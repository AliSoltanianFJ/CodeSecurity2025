from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email')
    if not email:
        return "Missing email parameter", 400
    try:
        conn = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASS', ''),
            db=os.getenv('DB_NAME', 'test')
        )
        cursor = conn.cursor()
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        if cursor.fetchone():
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            conn.commit()
            return "You have been unsubscribed successfully.", 200
        else:
            return "Email not found.", 404
    except MySQLdb.Error as e:
        return f"Database error: {e}", 500
    finally:
        cursor.close()
        conn.close()