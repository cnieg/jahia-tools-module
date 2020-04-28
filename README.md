# jahia-tools-module

Jahia module offering new administration endpoints (site import, site deletion ...)

Avoid installing this module on a production instance, as this exposes endpoints which can be dangerous if they are not properly secured.

## Usage

Import site
```
curl --user <root>:<pwd> -F site=portail -F file=@export.zip http://localhost:8080/modules/api/jahia-tools/sites
```

Delete site
```
curl --user <root>:<pwd> -X DELETE http://localhost:8080/modules/api/jahia-tools/sites/<siteKey>
```
