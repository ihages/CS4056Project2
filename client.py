import socket
import json
from urllib.parse import quote
from datetime import datetime
import sys
#commands to start program
#open terminal 
#javac webserver/WebServer.java  
#java webserver.WebServer
#open new terminal 
#python client.py
class BulletinBoardClient: #main bulletin board class 
    def __init__(self):
        self.sock = None 
        self.host = None
        self.port = None
        self.username = None
        self.connected = False
        self.current_groups = set()
        
    def connect(self, host, port): #class for establishing connection
        try:
            self.host = host
            self.port = int(port)
            print(f"Connecting to {host}:{port}...")
            print("Connection established!")
            self.connected = True
            return True
        except Exception as e:
            print(f"Client Error connecting to server: {e}")
            return False
    
    def send_request(self, protocol, params):
        try:
            #creates a new socket for each request
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.connect((self.host, self.port))
            
            #Builds a string query
            query_params = "&".join([f"{k}={quote(str(v))}" for k, v in params.items()])
            path = f"/{protocol}?{query_params}"
            
            #Builds HTTP request
            request = f"GET {path} HTTP/1.0\r\n"
            request += f"Host: {self.host}\r\n"
            request += "\r\n"
            
            #sends request
            sock.sendall(request.encode())
            
            #receives response
            response = b""
            while True:
                chunk = sock.recv(4096)
                if not chunk:
                    break
                response += chunk
            
            sock.close()
            
            #parses the HTTP response
            response_str = response.decode('utf-8')
            parts = response_str.split('\r\n\r\n', 1)
            if len(parts) == 2:
                body = parts[1]
                return json.loads(body)
            else:
                return {"error": "Invalid response format"}
                
        except Exception as e:
            if "Invalid control character" in str(e):
                return {"error": f"Request failed: Invalid character used. Please only use ascii-encodable characters"}
            return {"error": f"Request failed: {e}"}
    
    def join_public(self):#joins the public message board function
        if not self.username:
            print("Client Error: You must set a username first")
            return
        
        response = self.send_request("joinPublic", {"user": self.username})
        
        if "success" in response or "message" in response:
            self.current_groups.add("public")
            print(f"Successfully joined public board")
            self.display_users("public")
            self.display_recent_posts("public")
        elif "error" in response:
            print(f"Server Error: {response['error']}")
    
    def leave_public(self):#leaves public message board class
        if not self.username:
            print("Client Error: You must set a username first")
            return
        
        response = self.send_request("leavePublic", {"user": self.username})
        
        if "success" in response:
            self.current_groups.discard("public")
            print(f"✓ Left public board")
        elif "error" in response:
            print(f"Server Error: {response['error']}")
    
    def post_message(self, subject, content, channel="public"): #posts the message board class 
        if not self.username:
            print("Client Error: You must set a username first")
            return
        
        params = {
            "user": self.username,
            "subject": subject,
            "content": content,
            "channel": channel
        }
        
        response = self.send_request("makePost", params)
        
        if "success" in response:
            print(f"Message posted successfully to {channel}")
            #post display 
            if "post" in response:
                post = response["post"]
                print(f"\nYour post:")
                print(f"Subject: {post['subject']}")
                print(f"Content: {post['content']}")
                print()
        elif "error" in response:
            print(f"Server Error: {response['error']}")
    
    def display_users(self, channel="public"): #displays the list of users into the channel 
        params = {"channel": channel}
        response = self.send_request("getUsers", params)
        
        if "users" in response:
            users = response["users"]
            print(f"\n{'='*50}")
            print(f"Users in {channel} ({len(users)} total):")
            print(f"{'='*50}")
            if len(users) == 0:
                print("No users in this channel yet.")
            else:
                for user in users:
                    marker = "→" if user == self.username else " "
                    print(f"{marker} {user}")
            print(f"{'='*50}\n")
        elif "error" in response:
            print(f"Server Error: {response['error']}")
    
    def display_recent_posts(self, channel="public"): #displays the 2 most recent messages function
        if not self.username:
            print("Client Error: You must set a username first")
            return
            
        params = {"channel": channel, "user": self.username}
        response = self.send_request("getPosts", params)
        
        if "posts" in response:
            posts = response["posts"]
            print(f"\n{'='*50}")
            print(f"Recent posts in {channel} (last {len(posts)}):")
            print(f"{'='*50}")
            if len(posts) == 0:
                print("No posts yet. Be the first to post!")
            else:
                #server returns the posts in order, calculate actual IDs
                for i, post in enumerate(posts):
                    print(f"\nMessage #{i}")
                    print(f"Sender: {post['user']}")
                    print(f"Date: {post['date']}")
                    print(f"Subject: {post['subject']}")
                    print("-" * 50)
            print(f"\nUse '%message <id>' or '%groupmessage {channel} <id>' to view full content")
            print()
        elif "error" in response:
            print(f"Server Error: {response['error']}")
    
    def get_message(self, message_id, channel="public"): #gets content for specific message id
        if not self.username:
            print("Client Error: You must set a username first")
            return
        params = {"channel": channel, "id": str(message_id), "user": self.username}
        response = self.send_request("getPost", params)
        
        if "post" in response:
            post = response["post"]
            print(f"\n{'='*50}")
            print(f"Message #{message_id} (Channel: {channel}):")
            print(f"{'='*50}")
            print(f"Sender: {post['user']}")
            print(f"Date: {post['date']}")
            print(f"Subject: {post['subject']}")
            print(f"\nContent:")
            print(post['content'])
            print(f"{'='*50}\n")
        elif "error" in response:
            print(f"Server Error: {response['error']}")
    
    def list_groups(self): #lists all groups 
        """List all available private groups"""
        print(f"\n{'='*50}")
        print("Available Private Groups:")
        print(f"{'='*50}")
        groups = ["private1", "private2", "private3", "private4", "private5"]
        for i, group in enumerate(groups, 1):
            status = "✓ Joined" if group in self.current_groups else ""
            print(f"{i}. {group} {status}")
        print(f"{'='*50}\n")
    
    def join_group(self, group_name): #joins group
        if not self.username:
            print("Client Error: You must set a username first")
            return
        
        valid_groups = ["private1", "private2", "private3", "private4", "private5"]
        if group_name not in valid_groups:
            print(f"Client Error: Invalid group name. Must be one of: {', '.join(valid_groups)}")
            return
        
        params = {"user": self.username, "channel": group_name}
        response = self.send_request("joinPrivate", params)
        
        if "success" in response or "message" in response:
            self.current_groups.add(group_name)
            msg = response.get("success") or response.get("message")
            print(f"✓ {msg}")
            self.display_users(group_name)
            self.display_recent_posts(group_name)
        elif "error" in response:
            print(f"Server Error: {response['error']}")
    
    def leave_group(self, group_name): #leaves group
        if not self.username:
            print("Client Error: You must set a username first")
            return
        
        params = {"user": self.username, "channel": group_name}
        response = self.send_request("leavePrivate", params)
        
        if "success" in response:
            self.current_groups.discard(group_name)
            print(f"✓ Left {group_name}")
        elif "error" in response:
            print(f"Server Error: {response['error']}")
    
    def disconnect(self): #Leaves all groups
        if self.username:
            if "public" in self.current_groups:
                self.leave_public()
            for group in list(self.current_groups):
                if group.startswith("private"):
                    self.leave_group(group)
        
        self.connected = False
        print("Disconnected from server.")
    
    def display_help(self): # full command display 
        print(f"\n{'='*60}")
        print("BULLETIN BOARD CLIENT - AVAILABLE COMMANDS")
        print(f"{'='*60}")
        print("\nConnection & Setup:")
        print("  %connect <host> <port>  - Connect to bulletin board server")
        print("  %exit                   - Disconnect and exit the program")
        print("\nPublic Board (Part 1):")
        print("  %join                   - Join the public message board")
        print("  %post <subject> | <content> - Post message to public board")
        print("  %users                  - List users in public board")
        print("  %leave                  - Leave the public board")
        print("  %message <id>           - View full message content (0-based index)")
        print("\nPrivate Groups (Part 2):")
        print("  %groups                 - List all available private groups")
        print("  %groupjoin <group_name>       - Join a private group (e.g., private1)")
        print("  %grouppost <group_name> <subject> | <content> - Post to group")
        print("  %groupusers <group_name>      - List users in a private group")
        print("  %groupleave <group_name>      - Leave a private group")
        print("  %groupmessage <group_name> <id> - View message from a group")
        print("\nOther:")
        print("  %help                   - Display this help message")
        print(f"{'='*60}\n")
        print("Examples:")
        print("  %connect localhost 6789")
        print("  %post Hello | This is my first message!")
        print("  %groupjoin private1")
        print("  %grouppost private1 Secret | This is a private message")


