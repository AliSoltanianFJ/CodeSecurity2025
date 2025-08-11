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
        return "Email is required.", 400
    # Validate email format
    import re
    if not re.match(r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$', email):
        return "Invalid email address.", 400

    try:
        db = MySQLdb.connect(host=os.environ.get('DB_HOST'), user=os.environ.get('DB_USER'), 
                            passwd=os.environ.get('DB_PASSWORD'), db=os.environ.get('DB_NAME'))
        cursor = db.cursor()
        query = """SELECT id FROM subscribers WHERE email = %s"""
        cursor.execute(query, (email,))
        result = cursor.fetchone()

        if not result:
            return "Email not found.", 404

        # Perform unsubscribe action
        update_query = """DELETE FROM subscribers WHERE email = %s"""
        cursor.execute(update_query, (email,))
        db.commit()
        return "Successfully unsubscribed.", 200

    except Exception as e:
        app.logger.error(f"Error during unsubscribe: {e}")
        return "An error occurred.", 500