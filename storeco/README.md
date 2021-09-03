# StoReCo 

Enhanced Stand-off TEI Annotation with StoReCo: A generic approach with the use of RDF.

This tool is still under development. Changes are possible at any time.

## Installation

Download from https://github.com/KardungLa/StoReCo .

## Usage

### How to use StoReCo
<pre>
java -jar storeco-0.2.0-SNAPSHOT-standalone.jar
</pre>

<pre>
StoReCo

  Usage: storeco [options]

  Options:
  -h, --help
  -c, --config PATH    EDN file to read config options from
  -i, --input INPUT
  -o, --output OUTPUT
  -f, --format FORMAT  (turtle)
  -r, --root-ns ROOT   https://storeco.org/text/
  -w, --root-id ID     ALL
</pre>

### Example
<pre>
java -jar storeco-0.2.0-SNAPSHOT-standalone.jar -i AZW.tei.xml -o AZW.ttl -f turtle -r https://storeco.org/text/ -w AZW
</pre>

## Credits

Thanks to Antonio Garrote for his awesome work on the [clj-plaza](https://github.com/antoniogarrote/clj-plaza) library .

## License

Copyright © 2020-2021 Daniel Schlager 

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
