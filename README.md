# TcpSwitch  - программный кластер

TcpSwitch - программный кластер предназначен для переключения поставщиков и потребителей между кластерами Kafka,
размещенными в разных ЦОД-ах (*георезервирование*), но может быть использован и для переключения пользователей
между дублирующими копиями любых других ресуров, доступных по TCP/IP протоколу.

Программный кластер реализован на основе продукта "Avalanche - application framework for Java".


## 1. Элементы конфигурации кластера

### [Tunnel - ru.transset.tcp.Tunnel](tcpswitch/src/main/java/ru/transset/tcp/Tunnel.java)

Tunnel - описывает перенапраление TCP/IP пакетов с локального порта на удалённый хост.

### TunnelKafka - ru.transset.tcp.TunnelKafka

TunnelKafka - описывает перенапраление TCP/IP пакетов с локального порта на узел кластера Kafka (протокол Kafka).

### [TunnelFunction - ru.transset.tcp.TunnelFunction](tcpswitch/src/main/java/ru/transset/tcp/TunnelFunction.java)

TunnelFunction - включает или отключает группу туннелей.

### TCPSwitch - ru.transset.app.tcpswitch.TCPSwitch

TCPSwitch - включает или отключает одну из локальных групп туннелей. Одномоментно может быть
активна только одна группа туннелей или не одной. 

### TCPNode - ru.transset.app.tcpswitch.TCPNode

TCPNode - узел управления группой, включает или отключает одну и ту же группу туннелей на множестве узлов кластера.

### TCPManager - ru.transset.app.tcpswitch.TCPManager

TCPManager - управляет множеством узлов управления различными группами


## 2. Элементы мониторинга состояния кластеров kafka и других узлов кластера TcpSwitch

### JmxClient - ru.transset.app.jmx.JmxClient

JmxClient - контролирует метрики узла кластера Kafka

### JmxObject - ru.transset.app.jmx.JmxObject

JmxObject - описывает контролируемые метрики узла кластера Kafka

### KafkaBrokerStatus - ru.transset.kafka.jmx.threshold.KafkaBrokerStatus

KafkaBrokerStatus - контроль состояния метрики "kafka.server:type=KafkaServer,name=BrokerState" узла кластера Kafka

### JmxKafkaCluster - ru.transset.kafka.jmx.JmxKafkaCluster

JmxKafkaCluster - контролирует состояние кластера Kafka

### JmxKafka - ru.transset.kafka.jmx.JmxKafka

JmxKafka - управляет геораспределенными кластерами Kafka на основании мониторинга их состояния


## 3. Конфигурация кластерного приложения

Пример конфирурации узла кластерного приложения TcpSwitch - [avalanche-tcpswitch-config.xml](conf/avalanche-tcpswitch-config.xml)


## 4. Мониторинг состояния узлов кластерного приложения TcpSwitch

Мониторинг состояния узлов кластерного приложения [TcpSwitch](tcpswitch/JMX-TcpSwitch.jpg) с помощью утилиты *JConsole*. В примере
отображено состояния вызова функции *isMain()* удаленного экземпляра класса *JmxKafka*, вызываемого через интерфейс *http*
(имена объектов определы в конфигурационном файле). Функция *isMain()* вызывается для контроля согласованного состояния всех узлов
кластера TcpSwitch. Рассогласование состояний узлов кластера TcpSwitch может возникнуть при длительном разрыве связи между двумя
ЦОД-ами. При обнаружении рассогласованного состояния узлы кластера TcpSwitch автоматически его синхронизируют.

Проконтролировать можно и вызовы методов локальных *функций* (см. парку *function*)).


