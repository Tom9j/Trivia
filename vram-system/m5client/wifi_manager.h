#ifndef WIFI_MANAGER_H
#define WIFI_MANAGER_H

#include <Arduino.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>

enum ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    CONNECTION_FAILED,
    REQUEST_TIMEOUT
};

struct ServerResponse {
    bool success;
    int httpCode;
    String data;
    String error;
};

class WiFiManager {
private:
    String ssid;
    String password;
    String serverUrl;
    ConnectionStatus status;
    uint32_t lastConnectionAttempt;
    uint32_t connectionTimeout;
    uint32_t requestTimeout;
    uint8_t maxRetries;
    
    // Connection management
    bool connectToWiFi();
    void handleConnectionFailure();
    
public:
    WiFiManager(const String& ssid, const String& password, const String& serverUrl);
    ~WiFiManager();
    
    // Connection control
    bool begin();
    void disconnect();
    bool isConnected();
    ConnectionStatus getStatus() const { return status; }
    
    // Server communication
    ServerResponse getResource(const String& resourceId);
    ServerResponse getResourceInfo(const String& resourceId);
    ServerResponse checkResourceVersion(const String& resourceId);
    ServerResponse listResources(const String& type = "");
    ServerResponse getServerStats();
    
    // HTTP request helpers
    ServerResponse makeRequest(const String& endpoint, const String& method = "GET", 
                              const String& payload = "");
    
    // Connection settings
    void setConnectionTimeout(uint32_t timeout) { connectionTimeout = timeout; }
    void setRequestTimeout(uint32_t timeout) { requestTimeout = timeout; }
    void setMaxRetries(uint8_t retries) { maxRetries = retries; }
    
    // Network diagnostics
    void printNetworkInfo();
    int getSignalStrength();
    String getLocalIP();
    
    // Automatic reconnection
    bool checkAndReconnect();
    void setAutoReconnect(bool enable);
};

#endif