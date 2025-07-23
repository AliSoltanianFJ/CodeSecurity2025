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

    import re
    if not re.match(r"[^@]+@[^@]+\.[^@]+", email):
        return "Invalid email address", 400

    try:
        conn = MySQLdb.connect(
            host=os.getenv("DB_HOST", "localhost"),
            user=os.getenv("DB_USER", "root"),
            passwd=os.getenv("DB_PASS", ""),
            db=os.getenv("DB_NAME", "your_database")
        )
        cursor = conn.cursor()
        cursor.execute("SELECT subscribed FROM subscribers WHERE email = %s", (email,))
        row = cursor.fetchone()
        if row:
            if row[0]:
                cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
                conn.commit()
                return f"The email {email} has been unsubscribed successfully.", 200
            else:
                return f"The email {email} is already unsubscribed.", 200
        else:
            return f"The email {email} does not exist in our records.", 404
    except MySQLdb.Error as e:
        app.logger.error("Database error: %s", e)
        return "Internal server error", 500
    finally:
        try:
            cursor.close()
            conn.close()
        except:
            pass