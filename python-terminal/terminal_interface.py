#!/usr/bin/env python3
"""
Terminal Interface for M5StickC Plus2 Debug System
Real-time terminal interface with color-coded output and interactive commands
"""

import asyncio
import websockets
import json
import sys
import os
import signal
import threading
import time
from datetime import datetime
from typing import Dict, List, Optional
import argparse

# Terminal colors
class Colors:
    RESET = '\033[0m'
    BOLD = '\033[1m'
    DIM = '\033[2m'
    
    # Standard colors
    BLACK = '\033[30m'
    RED = '\033[31m'
    GREEN = '\033[32m'
    YELLOW = '\033[33m'
    BLUE = '\033[34m'
    MAGENTA = '\033[35m'
    CYAN = '\033[36m'
    WHITE = '\033[37m'
    
    # Bright colors
    BRIGHT_BLACK = '\033[90m'
    BRIGHT_RED = '\033[91m'
    BRIGHT_GREEN = '\033[92m'
    BRIGHT_YELLOW = '\033[93m'
    BRIGHT_BLUE = '\033[94m'
    BRIGHT_MAGENTA = '\033[95m'
    BRIGHT_CYAN = '\033[96m'
    BRIGHT_WHITE = '\033[97m'
    
    # Background colors
    BG_RED = '\033[41m'
    BG_GREEN = '\033[42m'
    BG_YELLOW = '\033[43m'
    BG_BLUE = '\033[44m'

