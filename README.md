# OA_RetrieveDeclarations
---

# Technical Implementation
To retrieve an array of declarations, I utilized the correponding `PSI` methods. 

My JSON contains the declarations of function and classes within the provided kotlin file. I omitted the `property` and `variable` declarations, because they would add unnecessary overhead and do not contain much information. However, my architecture allows to extend the app easily. The initial example contained only function declaration, so I decided my class declaration should include the following fields:

* `type`
* `name`
* `properties` (this is instead of parameters list in function declaration. I retrieve all the properties within the class, including those in the *primary constructor*.
* `body`

**Nested hierarchy**

I implemented a nested hierarchy, as it was in the example. If there are no nested declarations, the field `declarations` will be omitted and not presented.

# How to run
1. `git clone git@github.com:Valerii3/OA_RetrieveDeclarations.git`
2. Open the `cli_tool` folder within the *IDE*
3. Add programm arguments to the run configuration. You can choose any `kt file` and pass the abolute path. Run the program.

**Using Gradle**

1. `./gradlew run --args="absolute path"`

# Regards
Thank you JetBrains for the opportunity to become a part of **Fleet** Team during the summer internship.
