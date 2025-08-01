#include "wifi_manager.h"

WiFiManager::WiFiManager(const String& ssid, const String& password, const String& serverUrl) :
    ssid(ssid),
    password(password), 
    serverUrl(serverUrl),
    status(DISCONNECTED),
    lastConnectionAttempt(0),
    connectionTimeout(10000),
    requestTimeout(5000),
    maxRetries(3) {
}

WiFiManager::~WiFiManager() {
    disconnect();
}

bool WiFiManager::begin() {
    Serial.println("WIFI: Starting WiFi connection...");
    return connectToWiFi();
}

bool WiFiManager::connectToWiFi() {
    if (status == CONNECTED && WiFi.status() == WL_CONNECTED) {
        return true;
    }
    
    status = CONNECTING;
    lastConnectionAttempt = millis();
    
    WiFi.begin(ssid.c_str(), password.c_str());
    
    uint32_t startTime = millis();
    while (WiFi.status() != WL_CONNECTED && millis() - startTime < connectionTimeout) {
        delay(500);
        Serial.print(".");
    }
    
    if (WiFi.status() == WL_CONNECTED) {
        status = CONNECTED;
        Serial.println();
        Serial.println("WIFI: Connected successfully");
        Serial.println("WIFI: IP address: " + WiFi.localIP().toString());
        return true;
    } else {
        status = CONNECTION_FAILED;
        Serial.println();
        Serial.println("WIFI: Connection failed");
        handleConnectionFailure();
        return false;
    }
}

void WiFiManager::disconnect() {
    WiFi.disconnect();
    status = DISCONNECTED;
    Serial.println("WIFI: Disconnected");
}

bool WiFiManager::isConnected() {
    return (status == CONNECTED && WiFi.status() == WL_CONNECTED);
}

void WiFiManager::handleConnectionFailure() {
    Serial.println("WIFI: Connection failed, will retry...");
    // Could implement exponential backoff here
}

ServerResponse WiFiManager::getResource(const String& resourceId) {
    String endpoint = "/api/resources/" + resourceId;
    return makeRequest(endpoint);
}

ServerResponse WiFiManager::getResourceInfo(const String& resourceId) {
    String endpoint = "/api/resources/" + resourceId + "/info";
    return makeRequest(endpoint);
}

ServerResponse WiFiManager::checkResourceVersion(const String& resourceId) {
    String endpoint = "/api/resources/" + resourceId + "/version";
    return makeRequest(endpoint);
}

ServerResponse WiFiManager::listResources(const String& type) {
    String endpoint = "/api/resources";
    if (type.length() > 0) {
        endpoint += "?type=" + type;
    }
    return makeRequest(endpoint);
}

ServerResponse WiFiManager::getServerStats() {
    return makeRequest("/api/stats");
}

ServerResponse WiFiManager::makeRequest(const String& endpoint, const String& method, const String& payload) {
    ServerResponse response;
    response.success = false;
    response.httpCode = 0;
    response.data = "";
    response.error = "";
    
    if (!isConnected()) {
        if (!checkAndReconnect()) {
            response.error = "Not connected to WiFi";
            return response;
        }
    }
    
    HTTPClient http;
    String url = serverUrl + endpoint;
    
    Serial.println("HTTP: " + method + " " + url);
    
    http.begin(url);
    http.setTimeout(requestTimeout);
    http.addHeader("Content-Type", "application/json");
    
    int httpCode;
    if (method == "GET") {
        httpCode = http.GET();
    } else if (method == "POST") {
        httpCode = http.POST(payload);
    } else if (method == "DELETE") {
        httpCode = http.sendRequest("DELETE");
    } else {
        response.error = "Unsupported HTTP method";
        http.end();
        return response;
    }
    
    response.httpCode = httpCode;
    
    if (httpCode > 0) {
        response.data = http.getString();
        
        if (httpCode == HTTP_CODE_OK) {
            response.success = true;
            Serial.println("HTTP: Request successful (" + String(httpCode) + ")");
        } else {
            response.error = "HTTP error: " + String(httpCode);
            Serial.println("HTTP: Request failed with code " + String(httpCode));
        }
    } else {
        response.error = "HTTP request failed: " + String(httpCode);
        Serial.println("HTTP: Request failed: " + String(httpCode));
    }
    
    http.end();
    return response;
}

void WiFiManager::printNetworkInfo() {
    Serial.println("=== Network Information ===");
    Serial.println("WiFi Status: " + String(isConnected() ? "Connected" : "Disconnected"));
    
    if (isConnected()) {
        Serial.println("SSID: " + WiFi.SSID());
        Serial.println("IP Address: " + WiFi.localIP().toString());
        Serial.println("Gateway: " + WiFi.gatewayIP().toString());
        Serial.println("Subnet: " + WiFi.subnetMask().toString());
        Serial.println("DNS: " + WiFi.dnsIP().toString());
        Serial.println("Signal Strength: " + String(WiFi.RSSI()) + " dBm");
        Serial.println("MAC Address: " + WiFi.macAddress());
    }
    
    Serial.println("Server URL: " + serverUrl);
    Serial.println("Connection Timeout: " + String(connectionTimeout) + " ms");
    Serial.println("Request Timeout: " + String(requestTimeout) + " ms");
    Serial.println("Max Retries: " + String(maxRetries));
    Serial.println("===========================");
}

int WiFiManager::getSignalStrength() {
    return WiFi.RSSI();
}

String WiFiManager::getLocalIP() {
    return WiFi.localIP().toString();
}

bool WiFiManager::checkAndReconnect() {
    if (isConnected()) {
        return true;
    }
    
    // Don't try to reconnect too frequently
    uint32_t currentTime = millis();
    if (currentTime - lastConnectionAttempt < 5000) { // Wait at least 5 seconds
        return false;
    }
    
    Serial.println("WIFI: Attempting to reconnect...");
    return connectToWiFi();
}

void WiFiManager::setAutoReconnect(bool enable) {
    WiFi.setAutoReconnect(enable);
}