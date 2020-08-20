## SearchableRestEntity

This lib generates search POST endpoint for Spring JPA entities.
All necessary code is being automatically generated: Service, Repository, Mapper, Controller. 

Build: `gradle build -x bootJar`

Usage:

```java
@Entity
@SearchableRestEntity(path='/playerInfo', useEntityAsDto=true)
public class Player{
    private String familyName;
    private String givenName;
    private Int age;
    private PlayerScore playerScore;
}

@Entity
public class PlayerScore{
    private Int games;
    private Int totalScore;
} 
```

Check it with POST request:
```bash
curl \
  -H 'Content-Type: application/json' \
  -X POST \
  -d '{"key": "givenName", "operation": "EQUALS", "value": "Mark"}' \
  http://yourhost/playerInfo/search?page=0&size=10&sort=givenName,ASC
```

Body of search request can contains a complex queries:
```json
[{
    "key": "givenName",
    "operation": "EQUALS",
    "value": "Mark",
    "and": [
        {
            "key": "playerScore.totalScore",
            "operation": "GREATER",
            "value": "120",
            "or":[
                {
                    "key": "playerScore.games",
                    "operation": "LESS",
                    "value": "5"
                }
            ]
        },
        {
            "key": "playerScore.age",
            "operation": "GREATER",
            "value": "23",
            "and":[
                {
                    "key": "playerScore.totalScore",
                    "operation": "LESS_EQUALS",
                    "value": "1200"
                },
                {
                    "key": "playerScore.games",
                    "operation": "LESS_EQUALS",
                    "value": "10"
                }
            ]
        }
    ]
}]
```
