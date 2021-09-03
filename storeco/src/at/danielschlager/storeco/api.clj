;; Enhanced Stand-off TEI Annotation with StoReCo: A generic approach with the use of RDF.
;;
;; The Text Encoding Initiative (TEI) is the de facto standard for the representation of texts in digital form, especially in the digital humanities of high standing due to its diverse application spectrum.
;; Due to the increasing number and depth of analysis possibilities and the need for overlapping annotations such as Entity Recognition, Part of Speech Tagging, syntactic analysis, etc., the hierarchical structure soon reaches the boundaries of what is achievable. The usual way of storing primary data and annotation layers in a file results in a non-well-formed XML instance.
;; There are different approaches to stand-off notation recommended by the Text Encoding Initiative. However, the most recent approach using Xpointer seems to be too complicated, or there are not yet enough tools to make it more accessible to use. Also, the readability of the document in the XML source code suffers from it.
;; The StoReCo called Standoff Markup Resolver and Compiler joins the XML/TEI document with one or more RDF sources, which can be available as an XML-RDF document, Turtle, N3, or triple store/Sparql endpoint and produces an annotated PDF, HTML or an interactive viewer.
;; With this technique, an unlimited number of annotation levels can be introduced. The XML/TEI document is not enriched with additional markup elements and remains in the original for the producer. The link between TEI and RDF opens up new possibilities for use in Linked Open Data.
;;
;; Copyright © 2020-2021 Daniel Schlager
;;
;; This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0.
;;
;; This Source Code may also be made available under the following Secondary Licenses when the conditions for such availability set forth in the Eclipse Public License, v. 2.0 are satisfied: GNU General Public License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any later version, with the GNU Classpath Exception which is available at https://www.gnu.org/software/classpath/license.html.
;;

