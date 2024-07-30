package com.github.omkumar2003;



class Zone {
    String name;
    String masterNameServer;
    String[] allNameServers;
    String adminEmail;

    // timing se related are in nano second 
    long refresh;
    long retry;
    long expire;
    long SOATTL;
    long NSTTL;// The TTL for NS records.
    long MinTTL;
    // 	Handler func(name string) ([]Set, error)
public void validate(){
    
}
}