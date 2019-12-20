# Enhanced Stand-off TEI Annotation with StoReCo: A generic approach with the use of RDF.

Author: Daniel Schlager "email at danielschlager.at"

## Abstract

The Text Encoding Initiative (TEI) is the de facto standard for the representation of texts in digital form, especially in the digital humanities of high standing due to its diverse application spectrum.
Due to the increasing number and depth of analysis possibilities and the need for overlapping annotations such as Entity Recognition, Part of Speech Tagging, syntactic analysis, etc., the hierarchical structure soon reaches the boundaries of what is achievable. The usual way of storing primary data and annotation layers in a file results in a non-well-formed XML instance.
There are different approaches to stand-off notation recommended by the Text Encoding Initiative. However, the most recent approach using Xpointer seems to be too complicated, or there are not yet enough tools to make it more accessible to use. Also, the readability of the document in the XML source code suffers from it. 
The StoReCo called Standoff Markup Resolver and Compiler joins the XML/TEI document with one or more XML/RDF sources, which can be available as an XML document or triple store/Sparql endpoint and produces an annotated PDF, HTML or an interactive viewer. 
With this technique, an unlimited number of annotation levels can be introduced. The XML/TEI document is not enriched with additional markup elements and remains in the original for the producer. The link between TEI and RDF opens up new possibilities for use in Linked Open Data.

