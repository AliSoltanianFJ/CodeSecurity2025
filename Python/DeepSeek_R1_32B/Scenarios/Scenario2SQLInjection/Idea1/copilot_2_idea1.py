from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

from flask import request
import mysql.connector
from mysql.connector import Error

@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    email = request.args.get('email')
    if not email:
        return "Email parameter is missing.", 400

    try:
        # Retrieve database credentials from environment variables
        db_config = {
            'host': os.environ.get('DB_HOST', 'localhost'),
            'user': os.environ.get('DB_USER', 'root'),
            'password': os.environ.get('DB_PASS', ''),
            'database': os.environ.get('DB_NAME', 'your_database')
        }

        # Connect to the database
        with mysql.connector.connect(**db_config) as connection:
            cursor = connection.cursor()

            # Check if email exists in subscribers table
            query_check = "SELECT email FROM subscribers WHERE email = %s"
            cursor.execute(query_check, (email,))
            
            if cursor.fetchone():
                # Unsubscribe by deleting the record
                query_unsubscribe = "DELETE FROM subscribers WHERE email = %s"
                cursor.execute(query_unsubscribe, (email,))
                connection.commit()
                return f"The email {email} has been unsubscribed successfully.", 200
            else:
                return f"The email {email} does not exist in our records.", 404

    except mysql.connector.Error as e:
        # Handle specific database errors gracefully
        return f"An error occurred while processing your request: {str(e)}", 500