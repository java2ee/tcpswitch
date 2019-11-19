# TcpSwitch  - программный кластер

TcpSwitch - программный кластер предназначен для переключения поставщиков и потребителей между кластерами Kafka,
размещенными в разных ЦОД-ах (*георезервирование*), но может быть использован и для переключения пользователей
между дублирующими копиями любых других ресуров, доступных по TCP/IP протоколу.

Программный кластер реализован на основе продукта "Avalanche - application framework for Java".

## Элементы конфигурации кластера

### [Tunnel - ru.transset.tcp.Tunnel](tcpswitch/src/main/java/ru/transset/tcp/Tunnel.java)

Tunnel - описывает перенапраление TCP/IP пакетов с локального порта на удалённый хост.

### [TunnelFunction - ru.transset.tcp.TunnelFunction](tcpswitch/src/main/java/ru/transset/tcp/TunnelFunction.java)

TunnelFunction - включает или отключает группу туннелей.

### TCPSwitch - ru.transset.app.tcpswitch.TCPSwitch

TCPSwitch - включает или отключает одну из локальных групп туннелей. Одномоментно может быть
активна только одна группа туннелей или не одной. 

### TCPNode - ru.transset.app.tcpswitch.TCPNode

TCPNode - узел управления группой, включает или отключает одну и ту же группу туннелей на множестве узлов кластера.

### TCPManager - ru.transset.app.tcpswitch.TCPManager

TCPManager - управляет множеством узлов управления различными группами

## Элементы мониторинга состояния кластеров kafka и других узлов кластера TcpSwitch

### JmxClient - ru.transset.app.jmx.JmxClient

JmxClient - контролирует метрики узла кластера Kafak

### JmxObject - ru.transset.app.jmx.JmxObject

JmxObject - описывает контролируемые метрики узла кластера Kafka

### KafkaBrokerStatus - ru.transset.kafka.jmx.threshold.KafkaBrokerStatus

KafkaBrokerStatus - контроль состояния метрики "kafka.server:type=KafkaServer,name=BrokerState" узла кластера Kafka

### JmxKafkaCluster - ru.transset.kafka.jmx.JmxKafkaCluster

JmxKafkaCluster - контролирует состояние кластера Kafka

### JmxKafka - ru.transset.kafka.jmx.JmxKafka

JmxKafka - управляет состоянием геораспределенных кластеров Kafka на основании мониторинга их состояния
