# Distanbol
Distanbol generates a human-readable rendering of the Stanbol enrich-output.

Distanbol accepts two parameters as query parameters:

**URL:** The URL to the Stanbol output Json file.

**Confidence(Optional):** The minimum confidence threshold determined by Stanbol. The results will have a confidence of greater than this number. The default value is 0.7.

You can either:
 - Put your Stanbol json output URL as a query:
 
   `https://distanbol.acdh.oeaw.ac.at?URL={URL of your Stanbol output in json}&confidence={minimum confidence threshold}`
 - Or use the form on the [homepage](https://distanbol.acdh.oeaw.ac.at/).

### Examples with default Confidence(0.7):

 - [https://distanbol.acdh.oeaw.ac.at?URL=https://distanbol.acdh.oeaw.ac.at/example/example.json](https://distanbol.acdh.oeaw.ac.at?URL=https://distanbol.acdh.oeaw.ac.at/example/example.json)
 - [https://distanbol.acdh.oeaw.ac.at?URL=https://distanbol.acdh.oeaw.ac.at/example/example1.json](https://distanbol.acdh.oeaw.ac.at?URL=https://distanbol.acdh.oeaw.ac.at/example/example1.json)
 - [https://distanbol.acdh.oeaw.ac.at?URL=https://distanbol.acdh.oeaw.ac.at/example/example2.json](https://distanbol.acdh.oeaw.ac.at?URL=https://distanbol.acdh.oeaw.ac.at/example/example2.json)
 - [https://distanbol.acdh.oeaw.ac.at?URL=https://distanbol.acdh.oeaw.ac.at/example/example3.json](https://distanbol.acdh.oeaw.ac.at?URL=https://distanbol.acdh.oeaw.ac.at/example/example3.json)
 - [https://distanbol.acdh.oeaw.ac.at?URL=https://distanbol.acdh.oeaw.ac.at/example/example4.json](https://distanbol.acdh.oeaw.ac.at?URL=https://distanbol.acdh.oeaw.ac.at/example/example4.json)
 - [https://distanbol.acdh.oeaw.ac.at?URL=https://distanbol.acdh.oeaw.ac.at/example/example5.json](https://distanbol.acdh.oeaw.ac.at?URL=https://distanbol.acdh.oeaw.ac.at/example/example5.json)

### Examples with confidence as parameter(same example, different confidence):

 - [https://distanbol.acdh.oeaw.ac.at?URL=https://distanbol.acdh.oeaw.ac.at/example/example5.json&confidence=0.5](https://distanbol.acdh.oeaw.ac.at?URL=https://distanbol.acdh.oeaw.ac.at/example/example5.json&confidence=0.5)
 - [https://distanbol.acdh.oeaw.ac.at?URL=https://distanbol.acdh.oeaw.ac.at/example/example5.json&confidence=0.7](https://distanbol.acdh.oeaw.ac.at?URL=https://distanbol.acdh.oeaw.ac.at/example/example5.json&confidence=0.7)


## Licensing

All Distanbol code unless otherwise noted is licensed under the terms of the [MIT License](https://opensource.org/licenses/MIT).
