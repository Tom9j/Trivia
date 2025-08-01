# VRAM System for M5StickC Plus2

Advanced Virtual RAM system that enables M5StickC Plus2 to dynamically access large resources from a server with intelligent memory management and automatic caching.

## 🏗️ System Architecture

The VRAM system consists of three main components:

1. **Flask Resource Server** - Manages and serves resources with compression and versioning
2. **M5StickC Plus2 Client** - Handles memory management, caching, and WiFi communication
3. **Communication Protocol** - HTTP/JSON based with automatic retry and recovery

## 📁 Directory Structure

```
vram-system/
├── server/
│   ├── app.py                  # Flask server with REST API
│   ├── resource_manager.py     # Resource storage and management
│   ├── resources/              # Directory for stored resources
│   └── requirements.txt        # Python dependencies
├── m5client/
│   ├── vram_client.ino        # Main Arduino sketch
│   ├── memory_manager.h       # Dynamic memory management
│   ├── resource_cache.h       # Intelligent caching system
│   └── wifi_manager.h         # WiFi and HTTP communication
└── examples/
    ├── basic_usage.ino        # Simple usage example
    └── demo_resources/        # Sample resources for testing
```

## 🚀 Getting Started

### Server Setup

1. Install Python dependencies:
```bash
cd vram-system/server
pip install -r requirements.txt
```

2. Start the Flask server:
```bash
python app.py
```

The server will start on `http://0.0.0.0:5000` with demo resources pre-loaded.

### M5StickC Plus2 Setup

1. Install required Arduino libraries:
   - M5StickCPlus2
   - WiFi
   - HTTPClient
   - ArduinoJson
   - base64

2. Update WiFi credentials in `vram_client.ino`:
```cpp
const char* WIFI_SSID = "YOUR_WIFI_SSID";
const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";
const char* SERVER_URL = "http://192.168.1.100:5000";  // Your server IP
```

3. Upload the sketch to your M5StickC Plus2

## 📊 Features

### Memory Management
- **Dynamic Allocation**: Intelligent memory allocation with automatic cleanup
- **LRU Algorithm**: Least Recently Used with priority-based eviction
- **Memory Monitoring**: Real-time memory usage tracking
- **Automatic Cleanup**: Triggered at 90% memory usage, clears 30% of data

### Resource Caching
- **Intelligent Cache**: Priority-based caching with version control
- **Hit/Miss Tracking**: Performance monitoring and optimization
- **Resource Validation**: Automatic integrity checking with SHA256 hashes
- **Auto-Sync**: Periodic synchronization with server for updates

### Network Communication
- **Robust WiFi**: Automatic reconnection and error recovery
- **HTTP API**: RESTful communication with JSON responses
- **Compression**: Automatic gzip compression for large resources
- **Timeout Handling**: Configurable timeouts and retry mechanisms

## 🎮 Controls

- **Button A**: Cycle through display screens (Status/Memory/Cache/Network)
- **Button B**: Force memory cleanup
- **Power Button**: Print detailed system information to Serial

## 📺 Display Screens

1. **System Status**: Overall system health and statistics
2. **Memory Info**: Memory usage with visual progress bar
3. **Cache Info**: Cache hit ratio and cached resources
4. **Network Info**: WiFi connection and server status

## 🔧 API Endpoints

### Server API

- `GET /` - Health check
- `GET /api/resources` - List all resources
- `GET /api/resources/{id}` - Get specific resource
- `GET /api/resources/{id}/info` - Get resource metadata
- `GET /api/resources/{id}/version` - Check resource version
- `POST /api/resources` - Store new resource
- `DELETE /api/resources/{id}` - Delete resource
- `GET /api/stats` - Server statistics

### Resource Types

Resources are categorized by type and priority:
- **Critical**: System essential resources (priority 3)
- **Important**: Frequently used resources (priority 2)
- **Normal**: Standard resources (priority 1)
- **Cache**: Temporary/disposable resources (priority 0)

## 🔍 Memory Management Strategy

