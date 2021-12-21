# opekepe-metis

OPEKEPE satellite and aerial photography image validation tool

## Running in Docker

The following instructions use Docker to install Metis in using the Metis Docker Image on a single local machine.

The system is composed of a RabbitMQ server and the Metis Server instance.

The `yaml` snippet bellow can be used to deploy the application directly to a host running docker or a docker swarm
cluster.

You need to define the following:

+ A network for the services to run in (e.g., `overlay network`)
+ Single Volume for storing RabbitMQ data
+ RabbitMQ username and password
+ Single Volume for storing report results
+ Volume for getting access to the original satellite images

````yaml
version: '3.4'
services:

  rabbitmq:
    image: rabbitmq:3.9.11-management-alpine
    networks:
      - metis_broker_net
    volumes:
      - RabbitDataVolume:/var/lib/rabbitmq/
    environment:
      - RABBITMQ_HIPE_COMPILE=1
      - RABBITMQ_DEFAULT_USER=rabbituser
      - RABBITMQ_DEFAULT_PASS=rabbitpassword

  server:
    depends_on:
      - rabbitmq
    image: qopbot/metis-server:0.3
    networks:
      - metis_broker_net
    volumes:
      - MetisReportsVolume:/home/metis/reports/
      - /some/samba/share/mount:/home/metis/images/
    environment:
      SPRING_PROFILES_ACTIVE: docr
      RABBIT_HOST: rabbitmq
      RABBIT_PORT: 5672
      RABBIT_USER: rabbituser
      RABBIT_PASS: rabbitpassword
    ports:
      - "8081:8080"
networks:
  metis_broker_net:
volumes:
  RabbitDataVolume:
  MetisReportsVolume:

````

#### [Μήτις / Metis](https://en.wikipedia.org/wiki/Metis_(mythology))

Metis in ancient Greek religion, was a mythical goddess, an Oceanid nymph belonging to the second generation of Titans.
The Greek word metis meant a quality that combined wisdom and cunning. This quality was considered to be highly
admirable, the hero Odysseus being the embodiment of it. In the Classical era, metis was regarded by Athenians as one of
the notable characteristics of the Athenian character.
