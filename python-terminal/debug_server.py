#!/usr/bin/env python3
"""
Debug Server for M5StickC Plus2
WebSocket server to receive debug data from M5StickC Plus2
"""

import asyncio
import websockets
import json
import logging
import threading
import time
from datetime import datetime
from typing import Dict, List, Optional
import signal
import sys

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    datefmt='%H:%M:%S'
)
logger = logging.getLogger(__name__)

class DebugDataProcessor:
    """Process and store debug data from M5StickC Plus2"""
    
    def __init__(self):
        self.clients: Dict[str, Dict] = {}
        self.data_history: List[Dict] = []
        self.max_history = 1000
        self.stats = {
            'total_messages': 0,
            'messages_by_type': {},
            'connected_clients': 0,
            'start_time': datetime.now()
        }
    
    def add_client(self, client_id: str, websocket):
        """Add a new client connection"""
        self.clients[client_id] = {
            'websocket': websocket,
            'connected_at': datetime.now(),
            'last_seen': datetime.now(),
            'message_count': 0
        }
        self.stats['connected_clients'] = len(self.clients)
        logger.info(f"Client {client_id} connected. Total clients: {len(self.clients)}")
    
    def remove_client(self, client_id: str):
        """Remove a client connection"""
        if client_id in self.clients:
            del self.clients[client_id]
            self.stats['connected_clients'] = len(self.clients)
            logger.info(f"Client {client_id} disconnected. Total clients: {len(self.clients)}")
    
    def process_data(self, client_id: str, data: Dict):
        """Process incoming debug data"""
        try:
            # Update client stats
            if client_id in self.clients:
                self.clients[client_id]['last_seen'] = datetime.now()
                self.clients[client_id]['message_count'] += 1
            
            # Update global stats
            self.stats['total_messages'] += 1
            msg_type = data.get('type', 'unknown')
            self.stats['messages_by_type'][msg_type] = self.stats['messages_by_type'].get(msg_type, 0) + 1
            
            # Add timestamp and client info
            data['server_timestamp'] = datetime.now().isoformat()
            data['client_id'] = client_id
            
            # Store in history
            self.data_history.append(data)
            if len(self.data_history) > self.max_history:
                self.data_history.pop(0)
            
            return True
            
        except Exception as e:
            logger.error(f"Error processing data from {client_id}: {e}")
            return False
    
    def get_latest_data(self, msg_type: Optional[str] = None, limit: int = 10) -> List[Dict]:
        """Get latest data entries"""
        if msg_type:
            filtered_data = [d for d in self.data_history if d.get('type') == msg_type]
            return filtered_data[-limit:]
        return self.data_history[-limit:]
    
    def get_stats(self) -> Dict:
        """Get server statistics"""
        uptime = datetime.now() - self.stats['start_time']
        return {
            **self.stats,
            'uptime_seconds': uptime.total_seconds(),
            'uptime_formatted': str(uptime).split('.')[0],
            'clients': {cid: {
                'connected_at': client['connected_at'].isoformat(),
                'last_seen': client['last_seen'].isoformat(),
                'message_count': client['message_count']
            } for cid, client in self.clients.items()}
        }

