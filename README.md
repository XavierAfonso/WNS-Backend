# WNS-Backend


### Installation

Download the latest Elasticsearch version: https://www.elastic.co/webinars/getting-started-elasticsearch?elektra=home&storm=sub1
Download and install Kibana for visulizing Elasticsearch things from a web app: https://codingexplained.com/dev-ops/mac/installing-kibana-for-elasticsearch-on-os-x

Instead of using directly the Spring Security, try to implement it ourself : https://blog.ippon.fr/2017/10/12/preuve-dauthentification-avec-jwt/


Initiallement, nous avons commencé à implémenter tout le backend avec ElasticSearch. Nous nous sommes vite rendu compte qu'elasticsearch n'est pas propice 
aux utilisations courantes pour la gestion et des opérations de type CRUD sur des ressources classiques commes les utilisateurs, les profils, les catégories etc.
En effet, la littérature sur le net l'explique bien. ElasticSearch doit s'utiliser avec une base de données permettant de gérer les resources classiques comme MySQL
ou MongoDB pour n'en citer que deux.

Par conséquent, suite à la discussion avec Miguel et Nastaran, nous avons décidé d'implémenter les deux bases de données MongoDB et ElasticSearch.
MongoDB s'occupera d'habriter toutes les données qui sont relatives au réseau social (Utilisateurs, profils, followers). ElasticSearch lui s'occupera de
gérer un seul type de document: les livres.

Il existe des plugins pour importer et indexer les PDFs dans Elasticsearch: 

- https://qbox.io/blog/powerful-pdf-search-elasticsearch-mapper-attachment
- https://github.com/elastic/elasticsearch-mapper-attachments


### How to

Install MongoDB and Elasticsearch.

#### MongoDB

Launch mongo daemon and connect to it through mongo client.

Create the datbase by running this command:

```bash
use wns
```

Create a user for MongoDB:

```bash
db.createUser( { user: "wns",
                 pwd: "wns",
                 roles: [ { role: "clusterAdmin", db: "admin" },
                          { role: "readAnyDatabase", db: "admin" },
                          "readWrite"] },
               { w: "majority" , wtimeout: 5000 } )
```

#### Elasticsearch

Install and launch elasticsearch. Install also Kibana for visualizing data and using the dev tools for communicating easily with the REST Api.

For creating a elasticsearch index, open the dev tool in Kibana then run:

```bash
PUT wns
```

### Continuous deployment

We will follow this documentation for configuring our deployment of elasticsearch: https://devcenter.heroku.com/articles/bonsai

- For MongoDB we will store the database in MongoDB Atlas
- For ElasticSearch 



### Database Design

Here are some usefull articles concerning the design of our DB: 

- https://www.mongodb.com/blog/post/tracking-twitter-followers-with-mongodb

A problematic thing is the followers/following relation. Basicaly, we have two choice:

- Normalizating the datas in User collection in order to store an array called followers which contains the reference to the users we follow.
  The approach is faster than others but is limited to 16MB .. So clearly not scalable. A clear advantage of this approach is that all follower/following information for a user can be found by reading a single document.
- The second approach is to have two collection in addition to the User collection. The collection ``followers`` and ``following``
  which contains two refenrece to ``follower`` and the ``followed``. We inspired from this approach described here: https://github.com/mongodb-labs/socialite/blob/master/docs/graph.md

In the followers table we have added a Compound Unique Index on both from and to fields. In order to ensure a quick retrieve
but also that we have only one record with the same from and same to.


### ElasticSearch

Plugin for PDF https://qbox.io/blog/powerful-pdf-search-elasticsearch-mapper-attachment