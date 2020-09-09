(ns storeco.api
  "StoReCo API"
  (:gen-class)
  (:require [clojure.data.xml :as xml]
            [clojure.zip :as zip]
            [clojure.java.io :as io]
            [clojure.string :as str]
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
(def i 0)
(def stack (atom ()))

(defn pop-head!
  [items]
  (ffirst (swap-vals! items rest)))

(defn push-head!
  [value]
  (swap! stack conj value))

(defn get-config []
  (m/build-config
   (m/resource "config-defaults.edn")
   (m/file "./config-local.edn")))

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

(defn update-parent
  "Update global variable p which holds the parent xml:id"
  [newvalue]
  (alter-var-root #'parent (constantly newvalue)))

(defn increment-i
  "Update global variable i which holds the incremental counter for every element"
  []
  (alter-var-root #'i (constantly (inc i))))

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
                                   (rdf/with-model model (rdf/model-add-triples (rdf/make-triples [[s p o]])))))))))

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

(defn xml->rdf
  "Create RDF triples for every XML, preserving all attributes and add incremental counter value to every group of triples"
  [element]
  (let [ip parent]
    (cond
      (sequential? element) (let [c (count element)]
                              (cond
                                (= c 1) (do
                                          (def tr-id (keyword (uuid)))
                                          (add-to-model [[[:dhplusi tr-id]
                                                          [:dhplus :i]
                                                          (increment-i)]] *model*)
                                          (add-to-model [[[:dhplusi tr-id]
                                                          [:rdf :type]
                                                          [:dhplus :endTag]]] *model*)
                                          (add-to-model [[[:dhplusi tr-id]
                                                          [:dhplus :elementName]
                                                          [:dhplus (pop-head! stack)]]] *model*)
                                          (xml->rdf (first element)))
                                (> c 1) (if (= c 3)
                                          (do
                                            (def tr-id (keyword (uuid)))
                                            (add-to-model [[[:dhplusi tr-id]
                                                            [:dhplus :i]
                                                            (increment-i)]] *model*)
                                            (add-to-model [[[:dhplusi tr-id]
                                                            [:rdf :type]
                                                            [:dhplus :contentTag]]] *model*)
                                            (add-to-model [[[:dhplusi tr-id]
                                                            [:dhplus :hasContent]
                                                            (get-content element)]] *model*)
                                            (xml->rdf (first (rest element))))
                                          (do
                                            (xml->rdf (first (rest element)))
                                            (doseq [el (rest (rest element))]
                                              (xml->rdf el))
                                            (def tr-id (keyword (uuid)))
                                            (add-to-model [[[:dhplusi tr-id]
                                                            [:dhplus :i]
                                                            (increment-i)]] *model*)
                                            (add-to-model [[[:dhplusi tr-id]
                                                            [:rdf :type]
                                                            [:dhplus :endTag]]] *model*)
                                            (add-to-model [[[:dhplusi tr-id]
                                                            [:dhplus :elementName]
                                                            [:dhplus (pop-head! stack)]]] *model*)))))
      (map? element) (do
                       (def tr-id (keyword (get-id element)))
                       (push-head! (get-tag element))
                       (add-to-model [[[:dhplusi tr-id]
                                       [:dhplus :i]
                                       (increment-i)]] *model*)
                       (add-to-model [[[:dhplusi tr-id]
                                       [:rdf :type]
                                       [:dhplus :startTag]]] *model*)
                       (add-to-model [[[:dhplusi tr-id]
                                       [:dhplus :elementName]
                                       [:dhplus (name (get-tag element))]]] *model*)
                       (add-to-model [[[:dhplusi tr-id]
                                       [:dhplus :hasContent]
                                       (get-content element)]] *model*)
                       (if (:attrs element)
                         (do
                           (if (> (count (:attrs element)) 1)
                             (doseq [[k v] (:attrs element)]
                               (def nid (keyword (str (get-id element) "/" (name k))))
                               (add-to-model [[[:dhplusi tr-id]
                                               [:dhplus :attrs]
                                               [:dhplusi nid]]] *model*)
                               (add-to-model [[[:dhplusi nid]
                                               [:dhplus :attrName]
                                               (name k)]] *model*)
                               (add-to-model [[[:dhplusi nid]
                                               [:dhplus :attrValue]
                                               v]] *model*)))))
                       (xml->rdf (:content element))))))

(defn build
  "Build RDF from XML and save"
  ([options]
   (let [document (read-and-parse-file (:input options))
         output (:output options)
         format (:format options)]
     (cond
       (nil? document) (prn "File not found")
       :else
       (do
         (def document (read-and-parse-file (:input options)))
         (xml->rdf document)
         (def o (with-out-str (rdf/model->format *model* (keyword format))))
         (spit output o)))))
  ([input output format]
   (let [document (read-and-parse-file input)]
     (cond
       (nil? document) (prn "File not found")
       :else
       (do
         (def document (read-and-parse-file input))
         (xml->rdf document)
         (def o (with-out-str (rdf/model->format *model* (keyword format))))
         (spit output o))))))
