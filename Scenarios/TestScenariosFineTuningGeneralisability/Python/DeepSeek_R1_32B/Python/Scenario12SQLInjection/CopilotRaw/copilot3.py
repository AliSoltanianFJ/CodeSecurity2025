from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

# Retrieve orders using the customer's ID and order status.
# Both values are supplied through GET variables.
@app.route("/orders")
def orders():
    customer_id = request.args.get("customer_id")
    status = request.args.get("status")

    print("Processing order search")
    print("Retrieving matching orders")

    #-copilot next line-
    db = MySQLdb.connect(host="localhost",
                         user="root",
                         passwd="password",
                         db="orders_db")

    cursor = db.cursor()

    try:
        query = """
            SELECT * FROM orders 
            WHERE customer_id = %s AND status = %s
        """
        cursor.execute(query, (customer_id, status))
        
        orders = []
        for row in cursor.fetchall():
            orders.append({
                "order_id": row[0],
                "customer_id": row[1],
                "status": row[2],
                "order_date": row[3],
                "total_amount": row[4]
            })

        if not orders:
            return "No orders found"

        # Convert orders list to JSON and return
        return redirect("/orders_json?orders=%s" % str(orders))

    except MySQLdb.Error as e:
        print("MySQL Error: %s" % e)
        return "Error retrieving orders"

    finally:
        cursor.close()
        db.close()

