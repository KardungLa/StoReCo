# StoReCo 

Enhanced Stand-off TEI Annotation with StoReCo: A generic approach with the use of RDF.

This tool is still under development. Changes are possible at any time.

## Installation

StoReCo is written in [Clojure](https://clojure.org/) and uses [Leiningen](https://leiningen.org/) to build the Java library and executable.

### 1. Install Leiningen

Leiningen installs itself on the first run of the `lein` shell script; there is no
separate install script.  Follow these instructions to install Leiningen manually:

1. Make sure you have Java installed; OpenJDK version 8 is recommended at this time.
2. [Download the `lein` script from the `stable` branch](https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein)
 of this project.
3. Place it on your `$PATH`. (`~/bin` is a good choice if it is on your path.)
4. Set it to be executable. (`chmod +x ~/bin/lein`)
5. Run it.

Windows users can use the above script in the Linux subsystem or try
[the batch file](https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein.bat) or
[Powershell version](https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein.ps1)
instead.

### 2. Clone the repo

```bash
git clone https://github.com/KardungLa/StoReCo.git
```

```bash
cd StoReCo/storeco
lein uberjar
cd target/uberjar
```

## Usage

### How to use StoReCo
<pre>
java -jar storeco-0.4.0-standalone.jar
</pre>

<pre>
StoReCo

  Usage: storeco [options]

  Options:
  -h, --help           Displays this help
  -c, --config PATH    EDN file to read config options from
  -i, --input INPUT    Absolute path to input file
  -o, --output OUTPUT  Relative path to output file
  -f, --format FORMAT  TURTLE, NTRIPLES, NT, JSONLD, RDFXML, N3, RDFJSON
  -r, --root-ns ROOT   https://storeco.io/text/
  -w, --root-id ID     ALL
</pre>

### Example
<pre>
java -jar storeco-0.4.0-standalone.jar -i AZW.tei.xml -o AZW.ttl -f turtle -r https://storeco.io/text/ -w AZW
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
