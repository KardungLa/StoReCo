(ns storeco.api
  "StoReCo API"
  (:gen-class)
  (:require [clojure.data.xml :as xml]
            [clojure.zip :as zip]
            [clojure.java.io :as io]
            [maailma.core :as m]
            [plaza.rdf.core :as rdf])
  (:use [plaza.rdf.sparql]
        [plaza.rdf.implementations.jena]))

;; Copyright © 2020 Daniel Schlager

(init-jena-framework)

(xml/alias-uri 'xmlns "http://www.w3.org/XML/1998/namespace")

(rdf/register-rdf-ns :dhplus "https://dhplus.sbg.ac.at/ontologies#")
(rdf/register-rdf-ns :dhplusi "https://dhplus.sbg.ac.at/instance/")
(rdf/register-rdf-ns :ldp "http://www.w3.org/ns/ldp#")

(defn uuid [] (.toString (java.util.UUID/randomUUID)))

(def ^:dynamic *model* (rdf/defmodel))

(def parent nil)

(def xml-text "<?xml version='1.0' encoding='UTF-8'?><text xml:id=\"1\">a<p class=\"headline\" xml:id=\"2\">b<hi class=\"subheadline\" xml:id=\"3\">c</hi>inner</p><p xml:id=\"4\">e</p></text>")

(defn get-config []
  (m/build-config
    (m/resource "config-defaults.edn")
    (m/file "./config-local.edn")))

(defn register-rdf-ns
  "Register a given list of rdf namespaces"
  [ns])

(defn read-and-parse-str
  "Read and parse xml string"
  [xml]
  (->
    (-> xml .getBytes java.io.ByteArrayInputStream.)
    (xml/parse :namespace-aware false)))

(defn read-and-parse-file
  "Read and parse xml file"
  [file]
  (cond
    (instance? java.io.File file) (do (prn "io") (xml/parse (io/input-stream file)))
    :else
    (if (.exists (io/file file))
      (->>
        (slurp file)
        (xml/parse-str))
      nil)))

(defn update-parent
  "Update global variable p which holds the parent xml:id"
  [newvalue]
  (alter-var-root #'parent (constantly newvalue)))

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

(defn add-to-model
  "Add triple(s) to model"
  [triples model]
  (do
    (if (= (count triples) 1)
      (let [s (nth (get triples 0) 0)
            p (nth (get triples 0) 1)
            o (nth (get triples 0) 2)]
        (cond
          (string? o) (rdf/with-model model (rdf/model-add-triples (rdf/make-triples [[s p (rdf/rdf-literal o)]])))
          :else
          (rdf/with-model model (rdf/model-add-triples (rdf/make-triples [[s p o]])))))
      (doseq [element triples] (let [s (nth element 0)
                                     p (nth element 1)
                                     o (nth element 2)]
                                 (cond
                                   (string? o) (rdf/with-model model (rdf/model-add-triples (rdf/make-triples [[s p (rdf/rdf-literal o)]])))
                                   :else
                                   (rdf/with-model model (rdf/model-add-triples (rdf/make-triples [[s p o]])))))))))

(defn get-content
  "Get content of element"
  [element]
  (cond
    (nil? element) "" ;; return empty String
    (map? element) (first (:content element))
    :else element))

(defn xml->rdf [element]
  (let [ip parent]
    (do
      (cond
        (sequential? element) (if (> (count element) 1)
                                (do
                                  (def llp (str "List_" ip))
                                  (add-to-model
                                    [[[:dhplusi (keyword llp)]
                                      [:rdf :type]
                                      [:rdf :Seq]]
                                     [[:dhplusi (keyword ip)]
                                      [:ldp :contains]
                                      [:dhplusi (keyword llp)]]] *model*)
                                  (if (string? (last element))
                                    (do
                                      ;; inner text, we need to create another instance
                                      (def ie (str "i" (get-id element)))
                                      (add-to-model [[[:dhplusi (keyword ie)]
                                                      [:dhplus :hasContent]
                                                      (last element)]
                                                     [[:dhplusi (keyword ie)]
                                                      [:rdf :type]
                                                      [:dhplus :tag]]] *model*)))
                                  (update-parent (get-id element))
                                  (map xml->rdf element))
                                (do (update-parent (get-id element))
                                    (xml->rdf (first element))))
        (map? element) (do
                         (update-parent (get-id element))
                         (add-to-model [[[:dhplusi (keyword (get-id element))]
                                         [:rdf :type]
                                         [:dhplus :tag]]
                                        [[:dhplusi (keyword (get-id element))]
                                         [:dhplus :elementName]
                                         [:dhplus (name (:tag element))]]
                                        [[:dhplusi (keyword (get-id element))]
                                         [:dhplus :hasContent]
                                         (get-content (first (:content element)))]] *model*)
                         (if (:attrs element)
                           (do
                             (if (> (count (:attrs element)) 1)
                               (doseq [[k v] (:attrs element)]
                                 (def nid (keyword (str (get-id element) "/" (name k))))
                                 (add-to-model [[[:dhplusi (keyword (get-id element))]
                                                 [:dhplus :attrs]
                                                 [:dhplusi nid]]
                                                [[:dhplusi nid]
                                                 [:dhplus :attrName]
                                                 (name k)]
                                                [[:dhplusi nid]
                                                 [:dhplus :attrValue]
                                                 v]] *model*)))))
                         (xml->rdf (:content element)))))))

(defn build
  "Build RDF from XML and save"
  ([options]
   (prn options)
   (let [document (read-and-parse-file (:input options))
         output (:output options)
         format (:format options)]
     (cond
       (nil? document) (prn "File not found")
       :else
       (spit output (with-out-str (rdf/model-to-format *model* (keyword format)) (xml->rdf document))))))
  ([input output format]
   (let [document (read-and-parse-file input)]
     (cond
       (nil? document) (prn "File not found")
       :else
       (spit output (with-out-str (rdf/model-to-format *model* (keyword format)) (xml->rdf document)))))))
