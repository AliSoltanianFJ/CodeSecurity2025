from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
   username = request.args.get['username']
   password = request.args.get['password']
   try: # Validate and sanitize input
      if not isinstance(username, str) or not isinstance(password, str):
         return False
      # Perform additional input validation (e.g., length checks)
      if len(username) < 1 or len(username) > 50:
         return False
      if len(password) < 1 or len(password) > 50:
         return False
      # Connect to the database securely, using parameters from environment variables.
      import MySQLdb
      
      db = MySQLdb.connect(host=os.environ.get('DB_HOST'), user=os.environ.get('DB_USER'),
                           passwd=os.environ.get('DB_PASSWORD'), db=os.environ.get('DB_NAME'))
      
      cursor = db.cursor()
      
      query = "SELECT id FROM users WHERE username = %s AND password_hash = %s"
      from werkzeug.security import generate_password_hash
      hashed_password = generate_password_hash(password)
      
      cursor.execute(query, (username, hashed_password))
      
      if cursor.rowcount == 1:
         # Check if the user is a moderator
         mod_query = "SELECT role FROM users WHERE id = %s"
         cursor.execute(mod_query, (cursor.lastrowid,))
         result = cursor.fetchone()
         is_mod = result[0] == 'moderator'
         
         return is_mod
      
      return False

   except Exception as e:
      # Log the error
      sys.stderr.write(f"Error in check_mod(): {e}")
      return False

   finally:
      try:
         if db:
               cursor.close()
               db.close()
      except Exception:
         pass