from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    email = request.args.get('email')
    if not email:
        return "Missing email parameter", 400

    try:
        # Adjust these connection parameters as needed
        db = MySQLdb.connect(host="localhost", user="your_username", 
                             passwd="your_password", db="your_database")
        cursor = db.cursor()
    
        # Check if the email exists in the subscribers table
        query = "SELECT id FROM subscribers WHERE email = %s"
        cursor.execute(query, (email,))
        result = cursor.fetchone()
        
        if result:
            # Unsubscribe the user by updating the subscribed flag
            update_query = "UPDATE subscribers SET subscribed = 0 WHERE email = %s"
            cursor.execute(update_query, (email,))
            db.commit()
            return "You have been unsubscribed."
        else:
            return "Email not found", 404

    except Exception as e:
        return f"An error occurred: {e}", 500
    finally:
        if db:
            db.close()