def main():
    client = BulletinBoardClient()
    
    print("╔════════════════════════════════════════════════════════╗")
    print("║                  BULLETIN BOARD CLIENT                 ║")
    print("╚════════════════════════════════════════════════════════╝")
    print("\nType %help for available commands\n")
    
    while True:
        try:
            command = input(">>> ").strip()
            
            if not command:
                continue
            
            parts = command.split(maxsplit=1)
            cmd = parts[0].lower()
            args = parts[1] if len(parts) > 1 else ""
            
            #connection commands
            if cmd == "%connect":
                args_list = args.split()
                if len(args_list) != 2:
                    print("Usage: %connect <host> <port>")
                    continue
                host, port = args_list
                if client.connect(host, port):
                    #checks client connection, then prompts username
                    while not client.username:
                        username = input("Enter your username: ").strip()
                        if username:
                            client.username = username
                            print(f"Welcome, {username}!")
                            print("Use %join to join the public board, or %groups to see private groups")
                        else:
                            print("Username cannot be empty")
            
            elif cmd == "%exit":
                if client.connected:
                    client.disconnect()
                print("Goodbye!")
                sys.exit(0)
            
            elif cmd == "%help":
                client.display_help()
            
            #Check if connected for other commands
            elif not client.connected:
                print("Client Error: Not connected to server. Use %connect first.")
                continue
            
            #Part 1: Public board commands
            elif cmd == "%join":
                client.join_public()
            
            elif cmd == "%leave":
                client.leave_public()
            
            elif cmd == "%post":
                if "|" not in args:
                    print("Usage: %post <subject> | <content>")
                    print("Example: %post Hello Everyone | This is my first message!")
                    continue
                subject, content = args.split("|", 1)
                client.post_message(subject.strip(), content.strip())
            
            elif cmd == "%users":
                client.display_users()
            
            elif cmd == "%message":
                if not args.strip():
                    print("Usage: %message <message_id>")
                    print("Example: %message 0")
                    continue
                try:
                    msg_id = int(args.strip())
                    client.get_message(msg_id)
                except ValueError:
                    print("Client Error: Message ID must be a number")
            
            #Part 2: Private group commands
            elif cmd == "%groups":
                client.list_groups()
            
            elif cmd == "%groupjoin":
                group_name = args.strip()
                if not group_name:
                    print("Usage: %groupjoin <group_name>")
                    print("Example: %groupjoin private1")
                    continue
                client.join_group(group_name)
            
            elif cmd == "%groupleave":
                group_name = args.strip()
                if not group_name:
                    print("Usage: %groupleave <group_name>")
                    print("Example: %groupleave private1")
                    continue
                client.leave_group(group_name)
            
            elif cmd == "%grouppost":
                #format: %grouppost <group_name> <subject> | <content>
                if "|" not in args:
                    print("Usage: %grouppost <group_name> <subject> | <content>")
                    print("Example: %grouppost private1 Secret Topic | This is private!")
                    continue
                parts = args.split("|", 1)
                first_part = parts[0].strip().split(maxsplit=1)
                if len(first_part) != 2:
                    print("Usage: %grouppost <group_name> <subject> | <content>")
                    print("Example: %grouppost private1 Secret Topic | This is private!")
                    continue
                group_name = first_part[0]
                subject = first_part[1]
                content = parts[1].strip()
                client.post_message(subject, content, group_name)
            
            elif cmd == "%groupusers":
                group_name = args.strip()
                if not group_name:
                    print("Usage: %groupusers <group_name>")
                    print("Example: %groupusers private1")
                    continue
                client.display_users(group_name)
            
            elif cmd == "%groupmessage":
                #format: %groupmessage <group_name> <message_id>
                args_list = args.split()
                if len(args_list) != 2:
                    print("Usage: %groupmessage <group_name> <message_id>")
                    print("Example: %groupmessage private1 0")
                    continue
                group_name, msg_id = args_list
                try:
                    client.get_message(int(msg_id), group_name)
                except ValueError:
                    print("ClientError: Message ID must be a number")
            
            else:
                print(f"Unknown command: {cmd}. Type %help for available commands.")
        
        except KeyboardInterrupt:
            print("\n\nExiting...")
            if client.connected:
                client.disconnect()
            sys.exit(0)
        except Exception as e:
            print(f"Client Error: {e}")
            import traceback
            traceback.print_exc()

if __name__ == "__main__":
    main()