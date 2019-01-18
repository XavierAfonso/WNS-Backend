## Document technique - MAC

### Introduction

Cette documentation concerne la partie TWEB pour notre projet WNS. Dans le cadre du projet, nous avons choisit d'implémenté notre frontend en JavaScript avec le framework ReactJS accompagné d'un Backend Java et du framework Spring.

### Backend

Le Backend a été réalisé avec le framework Spring. Le backend a les roles suivants:

- Mettre à disposition les endpoints permettant de manipuler les données hébergés dans MongoDB et ElasticSearch
- Sécuriser l'accès aux ressources avec JWT

L'API du Backend a été entièrement réalisé avec le framework Spring. La sécurité a été utilisé avec Spring Security en suivant les recommendations de la communauté Spring. Nous avons été **surpris** et **choqué** de comparer la mise en place de la pile JWT dans l'ecosystème Spring et JavaScript.

En effet, pour avoir une intégration propre avec Spring Security, nous avons du implémenté toute une logique et une dizaines de classe. Pour ce faire, nous nous sommes principalement inspiré de la documentation du framework Spring et de cet article https://blog.ippon.fr/2017/10/12/preuve-dauthentification-avec-jwt/

#### JWT avec Spring Security

La configuration de notre API se fait dans notre classe **WebSecurityConfig** qui contient:

```java
protected void configure(HttpSecurity http) throws Exception {
    // disable caching
    http.headers().cacheControl();

    http.csrf().disable() // disable csrf for our requests.
        .authorizeRequests()
        .antMatchers("/").permitAll()
        .antMatchers(HttpMethod.POST,"/users/signin").permitAll()
        .antMatchers(HttpMethod.POST,"/users/signup").permitAll()
        .antMatchers(HttpMethod.GET, "/books/{\\d+}/pdf").permitAll()
        .anyRequest().authenticated()
        .and()
        // We filter the api/login requests
        .addFilterBefore(new JWTLoginFilter("/users/signin", authenticationManager()), UsernamePasswordAuthenticationFilter.class)
        // And filter other requests to check the presence of JWT in header
        .addFilterBefore(new JWTAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
}
```



Trois endpoints ne sont pas sécurisés dans notre application: l'authentication, l'enregistrement et l'accès à un PDF.

#### API Controlleurs

Notre backend dispose de trois Controlleurs gérant l'ensemble des ressources de notre application que sont:

- BookController
- NotificationController
- UserController

Nous avons conçu l'API en essayant de respécter les standards de développement en terme de développement d'API que ce soit dans les codes de retours ou le format des endpoints. Nous avons particulièrement fait attention à ce que les endpoints qui pourrait retourner un grand nombre de données soient paginés.

L'API a été conçu de façon à faciliter l'évolution de celle-ci. Que ce soit via la séparation entre la recherche et le traitement des données via les Respositories et les Services ou encore avec les DTO et les annotations.

Vous trouverez dans le projet notre export en JSON des requêtes Postman.

#### Déploiement

Le déploiement sur Heroku a été compliqué. Le déploiement de notre API Spring en Java et de la base de données n'a pas été un problème. Le problème a été ElasticSearch !

Il existe différents providers qui proposent comme **bonsai** seulement le problème est le suivant:

- Ces providers proposent de l'hébergement ElasticSearch free mais avec une rétention des données de moins de 7 jours.
- Aucune possibilité d'ajouter un plugin et créer un pipeline d'exécution

Par conséquent, compte tenu du temps à disposition et malgré nos recherches, nous n'avons pas pu effectuer le déploiement ElasticSearch correctement. Nous avons informé Miguel durant la phase d'implémentation de cette problématique et avons tenté de trouver des solutions rapides autour de nous mais sans succès. 

### Installation

Pour l'installation en locale du projet, vous devez:

- Installer MongoDB et créer une base de données **wns** avec un user **wns**

- Installer ElasticSearch (et Kibana si vous voulez executer facilement vos requêtes)

- Installer le plugin ElasticSearch et créer le pipeline (voir documentation README_MAC.pdf)


**Sources:**

https://qbox.io/blog/powerful-pdf-search-elasticsearch-mapper-attachment

https://qbox.io/blog/index-attachments-files-elasticsearch-mapper

https://www.mongodb.com/blog/post/tracking-twitter-followers-with-mongodb

https://github.com/mongodb-labs/socialite/blob/master/docs/graph.md

https://blog.ippon.fr/2017/10/12/preuve-dauthentification-avec-jwt/