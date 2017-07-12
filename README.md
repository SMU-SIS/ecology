# Ecology

## Synopsis

Ecology is an open source Android library used to catalyze the community of developers working in the field of multi-device app development. Using Ecology, developers can easily connect multiple devices to each other, send & listen to events locally as well as from other connected devices and also sync data across the connected devices.

## Usage

Entry point to Ecology is through a Connector. Currently, this library has one connector - Bluetooth Connector. Any custom connector can be created and used to establish a connection with Ecology.

### Bluetooth Connector

This inbuilt connector follows a client-server model. One device in the Ecology must act as the server and rest of the devices will act as clients. Server device can use the Bluetooth Server Connector to connect to the ecology while the client devices can use the Bluetooth Client Connector for the connection. **To use this connector, the client devices must be already paired with the server device via Bluetooth.**

### Ecology Creator

This class is used to create the Ecology instance. An ecology instance is created only once per application. This class helps to connect and disconnect from the ecology

### Ecology Connection

Below is how a server device establishes a connection:

```java
Ecology ecology = EcologyCreator.connect(new BluetoothServerConnector(), this, "Phone", 
                true, getApplication());
```

A client device can establish a connection as shown below:

```java
Ecology ecology = EcologyCreator.connect(new BluetoothClientConnector(), this, "Watch",
                false, getApplication());
```

### Room

A Room represents an ecology space dedicated to a set of similar functionalities. A room can be created as follows:

```java
Room room = ecology.getRoom(ROOM_NAME);
```

### Event Broadcaster

Event broadcaster is used to publish as well as receive events from the connected devices or locally in the ecology. Below is how we get event broadcaster part of the created room:

```java
// Get event broadcaster part of the created room
EventBroadcaster eventBroadcaster = room.getEventBroadcaster(this);
```

### Event Receiver

Events, both locally and to other connected devices in Ecology, are transmitted to Event Receivers. An Event receiver must implement the EventReceiver interface and define a handleEvent method:

```java
public interface EventReceiver {
    /**
     * Handle the events.
     *
     * @param eventType the type of the event
     * @param eventData the data of the event
     */
    public void handleEvent(String eventType, List<Object> eventData);
}
```

### Usage Of Event Broadcaster

These methods are thread safe and can be called from any thread. These method do not establish any connection across the devices. The connections are managed by the ecology.

- #### Event Subscription

To suscribe to an event of the type "eventType", one must call the subscribe method with the event type (a String) and an EventReceiver instance as arguments.

```java
eventBroadcaster.suscribe("eventType", eventReceiver)
```
Once an event type is subscribed, each time one of the event of this type is published (see [Publishing An Event](#publishing-an-event)), the handleEvent method of the event receiver is called with the event type and event data as arguments.

- #### Event Un-Subscription

If an event receiver is unsubscribed to an event type, it won't receive the events of this type anymore.

```java
eventBroadcaster.unsubscribe("eventType", eventReceiver)
```

- #### Publishing An Event

When an event is published, it will be distributed to all the event receivers(local as well as remote) that has subscribed to this event type.

```java
eventBroadcaster.publish("eventType", data);
```

## Classes Description

## Dependencies

- [Apache MINA Core](https://mvnrepository.com/artifact/org.apache.mina/mina-core/3.0.0-M2)

## Installation

## Documentation

## License
