#Test script to add more actions to YOUR-PAX 

b_class = "IDLE"   
b_module = "IDLE"
b_status = "IDLE"
b_port = None  
b_parent = None 

class IDLE:
    def __init__(self, shared_data):
        self.shared_data = shared_data

    def execute(self, ip="", port="", row=None, status_key=""):
        return 'success'
        


    
