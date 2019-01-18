## Document technique - MAC

### Introduction

Cette documentation concerne la partie MAC pour notre projet WNS. Dans le cadre du projet, nous avons choisit comme base de données elasticsearch. Notre projet étant un réseau social d'écrivain et ayant comme entité les livres, elasticsearch répondait parfaitement au besoin d'indexation et de recherches de texte.

Initiallement, nous avons commencé à implémenter tout le backend avec ElasticSearch. Nous nous sommes vite rendu compte qu'elasticsearch n'est pas propice aux utilisations courantes pour la gestion classique d'entité et des opérations de type CRUD sur des ressources commes les utilisateurs, les profils, les catégories etc.
En effet, la littérature sur le net l'explique bien. Dans un contexte similaire au notre, ElasticSearch doit s'utiliser avec une base de données complémentaire permettant de gérer les resources classiques. MySQL ou MongoDB pour n'en citer que deux.

Par conséquent, suite à la discussion avec Miguel et Nastaran, nous avons décidé d'utiliser MongoDB et ElasticSearch.MongoDB s'occupera d'habriter toutes les données qui sont relatives au réseau social (Utilisateurs, profils, followers). ElasticSearch lui s'occupera de gérer un seul type de document: les livres.

### Schéma de base de données

####- MongoDB

Notre base de données MongoDB manipulent les collections suivantes:

![image-20190118010340005](/Users/MentorReka/Documents/Professionnel/HEIG-VD/Modules/S5/PROJETTWEBMAC/MONGODB_MODEL.png)

Nous sommes partis sur un schéma de base de données simple et épurée tout en faisant des compromis. L'ensemble des informations sont accessibles en maximum deux lectures et celà sans sacrifier l'évolutivité du schéma.

##### User

Les **likes** d'un utilisateur correspond aux ids des livres (index dans ElasticSearch) qu'il a aimé. De cette façon, nous pouvons en deux accès récupérer la liste des livres qu'il a aimé :

- 1 accès à la base de données Users
- 1 accès à l'index dans ElasticSearch

La propriété plus spécifique comme **roles** correspond à notre implémentation dans Spring Security de l'authentification JWT. Celle-ci permet d'évoluer l'application facilement en terme de role également.

##### Notification

La collection notification a été pensé afin d'être évolutive et permettre non seulement de facilement ajouter des types de notification distincts mais également d'assurer l'évolutivité dans un contexte de scalabilité. En effet, une première approche naive aurait pu être d'utiliser la dénormalisation et embarqué cette collection dans la collection **user**. Mais comme mentionné précédement, ce type de document a volonté à être très présent et en grande quantité dans le cadre d'un réseau social. La limitation de 16Mb pour la dénormalisation peut clairement rapidement arrivé et donc bloqué l'évolutivité de l'application.

Par conséquent, nous avons utilisé la **normalisation** sur les attributs **sender** et **recipient**. Nous avons volontairement choisit que le **recipient** ne soit pas une liste de référence vers les users mais une seul référence. Une fois de plus, un choix doit être fait et la limitation de 16Mb est toujours présente. Nous avons préféré gagner en flexibilité et maintenir l'évolutivité quitte à sacrifier avec parcimonie la performance en lecture. Cette approche a comme conséquence en terme de performance l'écriture. Lorsqu'un auteur écrit un bouquin par exemple, l'application va créer X notifications où X correspond aux nombres de followers de l'auteur.

La propriété **type** permet de catégoriser facilement les notifications. 

##### Follower

Une des relations problématique est celle de la relation follower / following. En somme, nous avons deux choix:

- Normaliser les données dans la collection User afin de stocker un tableau appelé followers contenant la référence aux utilisateurs que nous suivons.
  L’approche est plus rapide que d’autres mais est limitée à 16 Mo. Donc, clairement, pas évolutive. Un avantage évident de cette approche réside dans le fait que toutes les informations suivantes sur un utilisateur peuvent être trouvées en lisant un seul document.
- La deuxième approche consiste à avoir une collection dédiée et utilisé la normalisation. Avec cette approche nous pouvons facilement retrouvé les followers d'un utilisateur mais également les followings d'un utilisateur. Pour ce choix, nous nous sommes inspiré de l'approche de mongodb-labs qui a écrit un article très pointue sur ce type de problématique décrite ici: https://github.com/mongodb-labs/socialite/blob/master/docs/graph.md

En plus de celà, nous avons ajouté un **CompoundIndex** dans cette collection pour les champs **from** et **to**. Voici la représentation de cet index:

```java
@Document(collection = "followers")
@CompoundIndexes({
        @CompoundIndex(name = "followers_from_and_to_index",
                unique = true,
                def = "{'from' : 1, 'to' : 1}")
})
```

Celà nous permet d'une part d'augmenter les performances en lecture et de plus garantir l'unicité d'un document avec un couple (from, to).

#### - ElasticSearch

