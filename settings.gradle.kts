rootProject.name = "app-native"

include("backend")
include("frontend")
include("backend:admin")
include("backend:components")
include("backend:components:core-component")
include("backend:components:domain-component")
include("backend:components:tenant-component")
include("backend:components:security-component")
include("backend:components:storage-component")
include("backend:components:bean-component")
include("backend:components:api-component")

include("backend:components:json-component")
include("backend:components:rest-apt-component")