### Allocation Policy
1. **First Fit**: Find first available memory block
2. **Priority Check**: Higher priority resources get preference
3. **Cleanup Trigger**: Automatic cleanup at 90% usage threshold
4. **Emergency Mode**: Force cleanup if allocation fails

### Cleanup Algorithm
1. **Sort by Score**: Combine LRU age, access count, and priority
2. **Remove Candidates**: Start with lowest score resources
3. **Preserve Critical**: Never remove locked or critical resources
4. **Target 30%**: Remove enough to free ~30% of used memory

## 📈 Performance Targets

- **Response Time**: < 100ms for small resources (< 1KB)
- **Memory Efficiency**: > 90% utilization before cleanup
- **Cache Hit Ratio**: > 80% for frequently accessed resources
- **Network Reliability**: < 1% failure rate with auto-recovery
- **Resource Limit**: Support resources up to 1MB

## 🛠️ Configuration Options

### Memory Manager
```cpp
MemoryManager(size_t maxMem = 200 * 1024);  // Max memory pool
setCleanupThreshold(float percentage);       // Cleanup trigger point
```

### WiFi Manager
```cpp
setConnectionTimeout(uint32_t timeout);      // WiFi connection timeout
setRequestTimeout(uint32_t timeout);         // HTTP request timeout
setMaxRetries(uint8_t retries);             // Maximum retry attempts
```

### Resource Cache
```cpp
setResourcePriority(String id, uint8_t priority);  // Change resource priority
touchResource(String id);                          // Update access time
```

## 📝 Usage Examples

### Basic Resource Loading
```cpp
// Load a text resource
String resourceId = "my_text_file";
size_t size;
uint8_t* data = resourceCache->getResource(resourceId, size);

if (data) {
    // Use the resource data
    String content((char*)data, size);
    Serial.println("Content: " + content);
}
```

### JSON Resource Processing
```cpp
// Load and parse JSON resource
uint8_t* jsonData = resourceCache->getResource("config", size);
if (jsonData) {
    DynamicJsonDocument doc(1024);
    deserializeJson(doc, (char*)jsonData);
    
    String value = doc["setting"];
    int number = doc["value"];
}
```

### Memory Monitoring
```cpp
// Check memory status
if (memoryManager->isMemoryLow()) {
    Serial.println("Memory usage critical!");
    memoryManager->forceCleanup();
}

float usage = memoryManager->getMemoryUsage();
Serial.println("Memory usage: " + String(usage) + "%");
```

## 🔧 Troubleshooting

### Common Issues

1. **WiFi Connection Failed**
   - Check SSID and password
   - Ensure server is reachable
   - Verify network configuration

2. **Memory Full**
   - Reduce number of cached resources
   - Lower cleanup threshold
   - Increase memory pool size

3. **Resource Not Found**
   - Check server is running
   - Verify resource exists on server
   - Check network connectivity

4. **Cache Misses**
   - Resources may be evicted due to memory pressure
   - Increase resource priority
   - Increase memory pool size

### Debug Information

Enable verbose logging by adding to setup():
```cpp
Serial.begin(115200);
// Enable debug output for all components
```

Monitor system status through Serial output and display screens.

## 📋 Technical Specifications

- **Target Platform**: M5StickC Plus2 (ESP32)
- **Memory Pool**: 200KB default (configurable)
- **Network Protocol**: HTTP/1.1 with JSON
- **Compression**: gzip compression for storage
- **Security**: SHA256 integrity checking
- **Real-time**: 50ms main loop interval
- **Display**: 240x135 TFT with 4 information screens

## 🔄 System Workflow

1. **Initialization**: Setup memory manager, cache, and WiFi
2. **Resource Loading**: Fetch high-priority resources from server
3. **Main Loop**: Handle user input, monitor memory, sync with server
4. **Memory Management**: Automatic cleanup when threshold reached
5. **Display Updates**: Real-time status information
6. **Error Recovery**: Automatic reconnection and retry mechanisms

This VRAM system provides a robust foundation for M5StickC Plus2 applications that need access to large datasets while maintaining efficient memory usage and reliable network communication.