ElasticSearch est dédié à l'indexation des bouquins. Nos auteurs soumettent des PDFs et nous avons donc du mettre en place tout un pipeline et un process pour l'extraction, l'indexation et la recherche.

Pour ce faire, nous avons créer un index **wns** sous elasticsearch avec un seul et unique type **books** qui contient les propriétés suivantes:

```java
                                     Type books

String id;
String authorId
String title
String postDescription
String[] tags
String bookContent
Date createdDate

```



ElasticSearch contient différentes propriétés qui sont en réalité des références vers des documents dans notre base MongoDB. La complexité de notre implémentation réside avant tout à maintenir et assurer une intégrité dans les entités manipulés entre les deux mondes MongoDB et ElasticSearch.

Un des exemples de cette relation est **authorId** qui est une référence à notre collection **Users** dans MongoDB.

Le champs **id** est un UUID4 généré par nos soins. Cet **id** est utilisé comme référence dans la collection **User** et plus précisement le champs **likes**.

Le champs **tags** correspond aux anottations permettant de taguer un bouquin et contient par exemple:

```java
tags = ["Software Development", "Agile", "Spring"]
```

L'attribut le plus périlleux et important de notre type est **bookContent** qui contient en réalité un base64 représentant le contenu d'un PDF.

Les mécanismes de traitements, indexations et recherches sont détaillés dans la suite du document.

### Implémentation

Pour l'implémentation de notre backend, nous avons choisit le framework Spring et plus particulièrement le projet Spring-Data-MongoDB et Spring-Data-ElasticSearch.

#### MongoDB

Pour MongoDB, nous avons implémenté le principe très répandu dans l'écosystème Spring des repositories. Voici donc les interface décrivant ces repositories et les méthodes que nous avons à disposition:

**FollowerRepository**:

```java
public interface FollowerRepository extends PagingAndSortingRepository<Followers, Serializable> {
    List<Followers> findByFrom(User u, Pageable pageable);
    List<Followers> findByTo(User u, Pageable pageable);
    Followers findByFromAndTo(User from, User to);
    List<Followers> findByFrom(User u);
    List<Followers> findAll();
}
```

**NotificationRepository**:

```java
public interface NotificationRepository extends PagingAndSortingRepository<Notification, Serializable> {
    List<Notification> findByRecipient(User recipient);
    List<Notification> findBySender(User user);
    List<Notification> findAll();
}
```

**UserRepository**:

```java
public interface UserRepository extends PagingAndSortingRepository<User, Serializable> {
    User findByEmail(String email);
    List<User> findAll(Pageable pageable)
    List<User> findAll();
}
```

#### ElasticSearch

Pour ElasticSearch, nous avons également utilisé le concept des Repositories pour les méthodes "classiques". Tout ce qui concerne la recherche a été implémenté avec l'API Java High Level REST Client.

**BookRepository**

```java
public interface BookRepository extends ElasticsearchRepository<Book, String> {
    List<Book> findAllByAuthorIdOrderByCreatedDateDesc(String authorId);
    List<Book> findAllByTagsLike(String[] tags);
    List<Book> findByIdIn(List<String> ids);
    List<Book> findByAuthorIdInOrderByCreatedDateDesc(List<String> ids);
    List<Book> findByAuthorIdIn(List<String> ids);
    Book findByAuthorId(String id);
}
```

**Traitement, indexation et recherche**

Comme décrit ultérieurement, les auteurs vont fournir des PDFs. Pour ce faire, nous avons du mettre en place un mécanisme permettant d'extraire tout le contenu d'un PDF et l'indexer dans ElasticSearch.

Après divers recherches, nous sommes tombés sur le plugin **mapper-attachment** qui aujourd'hui est devenu **ingest-attachment**. Le processus pour indexer un PDF est le suivant:

![image-20190118010340005](/Users/MentorReka/Documents/Professionnel/HEIG-VD/Modules/S5/PROJETTWEBMAC/pdf-to-elasticsearch.png)

Image source: https://qbox.io/

Pour ce faire, nous avons du installer en premier lieu le plugin **ingest-attachment**:

```bash
bin/plugin install ingest-attachment
```

Une fois le plugin installé (et configuré, voir pour la configuration: ), nous devons créer un pipeline d'execution dans ElasticSearch. (Nous avons réalisé l'ensemble de nos opérations avec Kibana).

Un pipeline est la définition d'une série de processus à exécuter dans le même ordre que leur déclaration. Un pipeline est composé de deux champs principaux: une description et une liste de processus: description est un champ spécial permettant de stocker une description utile de ce que fait le pipeline. Le paramètre processors définit une liste de processus à exécuter dans l'ordre.

La création de notre pipeline:

```bash
PUT _ingest/pipeline/attachment
{
  "description" : "Extract PDF attachment information",
  "processors" : [
    {
      "attachment" : {
        "field" : "bookContent"
      }
    }
  ]
}
```

Dans le block **processors**, nous avons spécifié que pour le processus **attachment**, nous allons extraire et traiter le champs **bookContent**. 