class TerminalInterface:
    """Terminal interface for M5StickC Plus2 debug monitoring"""
    
    def __init__(self, server_host: str = "localhost", server_port: int = 8765):
        self.server_host = server_host
        self.server_port = server_port
        self.websocket = None
        self.running = False
        self.connected = False
        self.data_counts = {}
        self.last_data = {}
        self.start_time = datetime.now()
        self.command_mode = False
        
        # Display settings
        self.show_debug = True
        self.show_memory = True
        self.show_performance = True
        self.show_logs = True
        self.max_lines = 50
        
        # Setup terminal
        self.setup_terminal()
    
    def setup_terminal(self):
        """Setup terminal for optimal display"""
        os.system('clear')
        # Hide cursor
        print('\033[?25l', end='')
        sys.stdout.flush()
    
    def cleanup_terminal(self):
        """Cleanup terminal settings"""
        # Show cursor
        print('\033[?25h', end='')
        print(f"{Colors.RESET}")
        sys.stdout.flush()
    
    def print_header(self):
        """Print the terminal header"""
        os.system('clear')
        uptime = datetime.now() - self.start_time
        uptime_str = str(uptime).split('.')[0]
        
        print(f"{Colors.BOLD}{Colors.CYAN}{'='*80}{Colors.RESET}")
        print(f"{Colors.BOLD}{Colors.CYAN}M5StickC Plus2 Debug Terminal{Colors.RESET}")
        print(f"{Colors.BOLD}{Colors.CYAN}{'='*80}{Colors.RESET}")
        
        # Connection status
        status_color = Colors.GREEN if self.connected else Colors.RED
        status_text = "CONNECTED" if self.connected else "DISCONNECTED"
        print(f"Status: {status_color}{Colors.BOLD}{status_text}{Colors.RESET} | "
              f"Uptime: {Colors.YELLOW}{uptime_str}{Colors.RESET} | "
              f"Server: {Colors.BLUE}{self.server_host}:{self.server_port}{Colors.RESET}")
        
        # Data statistics
        total_messages = sum(self.data_counts.values())
        print(f"Messages: {Colors.GREEN}{total_messages}{Colors.RESET} | "
              f"Types: {Colors.CYAN}{', '.join(self.data_counts.keys())}{Colors.RESET}")
        
        print(f"{Colors.DIM}{'─'*80}{Colors.RESET}")
        
        # Quick help
        print(f"{Colors.DIM}Commands: 'h' help | 'q' quit | 'c' clear | 's' stats | 'cmd' command mode{Colors.RESET}")
        print(f"{Colors.DIM}{'─'*80}{Colors.RESET}")
    
    def format_timestamp(self, timestamp):
        """Format timestamp for display"""
        if isinstance(timestamp, (int, float)):
            # Convert milliseconds to seconds
            if timestamp > 1000000000000:  # Likely milliseconds
                timestamp = timestamp / 1000
            dt = datetime.fromtimestamp(timestamp)
        else:
            dt = datetime.fromisoformat(timestamp.replace('Z', '+00:00'))
        return dt.strftime('%H:%M:%S.%f')[:-3]  # Show milliseconds
    
    def print_debug_data(self, data: Dict):
        """Print debug data with color coding"""
        timestamp = self.format_timestamp(data.get('timestamp', time.time()))
        
        print(f"{Colors.BRIGHT_BLUE}[{timestamp}] DEBUG{Colors.RESET}")
        
        # System info
        uptime = data.get('uptime', 0) / 1000  # Convert to seconds
        loop_count = data.get('loop_count', 0)
        healthy = data.get('system_healthy', True)
        
        health_color = Colors.GREEN if healthy else Colors.RED
        health_text = "HEALTHY" if healthy else "UNHEALTHY"
        
        print(f"  System: {health_color}{health_text}{Colors.RESET} | "
              f"Uptime: {Colors.YELLOW}{uptime:.1f}s{Colors.RESET} | "
              f"Loops: {Colors.CYAN}{loop_count}{Colors.RESET}")
        
        # WiFi info
        wifi_rssi = data.get('wifi_rssi', 0)
        wifi_status = data.get('wifi_status', 0)
        
        rssi_color = Colors.GREEN if wifi_rssi > -60 else Colors.YELLOW if wifi_rssi > -80 else Colors.RED
        
        print(f"  WiFi: Status={Colors.CYAN}{wifi_status}{Colors.RESET} | "
              f"RSSI: {rssi_color}{wifi_rssi}dBm{Colors.RESET}")
        
        # Sensor data
        sensors = data.get('sensors', {})
        if sensors:
            accel = sensors.get('accel', {})
            gyro = sensors.get('gyro', {})
            
            if accel:
                print(f"  Accel: X={Colors.GREEN}{accel.get('x', 0):.3f}{Colors.RESET} "
                      f"Y={Colors.GREEN}{accel.get('y', 0):.3f}{Colors.RESET} "
                      f"Z={Colors.GREEN}{accel.get('z', 0):.3f}{Colors.RESET}")
            
            if gyro:
                print(f"  Gyro:  X={Colors.BLUE}{gyro.get('x', 0):.3f}{Colors.RESET} "
                      f"Y={Colors.BLUE}{gyro.get('y', 0):.3f}{Colors.RESET} "
                      f"Z={Colors.BLUE}{gyro.get('z', 0):.3f}{Colors.RESET}")
    
    def print_memory_data(self, data: Dict):
        """Print memory information with color coding"""
        timestamp = self.format_timestamp(data.get('timestamp', time.time()))
        
        print(f"{Colors.BRIGHT_MAGENTA}[{timestamp}] MEMORY{Colors.RESET}")
        
        heap = data.get('heap', {})
        if heap:
            total = heap.get('total', 0)
            free = heap.get('free', 0)
            used = heap.get('used', 0)
            largest_free = heap.get('largest_free_block', 0)
            
            # Calculate percentage
            if total > 0:
                used_percent = (used / total) * 100
                color = Colors.GREEN if used_percent < 70 else Colors.YELLOW if used_percent < 90 else Colors.RED
            else:
                color = Colors.WHITE
                used_percent = 0
            
            print(f"  Heap: {color}{used}/{total} bytes ({used_percent:.1f}%){Colors.RESET} | "
                  f"Free: {Colors.GREEN}{free}{Colors.RESET} | "
                  f"Largest: {Colors.CYAN}{largest_free}{Colors.RESET}")
        
        # PSRAM info
        psram = data.get('psram', {})
        if psram and psram.get('total', 0) > 0:
            total = psram.get('total', 0)
            free = psram.get('free', 0)
            used = psram.get('used', 0)
            
            print(f"  PSRAM: {Colors.CYAN}{used}/{total} bytes{Colors.RESET} | "
                  f"Free: {Colors.GREEN}{free}{Colors.RESET}")
        
        # Flash info
        flash = data.get('flash', {})
        if flash:
            size = flash.get('size', 0)
            speed = flash.get('speed', 0)
            print(f"  Flash: Size={Colors.BLUE}{size} bytes{Colors.RESET} | "
                  f"Speed={Colors.YELLOW}{speed} Hz{Colors.RESET}")
    
    def print_performance_data(self, data: Dict):
        """Print performance metrics with color coding"""
        timestamp = self.format_timestamp(data.get('timestamp', time.time()))
        
        print(f"{Colors.BRIGHT_GREEN}[{timestamp}] PERFORMANCE{Colors.RESET}")
        
        cpu = data.get('cpu', {})
        performance = data.get('performance', {})
        system = data.get('system', {})
        network = data.get('network', {})
        
        if cpu:
            freq = cpu.get('frequency', 0)
            print(f"  CPU: Frequency={Colors.CYAN}{freq}MHz{Colors.RESET}")
        
        if performance:
            avg_loop = performance.get('avg_loop_time', 0)
            loops_per_sec = performance.get('loops_per_second', 0)
            uptime = performance.get('uptime', 0) / 1000
            
            loop_color = Colors.GREEN if avg_loop < 50 else Colors.YELLOW if avg_loop < 100 else Colors.RED
            
            print(f"  Loop: {loop_color}{avg_loop:.2f}ms{Colors.RESET} | "
                  f"Rate: {Colors.CYAN}{loops_per_sec:.1f}/s{Colors.RESET} | "
                  f"Uptime: {Colors.YELLOW}{uptime:.1f}s{Colors.RESET}")
        
        if system:
            temp = system.get('temperature', 0)
            temp_color = Colors.GREEN if temp < 60 else Colors.YELLOW if temp < 80 else Colors.RED
            print(f"  Temperature: {temp_color}{temp:.1f}°C{Colors.RESET}")
        
        if network:
            wifi_reconnects = network.get('wifi_reconnects', 0)
            ws_reconnects = network.get('websocket_reconnects', 0)
            
            reconnect_color = Colors.GREEN if (wifi_reconnects + ws_reconnects) == 0 else Colors.YELLOW
            print(f"  Reconnects: WiFi={reconnect_color}{wifi_reconnects}{Colors.RESET} | "
                  f"WS={reconnect_color}{ws_reconnects}{Colors.RESET}")
    
    def print_error_log(self, data: Dict):
        """Print error/log messages with color coding"""
        timestamp = self.format_timestamp(data.get('timestamp', time.time()))
        level = data.get('level', 'INFO')
        message = data.get('message', '')
        
        # Color based on level
        if level == 'ERROR':
            color = Colors.RED
            prefix = '✗'
        elif level == 'WARNING':
            color = Colors.YELLOW
            prefix = '⚠'
        elif level == 'INFO':
            color = Colors.BLUE
            prefix = 'ℹ'
        elif level == 'USER':
            color = Colors.MAGENTA
            prefix = '👤'
        else:
            color = Colors.WHITE
            prefix = '•'
        
        print(f"{color}[{timestamp}] {prefix} {level}: {message}{Colors.RESET}")
    
    def print_system_status(self, data: Dict):
        """Print system status information"""
        timestamp = self.format_timestamp(data.get('timestamp', time.time()))
        
        print(f"{Colors.BRIGHT_CYAN}[{timestamp}] SYSTEM STATUS{Colors.RESET}")
        
        system = data.get('system', {})
        wifi = data.get('wifi', {})
        websocket = data.get('websocket', {})
        
        if system:
            chip_model = system.get('chip_model', 'Unknown')
            healthy = system.get('healthy', True)
            
            health_color = Colors.GREEN if healthy else Colors.RED
            health_text = "HEALTHY" if healthy else "UNHEALTHY"
            
            print(f"  System: {Colors.CYAN}{chip_model}{Colors.RESET} | "
                  f"Status: {health_color}{health_text}{Colors.RESET}")
        
        if wifi:
            connected = wifi.get('connected', False)
            ssid = wifi.get('ssid', 'N/A')
            ip = wifi.get('ip', 'N/A')
            rssi = wifi.get('rssi', 0)
            
            wifi_color = Colors.GREEN if connected else Colors.RED
            wifi_status = "CONNECTED" if connected else "DISCONNECTED"
            
            print(f"  WiFi: {wifi_color}{wifi_status}{Colors.RESET} | "
                  f"SSID: {Colors.YELLOW}{ssid}{Colors.RESET} | "
                  f"IP: {Colors.BLUE}{ip}{Colors.RESET}")
        
        if websocket:
            ws_connected = websocket.get('connected', False)
            ws_color = Colors.GREEN if ws_connected else Colors.RED
            ws_status = "CONNECTED" if ws_connected else "DISCONNECTED"
            
            print(f"  WebSocket: {ws_color}{ws_status}{Colors.RESET}")
    
    def process_message(self, data: Dict):
        """Process incoming message and display appropriately"""
        msg_type = data.get('type', 'unknown')
        
        # Update statistics
        self.data_counts[msg_type] = self.data_counts.get(msg_type, 0) + 1
        self.last_data[msg_type] = data
        
        # Display based on type and settings
        if msg_type == 'debug' and self.show_debug:
            self.print_debug_data(data)
        elif msg_type == 'memory' and self.show_memory:
            self.print_memory_data(data)
        elif msg_type == 'performance' and self.show_performance:
            self.print_performance_data(data)
        elif msg_type == 'error_log' and self.show_logs:
            self.print_error_log(data)
        elif msg_type == 'system_status':
            self.print_system_status(data)
        elif msg_type == 'server_message':
            message = data.get('message', '')
            print(f"{Colors.BRIGHT_YELLOW}[SERVER] {message}{Colors.RESET}")
        
        print()  # Add spacing
    
    async def connect_to_server(self):
        """Connect to the debug server"""
        try:
            uri = f"ws://{self.server_host}:{self.server_port}/"
            print(f"Connecting to {uri}...")
            
            self.websocket = await websockets.connect(uri)
            self.connected = True
            print(f"{Colors.GREEN}Connected successfully!{Colors.RESET}")
            
            return True
            
        except Exception as e:
            print(f"{Colors.RED}Connection failed: {e}{Colors.RESET}")
            self.connected = False
            return False
    
    async def listen_for_messages(self):
        """Listen for messages from the server"""
        try:
            async for message in self.websocket:
                data = json.loads(message)
                self.process_message(data)
                
        except websockets.exceptions.ConnectionClosed:
            print(f"{Colors.RED}Connection to server lost{Colors.RESET}")
            self.connected = False
        except Exception as e:
            print(f"{Colors.RED}Error receiving message: {e}{Colors.RESET}")
    
    def handle_command(self, command: str):
        """Handle user commands"""
        cmd = command.strip().lower()
        
        if cmd == 'h' or cmd == 'help':
            self.show_help()
        elif cmd == 'q' or cmd == 'quit':
            self.running = False
        elif cmd == 'c' or cmd == 'clear':
            os.system('clear')
            self.print_header()
        elif cmd == 's' or cmd == 'stats':
            self.show_statistics()
        elif cmd == 'cmd' or cmd == 'command':
            self.enter_command_mode()
        elif cmd.startswith('toggle'):
            self.handle_toggle_command(cmd)
        else:
            print(f"{Colors.RED}Unknown command: {cmd}{Colors.RESET}")
            print(f"{Colors.DIM}Type 'h' for help{Colors.RESET}")
    
    def show_help(self):
        """Show help information"""
        print(f"\n{Colors.BRIGHT_CYAN}HELP - Available Commands{Colors.RESET}")
        print(f"{Colors.DIM}{'─'*40}{Colors.RESET}")
        print(f"{Colors.YELLOW}h, help{Colors.RESET}     - Show this help")
        print(f"{Colors.YELLOW}q, quit{Colors.RESET}     - Quit the terminal")
        print(f"{Colors.YELLOW}c, clear{Colors.RESET}    - Clear the screen")
        print(f"{Colors.YELLOW}s, stats{Colors.RESET}    - Show statistics")
        print(f"{Colors.YELLOW}cmd{Colors.RESET}         - Enter command mode (send to M5)")
        print(f"{Colors.YELLOW}toggle debug{Colors.RESET} - Toggle debug messages")
        print(f"{Colors.YELLOW}toggle memory{Colors.RESET} - Toggle memory messages")
        print(f"{Colors.YELLOW}toggle perf{Colors.RESET} - Toggle performance messages")
        print(f"{Colors.YELLOW}toggle logs{Colors.RESET} - Toggle log messages")
        print(f"{Colors.DIM}{'─'*40}{Colors.RESET}\n")
    
    def show_statistics(self):
        """Show detailed statistics"""
        uptime = datetime.now() - self.start_time
        print(f"\n{Colors.BRIGHT_CYAN}STATISTICS{Colors.RESET}")
        print(f"{Colors.DIM}{'─'*40}{Colors.RESET}")
        print(f"Uptime: {Colors.YELLOW}{str(uptime).split('.')[0]}{Colors.RESET}")
        print(f"Connected: {Colors.GREEN if self.connected else Colors.RED}{self.connected}{Colors.RESET}")
        print(f"Total Messages: {Colors.CYAN}{sum(self.data_counts.values())}{Colors.RESET}")
        
        print(f"\nMessage Types:")
        for msg_type, count in self.data_counts.items():
            print(f"  {Colors.YELLOW}{msg_type}{Colors.RESET}: {Colors.CYAN}{count}{Colors.RESET}")
        
        print(f"\nDisplay Settings:")
        print(f"  Debug: {Colors.GREEN if self.show_debug else Colors.RED}{self.show_debug}{Colors.RESET}")
        print(f"  Memory: {Colors.GREEN if self.show_memory else Colors.RED}{self.show_memory}{Colors.RESET}")
        print(f"  Performance: {Colors.GREEN if self.show_performance else Colors.RED}{self.show_performance}{Colors.RESET}")
        print(f"  Logs: {Colors.GREEN if self.show_logs else Colors.RED}{self.show_logs}{Colors.RESET}")
        print(f"{Colors.DIM}{'─'*40}{Colors.RESET}\n")
    
    def handle_toggle_command(self, cmd: str):
        """Handle toggle commands"""
        parts = cmd.split()
        if len(parts) < 2:
            print(f"{Colors.RED}Usage: toggle [debug|memory|perf|logs]{Colors.RESET}")
            return
        
        toggle_type = parts[1]
        
        if toggle_type == 'debug':
            self.show_debug = not self.show_debug
            status = "enabled" if self.show_debug else "disabled"
            print(f"Debug messages {Colors.CYAN}{status}{Colors.RESET}")
        elif toggle_type == 'memory':
            self.show_memory = not self.show_memory
            status = "enabled" if self.show_memory else "disabled"
            print(f"Memory messages {Colors.CYAN}{status}{Colors.RESET}")
        elif toggle_type in ['perf', 'performance']:
            self.show_performance = not self.show_performance
            status = "enabled" if self.show_performance else "disabled"
            print(f"Performance messages {Colors.CYAN}{status}{Colors.RESET}")
        elif toggle_type == 'logs':
            self.show_logs = not self.show_logs
            status = "enabled" if self.show_logs else "disabled"
            print(f"Log messages {Colors.CYAN}{status}{Colors.RESET}")
        else:
            print(f"{Colors.RED}Unknown toggle type: {toggle_type}{Colors.RESET}")
    
    async def enter_command_mode(self):
        """Enter command mode to send commands to M5"""
        print(f"\n{Colors.BRIGHT_YELLOW}COMMAND MODE{Colors.RESET}")
        print(f"{Colors.DIM}Send commands to M5StickC Plus2. Type 'exit' to return.{Colors.RESET}")
        print(f"{Colors.DIM}Available commands: get_status, get_memory, get_performance, restart{Colors.RESET}")
        
        while True:
            try:
                cmd = input(f"{Colors.YELLOW}M5> {Colors.RESET}")
                
                if cmd.strip().lower() == 'exit':
                    break
                
                if self.websocket and self.connected:
                    command_data = {
                        "command": cmd.strip(),
                        "timestamp": datetime.now().isoformat()
                    }
                    await self.websocket.send(json.dumps(command_data))
                    print(f"{Colors.GREEN}Command sent: {cmd}{Colors.RESET}")
                else:
                    print(f"{Colors.RED}Not connected to server{Colors.RESET}")
                    break
                    
            except (KeyboardInterrupt, EOFError):
                break
        
        print(f"{Colors.DIM}Exited command mode{Colors.RESET}")
    
    async def run(self):
        """Main run loop"""
        self.running = True
        
        try:
            # Connect to server
            if not await self.connect_to_server():
                return
            
            self.print_header()
            
            # Start listening for messages in the background
            listen_task = asyncio.create_task(self.listen_for_messages())
            
            # Command input loop
            while self.running and self.connected:
                try:
                    # Check if we have input available (non-blocking)
                    import select
                    if select.select([sys.stdin], [], [], 0.1)[0]:
                        command = input().strip()
                        if command:
                            if command.startswith('cmd'):
                                await self.enter_command_mode()
                            else:
                                self.handle_command(command)
                    
                    # Update header periodically
                    await asyncio.sleep(0.1)
                    
                except (KeyboardInterrupt, EOFError):
                    self.running = False
                    break
            
            # Cancel listening task
            listen_task.cancel()
            
        except Exception as e:
            print(f"{Colors.RED}Error: {e}{Colors.RESET}")
        finally:
            if self.websocket:
                await self.websocket.close()
            self.cleanup_terminal()

def main():
    """Main function"""
    parser = argparse.ArgumentParser(description='M5StickC Plus2 Debug Terminal Interface')
    parser.add_argument('--host', default='localhost', help='Server host (default: localhost)')
    parser.add_argument('--port', type=int, default=8765, help='Server port (default: 8765)')
    
    args = parser.parse_args()
    
    # Setup signal handling
    def signal_handler(signum, frame):
        print(f"\n{Colors.YELLOW}Shutting down...{Colors.RESET}")
        sys.exit(0)
    
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)
    
    # Create and run terminal interface
    terminal = TerminalInterface(args.host, args.port)
    
    try:
        asyncio.run(terminal.run())
    except KeyboardInterrupt:
        print(f"\n{Colors.YELLOW}Terminal interface stopped{Colors.RESET}")
    except Exception as e:
        print(f"{Colors.RED}Error: {e}{Colors.RESET}")

if __name__ == "__main__":
    main()