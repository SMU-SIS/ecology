# Ecology

## Synopsis

Ecology is an open source Android library used to catalyze the community of developers working in the field of multi-device app development. Using Ecology, developers can easily connect multiple devices to each other, send & listen to events locally as well as from other connected devices and also sync data across the connected devices.

## Setup

Entry point to Ecology is through a Connector. Currently, this library has one connector - Bluetooth Connector. Any custom connector can be created and used to establish a connection with Ecology.

### Bluetooth Connector

This inbuilt connector follows a client-server model. One device in the Ecology must act as the server and rest of the devices will act as clients. Server device can use the Bluetooth Server Connector to connect to the ecology while the client devices can use the Bluetooth Client Connector for the connection.

### Ecology Connection

Below is how a server device establishes a connection:

`Ecology ecology = EcologyCreator.connect(new BluetoothServerConnector(), this, "Phone", true, getApplication());`

A client device can establish a connection as show below:

`Ecology ecology = EcologyCreator.connect(new BluetoothClientConnector(), this, "Watch", false, getApplication());`


## Code Samples

## Dependencies

- [Apache MINA Core](https://mvnrepository.com/artifact/org.apache.mina/mina-core/3.0.0-M2)

## Installation

## Documentation

## License