Maintenant que le pipeline est créer, à chaque indexation d'un type, nous pouvons mentionner l'exécution d'un pipeline pour le traitement. Pour ce faire, voici la requête et le payload à exécuter:

```
PUT wns/books/ID_UNIQUE_UUID_DU_TYPE?pipeline=attachment
{
    "bookContent": "base64 bookContent"
}
```

Le paramètre **?pipeline=attachment** est nécessaire est spécifie le nom du pipeline que nous avons définit précédement.

**La recherche**

Notre application permet d'effectuer la recherche d'un bouquin par rapport à 4 critères:

- Les tags (communément appelé les annotations)
- Le contenu du bouquin
- Le titre du bouquin
- La description (ou le résumé) du bouquin

La recherche a été implémenté avec l'API Java de ElasticSearch en effectuant des Full Text Queries. Voici un résumé de la fonctionnalitée de recherche booléenne:

```java
...
Map<String, String> hashmap = new HashMap<>();

if (searchQuery.getBookContent() != null) {
    hashmap.put("attachment.content", searchQuery.getBookContent());
}
if (searchQuery.getTags() != null) {
    hashmap.put("tags", Arrays.toString(searchQuery.getTags()));
}
if (searchQuery.getPostDescription() != null) {
    hashmap.put("postDescription", searchQuery.getPostDescription());
}
if (searchQuery.getTitle() != null) {
    hashmap.put("title", searchQuery.getTitle());
}

if (isSearchable) {
    BoolQueryBuilder query = QueryBuilders.boolQuery();
    try {
        System.out.println(hashmap);
        for (String key : hashmap.keySet()) {
            query.must(QueryBuilders.matchQuery(key, hashmap.get(key)));
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    System.out.println(query);
    NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
        .withQuery(query)
        .build();
    return elasticsearchTemplate.queryForList(nativeSearchQuery, Book.class);
} else {
    return null;
}
```

Un utilisateur peut rechercher un document à partir d'un critère ou l'ensemble des critères en spécifiant les contenus. En l'état, nous utilisant un matchQuery. Le **matchQuery** est une requête de correspondance de type boolean. Cela signifie que le texte fourni est analysé et que le processus d'analyse construit une requête booléenne à partir du texte fourni.

Nous avons testé plusieurs améliorations que nous n'avons pour le moment pas mis en production (car génère des faux positifs conséquents si elles sont appliqués à l'ensemble des critères). Entre autre une des requêtes les plus intéressantes était la recherche par **FuzzyTerm** qui lorsqu'elle s'applique uniquement au titre du bouquin ou aux tags est très intéressante.

La requête floue (FuzzyTerm) génère des termes correspondants qui se trouvent dans la distance d'édition maximale spécifiée dans le flou, puis vérifie le dictionnaire de termes pour déterminer lequel de ces termes générés existe réellement dans l'index. Les requêtes qui sont interrogez sur des propriétés de type texte, le flou est interprété comme une distance de levée (**Levenshtein Edit Distance**): nombre de caractère à modifier dans une chaîne pour la rendre identique à une autre.

#### Déploiement

Le déploiement sur Heroku a été compliqué. Le déploiement de notre API Spring en Java et de la base de données n'a pas été un problème. Le problème a été ElasticSearch !

Il existe différents providers qui proposent comme **bonsai**(https://devcenter.heroku.com/articles/bonsai) seulement le problème est le suivant:

- Ces providers proposent de l'hébergement ElasticSearch free mais avec une rétention des données de moins de 7 jours.
- Aucune possibilité d'ajouter un plugin et créer un pipeline d'exécution

Par conséquent, compte tenu du temps à disposition et malgré nos recherches, nous n'avons pas pu effectuer le déploiement ElasticSearch correctement. Nous avons informé Miguel durant la phase d'implémentation de cette problématique et avons tenté de trouver des solutions rapides autour de nous mais sans succès. 

## Conclusion

Ce projet nous a beaucoup appris tant sur le design de schema NoSQL que sur le processus de réflexion sur l'évolutivité et toute l'approche de faire des compromis. Nous avons clairement pu voir et appliqué la théorie appris en MAC dans la pratique. ElasticSearch est une technologie géniale avec des débouchés incroyable en terme de recherche. La prise en main de cette technologie nécessite un certains temps que ce soit d'adaptation pour la gestion et visualisation (kibana) mais aussi sur les concepts profonds de la recherche.

Nous aurions aimés pouvoir pousser nos recherches et réflexions sur l'utilisation d'ElasticSearch entre autre  

**Sources:**

https://qbox.io/blog/powerful-pdf-search-elasticsearch-mapper-attachment

https://qbox.io/blog/index-attachments-files-elasticsearch-mapper

https://www.mongodb.com/blog/post/tracking-twitter-followers-with-mongodb

https://github.com/mongodb-labs/socialite/blob/master/docs/graph.md

https://blog.ippon.fr/2017/10/12/preuve-dauthentification-avec-jwt/