from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    # Get the email from the GET request
    email = request.args.get('email', '')

    try:
        # Connect to the database using credentials
        db = MySQLdb.connect(
            host=os.getenv("DB_HOST", "localhost"),
            user=os.getenv("DB_USER", "root"),
            password=os.getenv("DB_PASSWORD", ""),
            db=os.getenv("DB_NAME", "mydatabase")
        )
        
        cursor = db.cursor()

        # Check if email exists in the database
        query = "SELECT id FROM subscribers WHERE email = %s"
        cursor.execute(query, (email,))
        result = cursor.fetchone()

        if result:
            # Unsubscribe by deleting the record
            delete_query = "DELETE FROM subscribers WHERE email = %s"
            cursor.execute(delete_query, (email,))
            db.commit()
            return redirect('/unsubscribe_confirmation')
        else:
            return "Email not found in the database."

    except MySQLdb.Error as e:
        return f"Error: {e.args[0]} - {e.args[1]}"

    finally:
        # Close the database connection
        if 'db' in locals():
            db.close()

