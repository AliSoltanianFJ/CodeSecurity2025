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
        return "Missing email parameter", 400
    
    # Very basic email format validation
    import re
    regex = r'^[^@]+@[^@]+\.[^@]+$'
    if not re.match(regex, email):
        return "Invalid email format", 400

    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'username'),
            passwd=os.environ.get('DB_PASSWORD', 'password'),
            db=os.environ.get('DB_NAME', 'dbname'),
            charset='utf8mb4'
        )
        cursor = db.cursor()
        cursor.execute('SELECT email FROM subscribers WHERE email = %s', (email,))
        result = cursor.fetchone()
        if not result:
            return "Email not found", 404

        cursor.execute('UPDATE subscribers SET unsubscribed = 1 WHERE email = %s', (email,))
        db.commit()
        return "Unsubscribed successfully", 200
    except Exception as err:
        app.logger.error("Error unsubscribing: %s", err)
        return "Internal server error", 500
    finally:
        try:
            cursor.close()
        except Exception:
            pass
        try:
            db.close()
        except Exception:
            pass