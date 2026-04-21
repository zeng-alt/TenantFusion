rootProject.name = "app-native"

include("backend")
include("frontend")
include("backend:admin")
include("backend:components")
include("backend:components:core-component")
findProject(":backend:components:core-component")?.name = "core-component"
include("backend:components:domain-component")
findProject(":backend:components:domain-component")?.name = "domain-component"
include("backend:components:tenant-component")
findProject(":backend:components:tenant-component")?.name = "tenant-component"
include("backend:components:security-component")
findProject(":backend:components:security-component")?.name = "security-component"
include("backend:components:storage-component")
findProject(":backend:components:storage-component")?.name = "storage-component"
