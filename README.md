# Peripleo 2
[![Project Stats](https://www.openhub.net/p/peripleo2/widgets/project_thin_badge.gif)](https://www.openhub.net/p/peripleo2)

Home of Peripleo v.2 - a search frontend to [Pelagios](http://commons.pelagios.org/). Track
our progress on [Waffle.io](http://waffle.io/pelagios/peripleo2).

## Prerequisites

* Java 8 JDK
* [Play Framework 2.5.12](https://www.playframework.com/download)
* [ElasticSearch 2.4.4](https://www.elastic.co/downloads/past-releases/elasticsearch-2-4-4)

## Installation

* Clone this repository
* Create a copy of the file `conf/application.conf.template` and name it `conf/application.conf`.
  Make any environment-specific changes there. (For the most part, the defaults should be fine.)
* Deploy the query scripts to ElasticSearch by copying the files in `conf/es-scripts/`
  to the `config/scripts` folder on every data node in your ES cluster.
* Type `activator run` to start the application in development mode.
* Point your browser to [http://localhost:9000](http://localhost:9000).
* Peripleo automatically creates a single admin user with username 'admin' and password 'admin'.
  Be sure to remove this user (or at least change the password) for production use!
* To generate an Eclipse project, type `activator eclipse`.

## License

Peripleo 2.0 is licensed under the terms of the
[Apache 2.0 license](https://github.com/pelagios/peripleo2/blob/master/LICENSE).