(ns at.danielschlager.storeco.api
  "StoReCo API"
  (:gen-class)
  (:require [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [clj-http.client :as client]
            [maailma.core :as m]
            [plaza.rdf.core :as rdf]
            [plaza.rdf.predicates :as rdf-pred]
            [plaza.rdf.sparql :as sparql]
            [plaza.rdf.implementations.jena :as jena]))

; Initialize Apache Jena
(jena/init-jena-framework)

; -------------- Helper Methods and Variables ---------------------

; Define RDF/XML Namespaces
; @todo allow custom namespaces
(xml/alias-uri 'xmlns "http://www.w3.org/XML/1998/namespace")
(rdf/register-rdf-ns :tei "http://www.tei-c.org/ns/1.0/")
(rdf/register-rdf-ns :dhplus "https://dh.plus.ac.at/ontologies#")
(rdf/register-rdf-ns :dhplusi "https://dh.plus.ac.at/instance/")
(rdf/register-rdf-ns :ldp "http://www.w3.org/ns/ldp#")

(defn uuid [] (.toString (java.util.UUID/randomUUID)))

; Define RDF Model
(def ^:dynamic *model* (rdf/defmodel))

(def root-ns (atom ""))
(def root-id (atom ""))
(defn werk-ns [] (str @root-ns @root-id "#"))

(def parent nil)
(def i 0)
(def stack (atom ()))
(def istack (atom ()))
(def entries (atom ()))

; Helper Methods for mutable data
; Because of the design of the Jena Model, we can't use immutable data
(defn reset-vars! []
  (reset! root-id "")
  (reset! root-ns "")
  (reset! stack ())
  (reset! istack ())
  (reset! entries ()))

(defn add-entry [entry] (swap! entries conj entry))

(defn pop-head!
  [items]
  (ffirst (swap-vals! items rest)))

(defn push-head!
  [value]
  (swap! stack conj value))

(defn push-i-head!
  [value]
  (swap! istack conj value))

(defn get-config []
  (m/build-config
   (m/resource "config-defaults.edn")
   (m/file "./config-local.edn")))

(defn update-parent
  "Update global variable p which holds the parent xml:id"
  [newvalue]
  (alter-var-root #'parent (constantly newvalue)))

(defn increment-i
  "Update global variable i which holds the incremental counter for every element"
  []
  (do (alter-var-root #'i (constantly (inc i))) (push-i-head! i)) i)


; ------------- Main Methods --------------------

; Parse xml from string
(defn read-and-parse-str
  "Read and parse xml string"
  [xml]
  (->
   (-> xml .getBytes java.io.ByteArrayInputStream.)
   (xml/parse :namespace-aware false)))

; Parse xml from file
(defn read-and-parse-file
  "Read and parse xml file"
  [file]
  (cond
    (instance? java.io.File file) (let [f (.getAbsolutePath file)]
                                    (->>
                                     (slurp f)
                                     (xml/parse-str)))
    :else
    (if (.exists (io/file file))
      (->>
       (slurp file)
       (xml/parse-str))
      nil)))


(defn get-xml-id
  "Get xml:id of element"
  [element]
  (cond
    (map? element) (:xmlns.http%3A%2F%2Fwww.w3.org%2FXML%2F1998%2Fnamespace/id (:attrs element))
    (sequential? element) (:xmlns.http%3A%2F%2Fwww.w3.org%2FXML%2F1998%2Fnamespace/id (:attrs (second element)))))

(defn get-id
  "Get xml:id of element"
  [element]
  (let [e (get-xml-id element)]
    (cond
      (nil? e) (uuid)
      :else
      e)))

; Add data to Jena Model
(defn add-to-model
  "Add triple(s) to model"
  [triples model]
    (if (= (count triples) 1)
      (let [s (nth (get triples 0) 0)
            p (nth (get triples 0) 1)
            o (nth (get triples 0) 2)]
        (cond
          (int? o) (rdf/with-model model (rdf/model-add-triples (rdf/make-triples [[s p (rdf/rdf-typed-literal o)]])))
          (string? o) (rdf/with-model model (rdf/model-add-triples (rdf/make-triples [[s p (rdf/rdf-literal o)]])))
          (nil? o) (rdf/with-model model (rdf/model-add-triples (rdf/make-triples [[s p (rdf/rdf-literal "")]])))
          :else
          (rdf/with-model model (rdf/model-add-triples (rdf/make-triples [[s p o]])))))
      (doseq [element triples] (let [s (nth element 0)
                                     p (nth element 1)
                                     o (nth element 2)]
                                 (cond
                                   (string? o) (rdf/with-model model (rdf/model-add-triples (rdf/make-triples [[s p (rdf/rdf-literal o)]])))
                                   :else
                                   (rdf/with-model model (rdf/model-add-triples (rdf/make-triples [[s p o]]))))))))

(defn get-content
  "Get content of element"
  [element]
  (cond
    (nil? element) "" ;; return empty String
    (string? element) element
    (sequential? element) (get-content (first element))
    (map? element) (get-content (first (:content element)))))

(defn has-childs?
  "Check if element has more childs"
  [element]
  (> (count (first (rest (:content element)))) 0))

(defn get-childs
  "Get childs of an element"
  [element]
  (cond
    (map? element) (cond (has-childs? element) (first (rest (:content element))))))

(defn get-attrs
  "Get attributes of an element"
  [element]
  (:attrs element))

(defn get-tag
  "Get tag of an element"
  [element]
  (:tag element))

; @todo
(defn rdf->xml
  "Creates a XML of RDF"
  []
  (let [d (rdf/document->model (io/input-stream "Test.ttl") :turtle)
        query-str "SELECT * WHERE {
  ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?tag .
  ?s <https://dh.plus.ac.at/ontologies#i> ?i .
  ?s <https://dh.plus.ac.at/ontologies#elementName> ?elementName .
  ?s <https://dh.plus.ac.at/ontologies#hasContent> ?hasContent .
  } ORDER BY ASC(?i)"
        query (rdf/query-triples d query-str)]
   (for [q query] (if-not (nil? q)
    (println (count (rdf/walk-triples query (fn [s p o] [s p o]))))
    ))))

(defn parse-string
  "Parse String"
  [element]
  (let [tr-id (keyword uuid)]
    (add-entry tr-id)
    (add-to-model [[[(werk-ns) tr-id]
                    [:dhplus :i]
                    (increment-i)]] *model*)
    (add-to-model [[[(werk-ns) tr-id]
                    [:rdf :type]
                    [:dhplus :contentTag]]] *model*)
    (add-to-model [[[(werk-ns) tr-id]
                    [:dhplus :hasContent]
                    (get-content element)]] *model*)))

(defn parse-map-attrs
  "Parse Map"
  [element]
    (if (> (count (:attrs element)) 0)
      (doseq [[k v] (:attrs element)]
        (let [nid (keyword (str (get-id element) "/" (name k)))
              tr-id (keyword uuid)]
        (add-to-model [[[(werk-ns) tr-id]
                        [:dhplus :attrs]
                        [(werk-ns) nid]]] *model*)
        (add-to-model [[[(werk-ns) nid]
                        [:dhplus :attrName]
                        (name k)]] *model*)
        (add-to-model [[[(werk-ns) nid]
                        [:dhplus :attrValue]
                        v]] *model*)))
      nil
      ))

(defn parse-map-content
  "Parse Map Content"
  [element]
  (let [tr-id (keyword uuid)]
    (add-entry tr-id)
    (add-to-model [[[(werk-ns) tr-id]
                    [:dhplus :i]
                    (increment-i)]] *model*)
    (add-to-model [[[(werk-ns) tr-id]
                    [:rdf :type]
                    [:dhplus :endTag]]] *model*)
    (add-to-model [[[(werk-ns) tr-id]
                    [:dhplus :elementName]
                    [:tei (name (pop-head! stack))]]] *model*)))

(defn parse-map
  "Parse Map"
  [element]
    (let [tr-id (keyword (get-id element))]
    (add-entry tr-id)
    (push-head! (get-tag element))
    (add-to-model [[[(werk-ns) tr-id]
                    [:dhplus :i]
                    (increment-i)]] *model*)
    (add-to-model [[[(werk-ns) tr-id]
                    [:rdf :type]
                    [:dhplus :startTag]]] *model*)
    (add-to-model [[[(werk-ns) tr-id]
                    [:dhplus :elementName]
                    [:tei (name (get-tag element))]]] *model*)
    (add-to-model [[[(werk-ns) tr-id]
                    [:dhplus :hasContent]
                    (get-content element)]] *model*)))

(defn xml->rdf
  "Create RDF triples for every XML, preserving all attributes and add incremental counter value to every group of triples"
  [element]
  (cond
    (string? element) (parse-string element)
    (sequential? element) (let [c (count element)]
                            (cond
                              (= c 1) (do
                                        (xml->rdf (first element)))
                              (> c 1) (do
                                        (if (string? (first (rest element)))
                                          (xml->rdf (first (rest element)))
                                          (do (xml->rdf (first (rest element)))
                                              (if (> (count (rest (rest element))) 1)
                                                (do
                                                  (doseq [el (rest (rest element))]
                                                    (xml->rdf el)))))))))
    (map? element) (do (parse-map element)
                       (if (:attrs element) (parse-map-attrs element))
                       (if-not (string? (:content element)) (do (xml->rdf (:content element))
                                                                (parse-map-content element)
                                                                (if (instance? clojure.data.xml.node.Element (last (:content element))) () (xml->rdf (last (:content element)))))))
    :else (prn element)))

(defn append-rdf-header []
  (rdf/with-model *model* (rdf/model-add-triples (rdf/make-triples [[[@root-ns (keyword root-id)] [:rdf :type] [:dhplus :werk]]])))
  (doseq [e @entries] (rdf/with-model *model* (rdf/model-add-triples (rdf/make-triples [[[@root-ns (keyword root-id)] [:dhplus :entry] [(werk-ns) e]]])))))

(defn build-from-string
  "Build RDF from XML String and return"
  ([input format root-ns-param root-id-param]
   (let [document (read-and-parse-str input)]
     (cond
       (nil? document) (prn "File not found")
       :else
       (do
         (reset! root-id root-id-param)
         (reset! root-ns root-ns-param)
         (xml->rdf document)
         (append-rdf-header)
         (with-out-str (rdf/model->format *model* (keyword format))))))))

(defn build-from-url
  "Build RDF from URL and return"
  ([input format root-ns-param root-id-param]
   (let [req (client/get input {:accept :xml})]
     (if (= (:status req) 200)
       (build-from-string (:body req) format root-ns-param root-id-param)
       nil))))

(defn build
  "Build RDF from XML and return"
  ([options]
   (let [document (read-and-parse-file (:input options))
         root-id-param (:root-id options)
         root-ns-param (:root-ns options)
         format (:format options)]
     (cond
       (nil? document) (prn "File not found")
       :else
       (do
         (reset! root-id root-id-param)
         (reset! root-ns root-ns-param)
         (xml->rdf document)
         (append-rdf-header)
         (with-out-str (rdf/model->format *model* (keyword format)))))))
  ([input output format root-ns-param root-id-param]
   (let [document (read-and-parse-file input)]
     (cond
       (nil? document) (prn "File not found")
       :else
       (do
         (reset! root-id root-id-param)
         (reset! root-ns root-ns-param)
         (xml->rdf document)
         (append-rdf-header)
         (with-out-str (rdf/model->format *model* (keyword format))))))))

(defn build-file
  "Build RDF from XML and save"
  ([options]
  (spit (:output options) (build options)))
  ([input output format root-ns-param root-id-param]
  (spit output (build input output format root-ns-param root-id-param))
  ))
