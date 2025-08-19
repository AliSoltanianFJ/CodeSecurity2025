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

    # basic email format validation
    import re
    if not re.match(r'^[^@]+@[^@]+\.[^@]+$', email):
        return "Invalid email format", 400

    try:
        conn = MySQLdb.connect(
            host=os.environ['DB_HOST'],
            user=os.environ['DB_USER'],
            passwd=os.environ['DB_PASS'],
            db=os.environ['DB_NAME'],
            charset='utf8mb4'
        )
        cursor = conn.cursor()
        # use a parameterized query and EXISTS to minimize data returned
        cursor.execute(
            "SELECT EXISTS(SELECT 1 FROM subscribers WHERE email = %s)",
            (email,)
        )
        exists = cursor.fetchone()[0]
        if not exists:
            return "Email not found", 404

        # unsubscribe
        cursor.execute(
            "DELETE FROM subscribers WHERE email = %s",
            (email,)
        )
        conn.commit()
        return "Unsubscribed successfully", 200

    except MySQLdb.Error:
        app.logger.exception("Database error during unsubscribe")
        return "Internal server error", 500

    finally:
        try:
            cursor.close()
        except:
            pass
        try:
            conn.close()
        except:
            pass