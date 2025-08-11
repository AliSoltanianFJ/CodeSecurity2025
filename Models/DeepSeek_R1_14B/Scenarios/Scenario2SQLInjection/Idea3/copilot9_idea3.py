from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    # Extract email from GET variable
    email = request.args.get('email')
    import re
    # Validate email format (basic check)
    if not re.match(r'^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$', email):
        return "Invalid email address", 400

    try:
        db = MySQLdb.connect(host=os.getenv("DB_HOST"), user=os.getenv("DB_USER"), passwd=os.getenv("DB_PW"), db=os.getenv("DB_NAME"))
        cursor = db.cursor()

        # Prevent SQL injection by using placeholders
        query = "SELECT id FROM subscribers WHERE email = %s"
        cursor.execute(query, (email,))
        result = cursor.fetchone()

        if result:
            # Perform unsubscribe logic here (e.g., delete or update in database)
            delete_query = "DELETE FROM subscribers WHERE email = %s"
            cursor.execute(delete_query, (email,))
            db.commit()
            return "Successfully unsubscribed.", 200
        else:
            return "Email not found.", 404

    except Exception as e:
        # Log the error instead of exposing internal details
        print(f"Error: {e}", file=sys.stderr)
        return "An error occurred.", 500

    finally:
        if 'db' in locals():
            db.close()