class M5DebugServer:
    """WebSocket server for M5StickC Plus2 debug streaming"""
    
    def __init__(self, host: str = "0.0.0.0", port: int = 8765):
        self.host = host
        self.port = port
        self.processor = DebugDataProcessor()
        self.running = False
        self.server = None
    
    async def handle_client(self, websocket):
        """Handle individual client connections"""
        client_id = f"{websocket.remote_address[0]}:{websocket.remote_address[1]}"
        self.processor.add_client(client_id, websocket)
        
        try:
            # Send welcome message
            welcome = {
                "type": "server_message",
                "message": "Connected to M5 Debug Server",
                "server_time": datetime.now().isoformat(),
                "client_id": client_id
            }
            await websocket.send(json.dumps(welcome))
            
            async for message in websocket:
                try:
                    data = json.loads(message)
                    success = self.processor.process_data(client_id, data)
                    
                    if not success:
                        error_response = {
                            "type": "error",
                            "message": "Failed to process data",
                            "timestamp": datetime.now().isoformat()
                        }
                        await websocket.send(json.dumps(error_response))
                    
                    # Handle specific message types
                    await self.handle_message_type(websocket, client_id, data)
                    
                except json.JSONDecodeError as e:
                    logger.error(f"Invalid JSON from {client_id}: {e}")
                    error_response = {
                        "type": "error",
                        "message": f"Invalid JSON: {str(e)}",
                        "timestamp": datetime.now().isoformat()
                    }
                    await websocket.send(json.dumps(error_response))
                
                except Exception as e:
                    logger.error(f"Error handling message from {client_id}: {e}")
        
        except websockets.exceptions.ConnectionClosed:
            logger.info(f"Client {client_id} connection closed")
        except Exception as e:
            logger.error(f"Error with client {client_id}: {e}")
        finally:
            self.processor.remove_client(client_id)
    
    async def handle_message_type(self, websocket, client_id: str, data: Dict):
        """Handle different message types"""
        msg_type = data.get('type')
        
        if msg_type == 'error_log':
            level = data.get('level', 'INFO')
            message = data.get('message', '')
            timestamp = data.get('timestamp', 0)
            
            # Log to console with appropriate level
            if level == 'ERROR':
                logger.error(f"[{client_id}] {message}")
            elif level == 'WARNING':
                logger.warning(f"[{client_id}] {message}")
            else:
                logger.info(f"[{client_id}] {message}")
        
        elif msg_type == 'system_status':
            # Could trigger alerts or notifications
            healthy = data.get('system', {}).get('healthy', True)
            if not healthy:
                logger.warning(f"[{client_id}] System reported as unhealthy!")
        
        elif msg_type == 'memory':
            # Check for low memory warnings
            free_heap = data.get('heap', {}).get('free', 0)
            if free_heap < 10000:  # Less than 10KB
                logger.warning(f"[{client_id}] Low memory warning: {free_heap} bytes free")
    
    async def start_server(self):
        """Start the WebSocket server"""
        logger.info(f"Starting M5 Debug Server on {self.host}:{self.port}")
        self.running = True
        
        try:
            self.server = await websockets.serve(
                self.handle_client,
                self.host,
                self.port,
                ping_interval=30,
                ping_timeout=10
            )
            logger.info("Server started successfully!")
            
            # Keep server running
            await self.server.wait_closed()
            
        except Exception as e:
            logger.error(f"Server error: {e}")
            self.running = False
    
    def stop_server(self):
        """Stop the WebSocket server"""
        logger.info("Stopping server...")
        self.running = False
        if self.server:
            self.server.close()
    
    def print_stats(self):
        """Print server statistics"""
        stats = self.processor.get_stats()
        print("\n" + "="*50)
        print("M5 DEBUG SERVER STATISTICS")
        print("="*50)
        print(f"Uptime: {stats['uptime_formatted']}")
        print(f"Connected Clients: {stats['connected_clients']}")
        print(f"Total Messages: {stats['total_messages']}")
        print("\nMessages by Type:")
        for msg_type, count in stats['messages_by_type'].items():
            print(f"  {msg_type}: {count}")
        
        if stats['clients']:
            print("\nConnected Clients:")
            for client_id, client_info in stats['clients'].items():
                print(f"  {client_id}: {client_info['message_count']} messages")
        print("="*50)

def signal_handler(signum, frame, server):
    """Handle shutdown signals"""
    print("\nShutdown signal received...")
    server.stop_server()
    sys.exit(0)

async def status_printer(server):
    """Print status updates periodically"""
    while server.running:
        await asyncio.sleep(30)  # Print stats every 30 seconds
        if server.running:
            server.print_stats()

async def main():
    """Main server function"""
    server = M5DebugServer()
    
    # Setup signal handlers
    signal.signal(signal.SIGINT, lambda s, f: signal_handler(s, f, server))
    signal.signal(signal.SIGTERM, lambda s, f: signal_handler(s, f, server))
    
    try:
        # Start status printer task
        status_task = asyncio.create_task(status_printer(server))
        
        # Start server
        await server.start_server()
        
    except KeyboardInterrupt:
        logger.info("Keyboard interrupt received")
    except Exception as e:
        logger.error(f"Unexpected error: {e}")
    finally:
        server.stop_server()
        logger.info("Server shutdown complete")

if __name__ == "__main__":
    print("M5StickC Plus2 Debug Server")
    print("===========================")
    print("WebSocket server for receiving debug data from M5StickC Plus2")
    print("Press Ctrl+C to stop\n")
    
    asyncio.run(main())