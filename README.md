## SearchableRestEntity

This lib generates search controller for Spring JPA entities.

Build: `gradle build -x bootJar`

Usage:

```
@Entity
@SearchableRestEntity('/playerInfo')
public Player{
    private String familyName;
    private String givenName;
} 
```

